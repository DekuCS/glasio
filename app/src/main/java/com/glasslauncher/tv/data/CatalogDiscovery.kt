package com.glasslauncher.tv.data

import java.net.URLEncoder
import java.util.Locale

/** Select real discovery feeds from the installed add-ons, keeping their ranked first pages. */
object CatalogDiscovery {
    fun mediaType(type: String): String = when (type.lowercase(Locale.ROOT)) {
        "movie", "film", "filme" -> "movie"
        "series", "tv", "serie", "serien" -> "series"
        else -> type
    }

    fun group(c: CatalogRef): String = if (c.type.equals("anime", true) ||
        "anime" in c.id.lowercase(Locale.ROOT) || c.id.startsWith("mal.")) "anime" else mediaType(c.type)

    private fun rank(c: CatalogRef): Int? {
        if (c.requiresInput) return null
        val text = "${c.id} ${c.name}".lowercase(Locale.ROOT)
        if (listOf("search", "suchergebnis", "calendar", "upcoming", "schedule", "top_rated", "best rated", "besten bewertet").any { it in text }) return null
        return when {
            "trending" in text || "airing now" in text || c.id == "mal.airing" -> 0
            listOf("new_release", "new release", "neuerschein", "now_playing", "now playing", "on_the_air").any { it in text } || c.id == "mal.seasons" -> 1
            listOf("popular", "beliebt", "top_movie", "top_series").any { it in text } || c.id == "top" -> 2
            else -> null
        }
    }

    fun choose(all: List<CatalogRef>): List<CatalogRef> = listOf("movie", "series", "anime").flatMap { kind ->
        all.filter { group(it) == kind && rank(it) != null }
            .sortedWith(compareBy<CatalogRef> { rank(it) }.thenBy { it.key })
            .distinctBy { "${it.type}|${it.id}" }.take(3)
    }

    /** Alternate movies, TV and anime, so a large movie feed cannot crowd the others out. */
    fun merge(chosen: List<CatalogRef>, results: Map<String, List<HeroItem>>, cap: Int, vary: Boolean): List<HeroItem> {
        val groups = listOf("movie", "series", "anime").map { kind ->
            val feeds = chosen.filter { group(it) == kind }.map { results[it.key].orEmpty().iterator() }
            val ranked = mutableListOf<HeroItem>()
            while (feeds.any { it.hasNext() }) for (feed in feeds) if (feed.hasNext()) ranked += feed.next()
            // Keep the lead recommendation; variety stays within adjacent ranking bands.
            (ranked.take(1) + ranked.drop(1).chunked(5).flatMap { if (vary) it.shuffled() else it }).iterator()
        }
        val out = mutableListOf<HeroItem>()
        val seen = HashSet<String>()
        while (groups.any { it.hasNext() }) for (feed in groups) {
            while (feed.hasNext()) {
                val item = feed.next()
                if (!seen.add(item.key)) continue
                out += item
                if (cap > 0 && out.size >= cap) return out
                break
            }
        }
        return out
    }

    /** The country's own chart leads; the add-on feeds fill the rest, without duplicates. */
    fun lead(region: List<HeroItem>, rest: List<HeroItem>, cap: Int): List<HeroItem> {
        if (region.isEmpty()) return rest
        val out = ArrayList<HeroItem>(region.size + rest.size)
        val seen = HashSet<String>()
        for (item in region + rest) {
            if (!seen.add(item.key)) continue
            out += item
            if (cap > 0 && out.size >= cap) break
        }
        return out
    }

    fun url(c: CatalogRef, skip: Int): String {
        fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
        val extras = c.extra.toMutableMap()
        if (c.supportsSkip) extras["skip"] = skip.toString()
        val suffix = if (extras.isEmpty()) "" else "/" + extras.entries.joinToString("&") { "${enc(it.key)}=${enc(it.value)}" }
        return "${c.addon.removeSuffix("/manifest.json").trimEnd('/')}/catalog/${enc(c.type)}/${enc(c.id)}$suffix.json"
    }
}
