package com.glasslauncher.tv

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.glasslauncher.tv.data.AppleTvDesign
import com.glasslauncher.tv.data.LauncherThemes
import com.glasslauncher.tv.data.Settings
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy

class AppleTvDesignTest {
    @Test fun bundledTmdbTokenIsTheFallbackAndCustomTokensRemainOverrides() {
        val st = Settings(context(MemoryPrefs()))
        assertEquals(BuildConfig.TMDB_READ_ACCESS_TOKEN, st.tmdbToken)
        st.tmdbToken = "custom-read-token"
        assertEquals("custom-read-token", st.tmdbToken)
        st.tmdbToken = ""
        assertEquals(BuildConfig.TMDB_READ_ACCESS_TOKEN, st.tmdbToken)
    }

    @Test fun ownerPresetIncludesCustomSurfacesAndBehaviorWithoutPersonalData() {
        val st = Settings(context(MemoryPrefs()))
        assertEquals("", st.theme)
        assertEquals("water", st.dockStyle)
        assertEquals("water", st.menuStyle)
        assertEquals("liquid", st.tileStyle)
        assertEquals("fade", st.slideAnim)
        assertEquals(800, st.transitionMs)
        assertEquals(480, st.glassRes)
        assertEquals(24, st.blurRadius)
        assertEquals(0, st.refraction)
        assertEquals(1, st.prefetchDepth)
        assertEquals(1024, st.cacheMb)
        assertEquals(180, st.idleSeconds)
        assertEquals("1080-H264", st.saverQuality)
        assertEquals("de", st.lang)
        assertFalse(st.heroGallery)
        assertFalse(st.interceptHome)
        assertEquals("", st.weatherCity)
        assertEquals("", st.haWebhook)
        assertFalse(st.setupDone)
        assertFalse(AppleTvDesign.defaults.keys.any { it.contains("token", true) || it.contains("password", true) })
    }

    @Test fun versionTwoUpgradeKeepsUserSettingsAndAddsOnlyMissingPortableKeys() {
        val old = mapOf(AppleTvDesign.VERSION_KEY to 2, "theme" to "water_still", "dockScale" to .7f,
            "idleSeconds" to 45, "accountToken" to "local", "interceptHome" to true)
        val merged = old + AppleTvDesign.migration(old)
        old.filterKeys { it != AppleTvDesign.VERSION_KEY }.forEach { (k, v) -> assertEquals(v, merged[k]) }
        assertEquals("de", merged["lang"])
        assertEquals(3, merged[AppleTvDesign.VERSION_KEY])
        assertTrue(AppleTvDesign.migration(merged).isEmpty())
    }

    @Test fun exactlyThreeThemesAndLegacyMappings() {
        assertEquals(listOf("water_still", "water", "liquid"), LauncherThemes.keys)
        for (old in listOf("clear", "amoled", "bow", "unknown")) {
            assertEquals("water_still", LauncherThemes.normalize(old))
        }
        for (key in LauncherThemes.keys) assertEquals(key, LauncherThemes.normalize(key))
        assertEquals("", LauncherThemes.normalize("", allowCustom = true))
    }

    @Test fun migrationPreservesEveryStoredChoiceAndUnrelatedValue() {
        val old = mapOf("theme" to "water", "blurRadius" to 37, "dockOrder" to "custom",
            "weatherCity" to "Berlin", "bgMode" to "video", "bgVideo" to "/my/movie.mp4",
            "token" to "private", "continueMode" to "mixed", "saverSources" to "amazon")
        val merged = old + AppleTvDesign.migration(old)
        old.forEach { (key, value) -> assertEquals(key, value, merged[key]) }
        assertEquals("custom", merged["perfMode"])
        assertTrue(AppleTvDesign.migration(merged).isEmpty())
    }

    @Test fun olderAndAlreadyVersionedBackupsCannotReintroduceRemovedThemes() {
        for (versioned in listOf(false, true)) for (old in listOf("clear", "amoled", "bow")) {
            val values = mutableMapOf<String, Any>("theme" to old, "dockStyle" to old, "tileStyle" to old, "menuStyle" to old, "panelStyle" to "clear")
            if (versioned) values[AppleTvDesign.VERSION_KEY] = AppleTvDesign.VERSION
            val migrated = values + AppleTvDesign.migration(values)
            for (key in listOf("theme", "dockStyle", "tileStyle", "menuStyle")) assertEquals("water_still", migrated[key])
            assertEquals("clear", migrated["panelStyle"])
            assertTrue(AppleTvDesign.migration(migrated).isEmpty())
        }
    }

    @Test fun freshInstallPersistsDefaultsAndRestartKeepsChanges() {
        val prefs = MemoryPrefs()
        val first = Settings(context(prefs))
        assertFalse(first.setupDone)
        AppleTvDesign.defaults.forEach { (key, expected) ->
            assertEquals(key, expected, prefs.values[key])
            // Check real Settings getter types and special list-backed screensaver setting.
            val actual = Settings::class.java.getMethod("get" + key.replaceFirstChar { it.uppercase() }).invoke(first)
            assertEquals(key, when (key) { "saverSources" -> listOf("apple"); "dockApps" -> com.glasslauncher.tv.data.DockLayout.defaults; else -> expected }, actual)
        }
        first.popularCarousel = false
        first.dockScale = 0.8f
        first.weatherCity = "Berlin"
        val restarted = Settings(context(prefs))
        assertFalse(restarted.popularCarousel)
        assertEquals(0.8f, restarted.dockScale)
        assertEquals("Berlin", restarted.weatherCity)
        assertFalse(restarted.setupDone)
    }

    @Test fun explicitPresetKeepsAccountsOrderingIconsAndMedia() {
        val prefs = MemoryPrefs()
        prefs.values.putAll(mapOf("dockOrder" to "one\ntwo", "hiddenApps" to "hidden",
            "weatherCity" to "Berlin", "bgVideo" to "/my/movie.mp4", "iconPack" to "my.pack",
            "interceptHome" to true, "bootStart" to true, "accountToken" to "private"))
        val st = Settings(context(prefs))
        st.blurRadius = 35
        st.applyAppleTvDefaults()
        AppleTvDesign.defaults.forEach { (key, value) -> assertEquals(key, value, prefs.values[key]) }
        assertEquals("Berlin", st.weatherCity)
        assertEquals(listOf("one", "two"), st.dockOrder)
        assertEquals(listOf("hidden"), st.hiddenApps)
        assertEquals("my.pack", st.iconPack)
        assertEquals("/my/movie.mp4", st.bgVideo)
        assertTrue(st.interceptHome)
        assertEquals("private", prefs.values["accountToken"])
    }

    @Test fun themeApplicationAndBalancedPresetSurviveRestart() {
        val prefs = MemoryPrefs()
        val st = Settings(context(prefs))
        for (theme in LauncherThemes.keys + listOf("bow", "clear", "amoled")) {
            st.applyTheme(theme)
            val expected = LauncherThemes.normalize(theme)
            val again = Settings(context(prefs))
            assertEquals(expected, again.theme)
            assertEquals(expected, again.dockStyle)
            assertEquals(expected, again.tileStyle)
            assertEquals(expected, again.menuStyle)
            assertEquals("clear", again.panelStyle)
        }
        st.applyMode("performance")
        st.applyMode("balanced")
        assertEquals("balanced", st.perfMode)
        for (key in listOf("imageQuality", "prefetchDepth", "transitionMs", "refraction",
            "heroGallery", "galleryShots", "glassRes", "pageScale", "memTier", "cacheMb", "slideAnim")) {
            assertEquals(key, AppleTvDesign.balanced[key], prefs.values[key])
        }
    }

    @Test fun legacyCollectionSwitchIsPreserved() {
        assertEquals("", AppleTvDesign.migration(mapOf("useAppleAerials" to false))["saverSources"])
        assertEquals("apple", AppleTvDesign.migration(mapOf("useAppleAerials" to true))["saverSources"])
    }

    @Test fun videoFallbackKeepsTheSameTitleAndTriesLowerEncodings() {
        val video = com.glasslauncher.tv.saver.AerialSource.Video("one", "One", "apple",
            mapOf("4K-HDR" to "hdr", "4K-SDR" to "sdr", "1080-H264" to "avc"), "")
        assertEquals(listOf("hdr", "sdr", "avc"), video.playbackUrls("4K-HDR"))
        assertEquals(listOf("sdr", "avc"), video.playbackUrls("4K-SDR"))
    }

    private fun context(prefs: MemoryPrefs): Context = object : ContextWrapper(null) {
        override fun getSharedPreferences(name: String, mode: Int) = prefs.proxy
    }

    /** Real in-memory persistence through Android's interfaces, with atomic editor commits. */
    private class MemoryPrefs {
        val values = mutableMapOf<String, Any>()
        val proxy = Proxy.newProxyInstance(SharedPreferences::class.java.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
            val a = args.orEmpty()
            when (method.name) {
                "getAll" -> values.toMap()
                "contains" -> values.containsKey(a[0])
                "edit" -> editor()
                "getBoolean", "getInt", "getFloat", "getString", "getLong", "getStringSet" -> values[a[0]] ?: a[1]
                else -> null
            }
        } as SharedPreferences
        private fun editor(): SharedPreferences.Editor {
            val pending = mutableMapOf<String, Any>()
            var clear = false
            lateinit var editor: SharedPreferences.Editor
            editor = Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader, arrayOf(SharedPreferences.Editor::class.java)) { _, method, args ->
                when (method.name) {
                    "commit", "apply" -> { if (clear) values.clear(); values.putAll(pending); if (method.name == "commit") true else null }
                    "clear" -> { clear = true; editor }
                    else -> { pending[args!![0] as String] = args[1]!!; editor }
                }
            } as SharedPreferences.Editor
            return editor
        }
    }
}
