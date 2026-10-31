package com.glasslauncher.tv

import com.glasslauncher.tv.data.CatalogDiscovery
import com.glasslauncher.tv.data.CatalogRef
import com.glasslauncher.tv.data.HeroItem
import com.glasslauncher.tv.data.Tmdb
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class CatalogDiscoveryTest {
    private fun catalog(id: String, type: String = "movie", name: String = id, required: Boolean = false) =
        CatalogRef("https://example.org/manifest.json", "Test", type, id, name, requiresInput = required)
    private fun item(id: String, type: String) = HeroItem("$type:$id", id, type, id, "", "", "", "", "", "", emptyList(), "")

    @Test fun choosesRealDiscoveryFeedsIncludingGermanTypesAndAnime() {
        val feeds = listOf(catalog("tmdb.trending"), catalog("tmdb.trending", "series"),
            catalog("custom.new_releases", "Film", "Neuerscheinungen"),
            catalog("custom.new_releases", "Serie", "Neuerscheinungen"),
            catalog("tmdb.top_series", "Serien", "Beliebt"), catalog("mal.airing", "anime"),
            catalog("mal.most_popular", "anime"))
        assertEquals(feeds.toSet(), CatalogDiscovery.choose(feeds).toSet())
        assertEquals("series", CatalogDiscovery.mediaType("Serien"))
        assertEquals("movie", CatalogDiscovery.mediaType("Film"))
    }

    @Test fun ignoresSearchCalendarNicheAndAllTimeTopRatedInAutomaticMode() {
        val feeds = listOf(catalog("search.movie", name = "Popular search", required = true),
            catalog("calendar-videos", "series", required = true), catalog("tmdb.top_rated_movie"),
            catalog("tmdb.discover.movie.world.brazilian"), catalog("tmdb.discover.movie.world.chinese"),
            catalog("tmdb.trending"))
        assertEquals(listOf(feeds.last()), CatalogDiscovery.choose(feeds))
    }

    @Test fun alternatesMediaTypesAndDeduplicatesWithoutLosingTheTopPicks() {
        val movie = catalog("tmdb.trending")
        val newer = catalog("new_releases")
        val series = catalog("tmdb.trending", "series")
        val anime = catalog("mal.airing", "anime")
        val results = mapOf(movie.key to listOf(item("m1", "movie"), item("m2", "movie")),
            newer.key to listOf(item("m1", "movie"), item("m3", "movie")),
            series.key to listOf(item("s1", "series"), item("s2", "series")),
            anime.key to listOf(item("a1", "anime"), item("a2", "anime")))
        val merged = CatalogDiscovery.merge(listOf(movie, newer, series, anime), results, 0, false)
        assertEquals(listOf("m1", "s1", "a1"), merged.take(3).map { it.id })
        assertEquals(7, merged.size)
        assertEquals(merged.size, merged.distinctBy { it.key }.size)
        assertEquals(4, CatalogDiscovery.merge(listOf(movie, newer, series, anime), results, 4, true).size)
    }

    @Test fun varietyKeepsLeadRecommendationsAndCannotPullFromDeepCatalogPages() {
        val feed = catalog("tmdb.trending")
        val titles = (1..30).map { item("m$it", "movie") }
        repeat(10) {
            val mixed = CatalogDiscovery.merge(listOf(feed), mapOf(feed.key to titles), 30, true)
            assertEquals(titles.first(), mixed.first())
            assertEquals(titles.subList(1, 6).toSet(), mixed.subList(1, 6).toSet())
        }
    }

    @Test fun encodesRequiredExtrasInTheStremioPathAndKeepsLocalizedType() {
        val feed = catalog("custom:recent", "Film").copy(extra = mapOf("genre" to "Science Fiction"))
        assertEquals("https://example.org/catalog/Film/custom%3Arecent/genre=Science%20Fiction&skip=20.json", CatalogDiscovery.url(feed, 20))
        assertEquals("https://example.org/catalog/movie/top.json", CatalogDiscovery.url(catalog("top").copy(supportsSkip = false), 0))
    }

    @Test fun animeFallbackRejectsOldUnknownAndUnreleasedTitles() {
        val today = LocalDate.of(2026, 10, 31)
        assertTrue(Tmdb.recentAnimeDate("2026-10-01", today))
        assertTrue(Tmdb.recentAnimeDate(today.minusDays(730).toString(), today))
        assertFalse(Tmdb.recentAnimeDate("2016-07-12", today))
        assertFalse(Tmdb.recentAnimeDate(today.plusDays(1).toString(), today))
        assertFalse(Tmdb.recentAnimeDate("", today))
    }

    @Test fun absentOrFailedFeedsDoNotBlockTheAvailableMediaGroups() {
        val feeds = listOf(catalog("tmdb.trending"), catalog("mal.airing", "anime"))
        assertEquals(listOf(item("a1", "anime")), CatalogDiscovery.merge(feeds, mapOf(feeds.last().key to listOf(item("a1", "anime"))), 70, false))
        assertTrue(CatalogDiscovery.merge(feeds, emptyMap(), 70, true).isEmpty())
    }
}
