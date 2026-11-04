package com.glasslauncher.tv

import com.glasslauncher.tv.data.DockLayout
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class DockLayoutTest {
    @Test fun versionOneUpgradeAddsPinsWithoutChangingAppOrderingAndKeepsCustomPins() {
        val design = com.glasslauncher.tv.data.AppleTvDesign
        val old = mapOf(design.VERSION_KEY to 1, "dockOrder" to "custom-order")
        val upgraded = old + design.migration(old)
        assertEquals(DockLayout.defaults.joinToString("\n"), upgraded["dockApps"])
        assertEquals("custom-order", upgraded["dockOrder"])
        assertTrue(design.migration(upgraded).isEmpty())
        assertFalse(design.migration(old + ("dockApps" to "custom.app")).containsKey("dockApps"))
    }

    @Test fun defaultsKeepOnlyNuvioSmartTubeSpotifyAndSettings() {
        assertEquals(listOf("space.nuvio.nativelegacy", DockLayout.SMARTTUBE,
            "com.spotify.tv.android", DockLayout.SETTINGS), DockLayout.defaults)
    }

    @Test fun fireTvYoutubeAliasKeepsTheRequestedPositionAndOtherAppsStayOut() {
        val installed = setOf("space.nuvio.nativelegacy", "com.amazon.firetv.youtube",
            "com.spotify.tv.android", "com.netflix.ninja", "unrelated.app")
        assertEquals(listOf("space.nuvio.nativelegacy", "com.amazon.firetv.youtube",
            "com.spotify.tv.android", DockLayout.SETTINGS),
            DockLayout.resolve(listOf("space.nuvio.nativelegacy", DockLayout.YOUTUBE, "com.spotify.tv.android", DockLayout.SETTINGS),
                installed, emptySet()))
    }

    @Test fun customPinsOrderHiddenAndMissingAppsAreRespected() {
        assertEquals(listOf("custom.app", DockLayout.SETTINGS),
            DockLayout.resolve(listOf("custom.app", "missing.app", "hidden.app", DockLayout.SETTINGS, "custom.app"),
                setOf("custom.app", "hidden.app"), setOf("hidden.app")))
    }

    @Test fun nativeAndroidTvYoutubeIsPreferredAndAliasesAreDeduplicated() {
        assertEquals(listOf(DockLayout.YOUTUBE), DockLayout.resolve(
            listOf(DockLayout.YOUTUBE, "com.amazon.firetv.youtube"),
            setOf(DockLayout.YOUTUBE), emptySet()))
    }

    @Test fun bothNuvioSourcesBelongToTheHomeContinueWatchingRow() {
        assertTrue(DockLayout.isNuvio("Nuvio"))
        assertTrue(DockLayout.isNuvio("Nuvio Legacy"))
        assertFalse(DockLayout.isNuvio("Trakt"))
        assertFalse(DockLayout.isNuvio("Stremio"))
    }

    @Test fun heroVisibilityCannotGateLibraryOrBackgroundCollection() {
        // Regression guard: the old hero-only switch disabled Library cards and account synchronization.
        for (file in listOf("data/ContinueWatching.kt", "ui/LibraryScreen.kt")) {
            assertFalse(file, File("src/main/java/com/glasslauncher/tv/$file").readText().contains(".continueMode"))
        }
    }
}
