package com.glasslauncher.tv.data

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONObject
import java.time.LocalDate
import java.net.URLEncoder
import java.util.Locale

/**
 * Optional TMDB enrichment (same idea as Nuvio TV Relay): title logo, full-size backdrop, localized
 * synopsis, rating, year, genres and the streaming platform; also backs up empty anime feeds.
 * Only runs when a read token is set.
 */
class Tmdb(private val http: Http) {
    companion object {
        internal fun recentAnimeDate(date: String, today: LocalDate): Boolean = runCatching {
            val first = LocalDate.parse(date)
            !first.isAfter(today) && !first.isBefore(today.minusDays(730))
        }.getOrDefault(false)

        /** Country whose charts the carousel follows: what is popular, trending and new here, not in the US. */
        const val REGION = "DE"

        /** TMDB's v3 API Key (32 hex characters): not what Glassio takes. */
        fun looksLikeApiKey(t: String) = Regex("^[0-9a-fA-F]{32}$").matches(t.trim())

        /**
         * Checks the API Read Access Token with TMDB itself: true = works, false = TMDB refused it,
         * null = couldn't ask (offline).
         */
        fun verify(token: String): Boolean? {
            if (token.isBlank()) return false
            if (looksLikeApiKey(token)) return false
            return runCatching {
                val c = java.net.URL("https://api.themoviedb.org/3/authentication").openConnection() as java.net.HttpURLConnection
                c.connectTimeout = 8000; c.readTimeout = 8000
                c.setRequestProperty("Authorization", "Bearer ${token.trim()}")
                c.setRequestProperty("Accept", "application/json")
                try {
                    when (c.responseCode) {
                        in 200..299 -> JSONObject(c.inputStream.bufferedReader().use { it.readText() }).optBoolean("success", true)
                        401, 403 -> false
                        else -> null
                    }
                } finally { c.disconnect() }
            }.getOrNull()
        }
    }

    private val ttl = 30L * 24 * 3600 * 1000

    private fun get(url: String, token: String) = http.json(url, ttl, token)
    private fun norm(s: String) = s.lowercase(Locale.ROOT).replace(Regex("\\(.*?\\)|\\[.*?\\]"), " ")
        .replace(Regex("[^\\p{L}\\p{N}]"), " ").replace(Regex("\\s+"), " ").trim()

    /** Fallback when an installed anime provider returns no feed. Resolves canonical IMDb IDs before titles enter Home. */
    suspend fun currentAnime(token: String, lang: String, limit: Int): List<HeroItem> = coroutineScope {
        if (token.isBlank()) return@coroutineScope emptyList()
        val today = LocalDate.now()
        val language = TMDB_LANGS[lang] ?: "en-US"
        val url = "https://api.themoviedb.org/3/discover/tv?language=$language&sort_by=popularity.desc" +
            "&with_genres=16&with_origin_country=JP&include_adult=false&vote_count.gte=20&vote_average.gte=6" +
            "&air_date.gte=${today.minusDays(120)}&air_date.lte=$today&first_air_date.gte=${today.minusDays(730)}&first_air_date.lte=$today"
        val list = runCatching { http.json(url, 60 * 60 * 1000L, token).optJSONArray("results") }.getOrNull()
            ?: return@coroutineScope emptyList()
        val gate = Semaphore(3)
        (0 until minOf(list.length(), limit)).map { index -> async {
            gate.withPermit {
                val m = list.optJSONObject(index) ?: return@withPermit null
                val id = m.optInt("id")
                if (id <= 0 || m.optBoolean("adult") || !recentAnimeDate(m.optString("first_air_date"), today)) return@withPermit null
                val imdb = runCatching { get("https://api.themoviedb.org/3/tv/$id/external_ids", token).optString("imdb_id") }.getOrNull()
                    ?.takeIf { it.matches(Regex("tt\\d{7,10}")) } ?: return@withPermit null
                val name = m.optString("name").ifBlank { m.optString("original_name") }
                val backdrop = m.optString("backdrop_path").takeIf { it.startsWith("/") } ?: return@withPermit null
                if (name.isBlank()) return@withPermit null
                val poster = m.optString("poster_path").takeIf { it.startsWith("/") }.orEmpty()
                HeroItem(key = "series:$imdb", id = imdb, type = "series", name = name,
                    poster = if (poster.isEmpty()) "" else "https://image.tmdb.org/t/p/original$poster",
                    background = "https://image.tmdb.org/t/p/original$backdrop", logo = "",
                    overview = m.optString("overview"), rating = String.format(Locale.US, "%.1f", m.optDouble("vote_average")),
                    year = m.optString("first_air_date").take(4), genres = listOf("Anime"), platform = "")
            }
        } }.awaitAll().filterNotNull()
    }

    /**
     * What is popular, trending and new in [REGION] right now, from the charts that know the country:
     * now playing (German cinemas), popular (ranked for the region), the German productions, and the weekly
     * trend list (the only one TMDB has no region parameter for). Round-robin over the feeds, so no single
     * chart fills the carousel. IMDb IDs are resolved here because that is what the add-ons play by.
     */
    suspend fun discovery(token: String, lang: String, limit: Int): List<HeroItem> = coroutineScope {
        if (token.isBlank() || limit <= 0) return@coroutineScope emptyList()
        val language = TMDB_LANGS[lang] ?: "de-DE"
        val feeds = listOf(
            "movie/now_playing?region=$REGION",
            "trending/all/week",
            "movie/popular?region=$REGION",
            "tv/popular",
            "discover/movie?sort_by=popularity.desc&with_origin_country=$REGION",
            "discover/tv?sort_by=popularity.desc&with_origin_country=$REGION",
        )
        val per = (limit / feeds.size + 3).coerceIn(4, 20)
        val rows = feeds.map { feed ->
            async {
                runCatching {
                    get("https://api.themoviedb.org/3/$feed&language=$language&include_adult=false", token)
                        .optJSONArray("results")
                }.getOrNull()?.let { arr -> (0 until minOf(arr.length(), per)).mapNotNull { arr.optJSONObject(it) } }
                    .orEmpty().toMutableList()
            }
        }.awaitAll().filter { it.isNotEmpty() }
        if (rows.isEmpty()) return@coroutineScope emptyList()

        val picked = mutableListOf<JSONObject>()
        val seen = HashSet<Int>()
        var turn = 0
        // Bounded: every row shrinks by one per turn, so this ends even if every entry is a duplicate.
        while (picked.size < limit && rows.any { it.isNotEmpty() } && turn < limit * rows.size) {
            val row = rows[turn % rows.size]
            if (row.isNotEmpty()) {
                val m = row.removeAt(0)
                if (seen.add(m.optInt("id"))) picked += m
            }
            turn++
        }

        val gate = Semaphore(6)
        picked.map { m -> async {
            gate.withPermit {
                val id = m.optInt("id")
                if (id <= 0) return@withPermit null
                val isTv = m.optString("media_type") == "tv" || m.has("first_air_date")
                val kind = if (isTv) "tv" else "movie"
                val imdb = runCatching { get("https://api.themoviedb.org/3/$kind/$id/external_ids", token).optString("imdb_id") }
                    .getOrNull()?.takeIf { it.matches(Regex("tt\\d{7,10}")) } ?: return@withPermit null
                val backdrop = m.optString("backdrop_path").takeIf { it.startsWith("/") } ?: return@withPermit null
                val name = (if (isTv) m.optString("name") else m.optString("title"))
                    .ifBlank { if (isTv) m.optString("original_name") else m.optString("original_title") }
                if (name.isBlank()) return@withPermit null
                val poster = m.optString("poster_path").takeIf { it.startsWith("/") }.orEmpty()
                val date = if (isTv) m.optString("first_air_date") else m.optString("release_date")
                val score = m.optDouble("vote_average", 0.0)
                HeroItem(
                    key = "${if (isTv) "series" else "movie"}:$imdb", id = imdb, type = if (isTv) "series" else "movie",
                    name = name,
                    poster = if (poster.isEmpty()) "" else "https://image.tmdb.org/t/p/original$poster",
                    background = "https://image.tmdb.org/t/p/original$backdrop", logo = "",
                    overview = m.optString("overview"),
                    rating = if (score > 0.1) String.format(Locale.US, "%.1f", score) else "",
                    year = if (date.matches(Regex("(19|20)\\d{2}.*"))) date.take(4) else "",
                    genres = emptyList(), platform = "",
                )
            }
        } }.awaitAll().filterNotNull()
    }

    fun enrich(item: HeroItem, token: String, lang: String): HeroItem? {
        if (token.isBlank()) return null
        val isTv = item.type.equals("series", true) || item.type.equals("tv", true)
        val kind = if (isTv) "tv" else "movie"
        val tmdbLang = TMDB_LANGS[lang] ?: "en-US"
        val imdb = item.id.takeIf { it.matches(Regex("tt\\d{7,10}")) }
        var tmdbId = 0
        runCatching {
            if (imdb != null) {
                val r = get("https://api.themoviedb.org/3/find/$imdb?external_source=imdb_id", token)
                tmdbId = r.optJSONArray(if (isTv) "tv_results" else "movie_results")?.optJSONObject(0)?.optInt("id", 0) ?: 0
            }
            if (tmdbId == 0) {
                val q = URLEncoder.encode(item.name, "UTF-8")
                val y = if (item.year.isNotBlank()) (if (isTv) "&first_air_date_year=${item.year}" else "&year=${item.year}") else ""
                val r = get("https://api.themoviedb.org/3/search/$kind?query=$q$y&include_adult=false", token)
                val results = r.optJSONArray("results")
                if (results != null) for (i in 0 until results.length()) {
                    val c = results.optJSONObject(i) ?: continue
                    val titles = listOf("name", "title", "original_name", "original_title").map { norm(c.optString(it)) }
                    if (norm(item.name) in titles) { tmdbId = c.optInt("id", 0); break }
                }
            }
        }
        if (tmdbId == 0) return null
        val d = runCatching {
            get("https://api.themoviedb.org/3/$kind/$tmdbId?language=$tmdbLang&append_to_response=images&include_image_language=en,null", token)
        }.getOrNull() ?: return null

        var overview = d.optString("overview").trim()
        if (overview.isBlank() && lang != "en") {
            overview = runCatching { get("https://api.themoviedb.org/3/$kind/$tmdbId?language=en-US", token).optString("overview").trim() }.getOrDefault("")
        }
        val backdrop = d.optString("backdrop_path").takeIf { it.startsWith("/") }?.let { "https://image.tmdb.org/t/p/original$it" }.orEmpty()
        val poster = d.optString("poster_path").takeIf { it.startsWith("/") }?.let { "https://image.tmdb.org/t/p/original$it" }.orEmpty()

        var logoPath: String? = null; var rank = Int.MAX_VALUE
        d.optJSONObject("images")?.optJSONArray("logos")?.let { logos ->
            for (i in 0 until logos.length()) {
                val l = logos.optJSONObject(i) ?: continue
                val p = l.optString("file_path"); if (!p.startsWith("/")) continue
                val lr = when (l.optString("iso_639_1")) { "en" -> 0; "null", "" -> 1; else -> 2 }
                val r = lr * 1_000_000 - l.optInt("width", 0).coerceIn(0, 999_999)
                if (r < rank) { rank = r; logoPath = p }
            }
        }
        // Textless backdrops first, best rated first: the Apple TV style gallery for this title.
        val gallery = d.optJSONObject("images")?.optJSONArray("backdrops")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
                .filter { it.optString("file_path").startsWith("/") && it.optInt("width") >= 1280 }
                .sortedWith(compareBy<JSONObject> { if (it.isNull("iso_639_1")) 0 else 1 }.thenByDescending { it.optDouble("vote_average") })
                .map { "https://image.tmdb.org/t/p/original" + it.optString("file_path") }
                .distinct().take(6)
        }.orEmpty()
        // Highest resolution: textless first, then the widest file.
        val bestArt = d.optJSONObject("images")?.optJSONArray("backdrops")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.filter { it.optString("file_path").startsWith("/") }
                .sortedWith(compareBy<JSONObject> { if (it.isNull("iso_639_1")) 0 else 1 }.thenBy { if (it.optInt("width") > 3840) 1 else 0 }.thenByDescending { it.optInt("width") })
                .firstOrNull()?.let { "https://image.tmdb.org/t/p/original" + it.optString("file_path") }
        }.orEmpty()
        // Genres follow the synopsis language; English only when TMDB has no translation for this title.
        fun genresOf(src: JSONObject): List<String> = src.optJSONArray("genres")?.let { g ->
            (0 until minOf(3, g.length())).mapNotNull { g.optJSONObject(it)?.optString("name")?.takeIf { n -> n.isNotBlank() } }
        }.orEmpty()
        val genres = genresOf(d).ifEmpty {
            if (lang == "en") emptyList()
            else runCatching { get("https://api.themoviedb.org/3/$kind/$tmdbId?language=en-US", token) }.getOrNull()?.let(::genresOf).orEmpty()
        }
        val score = d.optDouble("vote_average", -1.0)
        val rating = if (score > 0.1) String.format(Locale.US, "%.1f", score) else ""
        val date = if (isTv) d.optString("first_air_date") else d.optString("release_date")
        val year = if (date.matches(Regex("(19|20)\\d{2}.*"))) date.take(4) else ""
        val platform = runCatching { provider(kind, tmdbId, token) }.getOrDefault("")

        return item.copy(
            background = backdrop.ifBlank { item.background },
            poster = poster.ifBlank { item.poster },
            logo = logoPath?.let { "https://image.tmdb.org/t/p/original$it" } ?: item.logo,
            overview = overview.ifBlank { item.overview },
            rating = rating.ifBlank { item.rating },
            year = year.ifBlank { item.year },
            genres = genres.ifEmpty { item.genres },
            platform = platform.ifBlank { item.platform },
            gallery = (listOf(backdrop.ifBlank { item.background }) + gallery).filter { it.isNotBlank() }.distinct(),
            addonArt = item.addonArt.ifBlank { item.background },
            bestArt = bestArt,
        )
    }

    private fun provider(kind: String, id: Int, token: String): String {
        val results = get("https://api.themoviedb.org/3/$kind/$id/watch/providers", token).optJSONObject("results") ?: return ""
        // The German catalogue first: a platform that only carries the title in the US is not on this TV.
        val country = results.optJSONObject(REGION) ?: results.optJSONObject("US") ?: return ""
        for (cat in listOf("flatrate", "free", "ads")) {
            val arr = country.optJSONArray(cat) ?: continue
            for (i in 0 until arr.length()) {
                val name = display(arr.optJSONObject(i)?.optString("provider_name").orEmpty())
                if (name.isNotBlank()) return name
            }
        }
        return ""
    }

    private fun display(raw: String): String {
        val n = raw.lowercase(Locale.ROOT).replace("+", " plus ").replace(Regex("[^a-z0-9]+"), " ").trim()
        return when {
            n.contains("apple tv") -> "Apple TV"
            n.contains("prime video") || n.startsWith("amazon prime") -> "Prime Video"
            n.contains("peacock") -> "Peacock"
            n.contains("paramount") -> "Paramount"
            n == "hbo" || n.contains("hbo max") -> "HBO"
            n == "max" || n.startsWith("max ") -> "Max"
            n.contains("disney") -> "Disney"
            n.contains("netflix") -> "Netflix"
            n.contains("hulu") -> "Hulu"
            n.contains("crunchyroll") -> "Crunchyroll"
            n.contains("youtube") -> "YouTube"
            else -> raw.trim().replace(Regex("(?i)\\s+(premium|plus|channel|channels|subscription)$"), "").trim()
        }
    }
}

/** Synopsis languages (Settings → Carousel → Synopsis language): setting value → TMDB language. */
val TMDB_LANGS = linkedMapOf(
    "en" to "en-US", "ar" to "ar-SA", "fr" to "fr-FR", "es" to "es-ES", "es-MX" to "es-MX", "de" to "de-DE",
    "it" to "it-IT", "pt" to "pt-PT", "pt-BR" to "pt-BR", "nl" to "nl-NL", "tr" to "tr-TR", "ru" to "ru-RU",
    "uk" to "uk-UA", "pl" to "pl-PL", "fa" to "fa-IR", "he" to "he-IL", "hi" to "hi-IN", "id" to "id-ID",
    "ja" to "ja-JP", "ko" to "ko-KR", "zh" to "zh-CN", "zh-TW" to "zh-TW", "sv" to "sv-SE", "el" to "el-GR",
)
val LANG_NAMES = mapOf(
    "en" to "English", "ar" to "Arabic", "fr" to "French", "es" to "Spanish (Spain)", "es-MX" to "Spanish (Latin America)",
    "de" to "German", "it" to "Italian", "pt" to "Portuguese", "pt-BR" to "Portuguese (Brazil)", "nl" to "Dutch",
    "tr" to "Turkish", "ru" to "Russian", "uk" to "Ukrainian", "pl" to "Polish", "fa" to "Persian", "he" to "Hebrew",
    "hi" to "Hindi", "id" to "Indonesian", "ja" to "Japanese", "ko" to "Korean", "zh" to "Chinese (Simplified)",
    "zh-TW" to "Chinese (Traditional)", "sv" to "Swedish", "el" to "Greek",
)
