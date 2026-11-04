package com.glasslauncher.tv.data

/** Selectable themes only. Matte/black renderers remain available for internal surfaces. */
object LauncherThemes {
    val choices = listOf("water_still" to "Glass still", "water" to "Glass is glass", "liquid" to "Liquid")
    val keys = choices.map { it.first }
    fun normalize(value: String, allowCustom: Boolean = false): String =
        if (value in keys || (allowCustom && value.isBlank())) value else "water_still"
    fun name(value: String): String = choices.firstOrNull { it.first == value }?.second ?: "Custom"
}

/** Portable preset captured from the configured Fire TV; accounts and device permissions stay local. */
object AppleTvDesign {
    const val VERSION = 3
    const val VERSION_KEY = "appleTvDesignVersion"
    val defaults: Map<String, Any> = linkedMapOf(
        "theme" to "",
        "dockStyle" to "water",
        "tileStyle" to "liquid",
        "menuStyle" to "water",
        "panelStyle" to "clear",
        "dockApps" to DockLayout.defaults.joinToString("\n"),
        "dockScale" to 0.9f,
        "dockFit" to true,
        "dockHide" to false,
        "dockPos" to "bottom",
        "showFolder" to false,
        "libraryDown" to true,
        "focusColor" to false,
        "focusPlate" to "light",
        "edgeBounce" to true,
        "tileLift" to false,
        "tileZoom" to "tile",
        "glow" to "white",
        "glowSize" to 0.6f,
        "cornerRadius" to 20,
        "blurRadius" to 24,
        "refraction" to 0,
        "glassTint" to 4,
        "heroAutoplay" to true,
        "slideAnim" to "apple",
        "transitionMs" to 800,
        "heroButtons" to "apple",
        "heroPlus" to true,
        "titleMode" to "logo",
        "logoWhite" to true,
        "titleScale" to 0.7f,
        "showMeta" to true,
        "metaScale" to 0.9f,
        "showGenres" to true,
        "genresScale" to 0.8f,
        "showSynopsis" to true,
        "synopsisOnFocus" to true,
        "synopsisScale" to 0.9f,
        "synopsisLines" to 4,
        "dotsMode" to "window",
        "dotsAutoHide" to true,
        "dotsMax" to 10,
        "heroGallery" to false,
        "galleryShots" to 3,
        "popularCarousel" to true,
        "shuffle" to true,
        "artChoice" to "best",
        "perCatalog" to 50,
        "maxItems" to 70,
        "surprise" to true,
        "surpriseMin" to 60,
        "descLang" to "de",
        "continueMode" to "off",
        "libraryCw" to true,
        "cwProgress" to true,
        "libraryCwSource" to true,
        "bgMode" to "carousel",
        "showClock" to true,
        "clock24h" to true,
        "showDate" to true,
        "clockPos" to "left",
        "weatherMode" to "condition",
        "topAutoHide" to false,
        "topHideSec" to 3,
        "menuMode" to "auto",
        "menuScale" to 0.95f,
        "menuClock" to true,
        "menuDate" to true,
        "menuWeather" to true,
        "menuCity" to true,
        "menuLibrary" to true,
        "menuAndroid" to true,
        "menuWifi" to true,
        "menuThemes" to false,
        "menuBackground" to true,
        "menuSaver" to true,
        "menuCamera" to false,
        "menuSys" to true,
        "saverMode" to "aerial",
        "useAppleAerials" to true,
        "saverSources" to "apple",
        "saverMine" to false,
        "saverQuality" to "1080-H264",
        "saverShuffle" to true,
        "saverFade" to true,
        "saverCache" to true,
        "saverCacheGb" to 4,
        "saverStyle" to "apple",
        "saverClock" to true,
        "saverDate" to true,
        "saverWeather" to true,
        "saverPlace" to true,
        "saverSys" to false,
        "saverPos" to "move",
        "saverSize" to 0.8f,
        "saverShowSec" to 10,
        "backSaver" to true,
        "libraryPosterSize" to 1.0f,
        "libraryColumns" to 5,
        "librarySpacing" to 1.1f,
        "libraryTitles" to false,
        "libraryCwShape" to "backdrop",
        "libraryCwSize" to 1.0f,
        "perfMode" to "custom",
        "glassRes" to 480,
        "imageQuality" to "hd",
        "cacheMb" to 1024,
        "prefetchDepth" to 1,
        "memTier" to 0,
        "pageScale" to 0.667f,
        "lang" to "de",
        "idleSeconds" to 180,
        "libraryTrakt" to true,
        "libraryNuvio" to true,
        "catalogsNuvio" to true,
        "cwLegacy" to true,
        "cwTrakt" to false,
        "cwLead" to 0,
        "sysCpu" to true,
        "sysGpu" to true,
        "bootStart" to false

    )

    /** Named performance presets stay independent of the owner's customized install defaults. */
    val balanced: Map<String, Any> = mapOf(
        "imageQuality" to "hd", "prefetchDepth" to 6, "transitionMs" to 700,
        "refraction" to 6, "heroGallery" to true, "galleryShots" to 3,
        "glassRes" to 720, "pageScale" to 1f, "memTier" to 1,
        "cacheMb" to 2048, "slideAnim" to "apple"
    )

    @Suppress("UNCHECKED_CAST")
    fun <T> value(key: String): T = defaults.getValue(key) as T

    /**
     * Old preferences have no provenance: even a value equal to an old default may be intentional.
     * Only missing keys receive the preset. Theme sanitation also runs on every backup restore,
     * even if that backup already carries the version marker.
     */
    fun migration(stored: Map<String, *>): Map<String, Any> = buildMap {
        if ((stored[VERSION_KEY] as? Int ?: 0) < VERSION) {
            defaults.forEach { (key, value) -> if (key !in stored) put(key, value) }
            // Preserve the older collection switch when no explicit collection list exists.
            if ("saverSources" !in stored && stored["useAppleAerials"] is Boolean) {
                put("saverSources", if (stored["useAppleAerials"] == true) "apple" else "")
            }
            put(VERSION_KEY, VERSION)
        }
        listOf("theme", "dockStyle", "tileStyle", "menuStyle").forEach { key ->
            (stored[key] as? String)?.let { old ->
                val normalized = LauncherThemes.normalize(old, allowCustom = key == "theme")
                if (old != normalized) put(key, normalized)
            }
        }
    }
}
