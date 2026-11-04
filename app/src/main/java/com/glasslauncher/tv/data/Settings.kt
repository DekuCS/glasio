package com.glasslauncher.tv.data

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf

/**
 * Every launcher option lives here. Reads touch a Compose state counter, so any composable that
 * reads a property recomposes automatically when a setter bumps it.
 */
val MENU_KEYS = listOf("library", "sliders", "gear", "wifi", "theme", "camera", "saver", "background")
/** The menu's order on a new TV. */
val MENU_DEFAULT = listOf("library", "camera", "sliders", "gear", "wifi", "saver", "theme", "background")
val MENU_NAMES = mapOf("library" to "Library", "sliders" to "Glassio settings", "gear" to "Android settings", "wifi" to "Wi-Fi",
    "theme" to "Themes", "camera" to "Camera", "saver" to "Screensaver", "background" to "Background")

class Settings(private val ctx: Context) {
    private val sp = ctx.getSharedPreferences("glass", Context.MODE_PRIVATE)
    private val rev = mutableIntStateOf(0)

    init {
        // Detect an existing installation BEFORE adding the preset's keys.
        firstRun()
        migrateDesign()
    }

    private fun writeValues(values: Map<String, Any>) {
        if (values.isEmpty()) return
        val editor = sp.edit()
        values.forEach { (key, value) ->
            when (value) {
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Float -> editor.putFloat(key, value)
                is String -> editor.putString(key, value)
                else -> error("Unsupported design value: $key")
            }
        }
        check(editor.commit()) { "Could not persist design settings" }
        touch()
    }

    fun migrateDesign() = writeValues(AppleTvDesign.migration(sp.all))

    /** Explicit opt-in for existing users; accounts, icons, app order and media files are untouched. */
    fun applyAppleTvDefaults() {
        writeValues(AppleTvDesign.defaults + (AppleTvDesign.VERSION_KEY to AppleTvDesign.VERSION))
        I18n.lang = lang
        com.glasslauncher.tv.ui.Glow.amount = glowSize
        com.glasslauncher.tv.ui.GlassCache.res = glassRes
    }

    private fun touch() { rev.intValue = rev.intValue + 1 }
    /** Test only: every stored value (am start --ez dump_prefs true → logcat GlassPrefs). */
    fun dumpAll(): Map<String, *> = sp.all

    private fun b(k: String, d: Boolean): Boolean { rev.intValue; return sp.getBoolean(k, d) }
    private fun i(k: String, d: Int): Int { rev.intValue; return sp.getInt(k, d) }
    private fun f(k: String, d: Float): Float { rev.intValue; return sp.getFloat(k, d) }
    private fun s(k: String, d: String): String { rev.intValue; return sp.getString(k, d) ?: d }
    private fun putB(k: String, v: Boolean) { sp.edit().putBoolean(k, v).apply(); touch() }
    private fun putI(k: String, v: Int) { sp.edit().putInt(k, v).apply(); touch() }
    private fun putF(k: String, v: Float) { sp.edit().putFloat(k, v).apply(); touch() }
    private fun putS(k: String, v: String) { sp.edit().putString(k, v).apply(); touch() }
    private fun list(k: String): List<String> =
        s(k, "").split('\n').map { it.trim() }.filter { it.isNotEmpty() }
    private fun putList(k: String, v: List<String>) = putS(k, v.joinToString("\n"))

    // ---- Hero / carousel
    var heroAutoplay: Boolean get() = b("heroAutoplay", AppleTvDesign.value("heroAutoplay")); set(v) = putB("heroAutoplay", v)
    /** auto | performance | balanced | quality | custom */
    var perfMode: String get() = s("perfMode", AppleTvDesign.value("perfMode")); set(v) = putS("perfMode", v)

    /**
     * What this TV can carry, from its memory and GPU: 2 GB / GLES 2 boxes get "performance",
     * ~4 GB boxes (Google TV Streamer, Shield) "balanced", bigger ones "quality".
     */
    fun deviceTier(): String {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val mi = android.app.ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val gb = mi.totalMem / (1024.0 * 1024 * 1024)
        val gles = am.deviceConfigurationInfo.reqGlEsVersion
        val cores = Runtime.getRuntime().availableProcessors()
        return when {
            am.isLowRamDevice || gb < 2.6 || gles < 0x30000 || android.os.Build.VERSION.SDK_INT < 33 -> "performance"
            gb < 5.0 || cores < 6 -> "balanced"
            else -> "quality"
        }
    }
    /** The tier Auto picked, for display. */
    val autoTier: String by lazy { deviceTier() }

    /** The page picture is prepared at this fraction of the screen size (2/3 in Performance: 2.25× less memory and drawing). */
    var pageScale: Float get() = f("pageScale", AppleTvDesign.value("pageScale")); set(v) = putF("pageScale", if (v < 0.9f) 0.667f else 1f)
    /** 0 small, 1 medium, 2 large memory caches (pictures kept ready); set by the mode. */
    var memTier: Int get() = i("memTier", AppleTvDesign.value("memTier")); set(v) = putI("memTier", v.coerceIn(0, 2))

    /** Glass copies of the backdrop: 480 px (light) or 720 px (crisper glass, more memory). */
    var glassRes: Int get() = i("glassRes", AppleTvDesign.value("glassRes")); set(v) = putI("glassRes", if (v >= 720) 720 else 480)

    /** Which picture a title uses: main | second | best (highest resolution) | addon (the add-on's own). */
    var artChoice: String get() = s("artChoice", AppleTvDesign.value("artChoice")); set(v) = putS("artChoice", v)
    /** Per-title picture picked with a long press on the carousel: "key\turl" lines. */
    fun artPick(key: String): String? = list("artPicks").firstOrNull { it.startsWith("$key\t") }?.substringAfter('\t')
    fun setArtPick(key: String, url: String?) {
        val rest = list("artPicks").filterNot { it.startsWith("$key\t") }
        putList("artPicks", if (url == null) rest else (rest + "$key\t$url").takeLast(300))
    }
    fun clearArtPicks() = putList("artPicks", emptyList())
    /** max = original pictures (4K source, sharpest) | high = 1280 px pictures (faster, lighter) */
    var imageQuality: String get() = s("imageQuality", AppleTvDesign.value("imageQuality")); set(v) = putS("imageQuality", v)
    /** How many slides ahead are prepared (pictures, logos, glass). */
    var prefetchDepth: Int get() = i("prefetchDepth", AppleTvDesign.value("prefetchDepth")); set(v) = putI("prefetchDepth", v.coerceIn(1, 8))

    fun applyMode(m: String) {
        // Auto: a weak TV gets the light preset; any other keeps the defaults (made on a 4 GB Google TV Streamer).
        val tier = if (m == "auto") deviceTier().takeIf { it == "performance" } else m
        when (tier) {
            // Performance: pictures at 1280 px and drawn at 2/3 resolution, no rim refraction (no GPU effects at
            // all), a plain crossfade and small caches: for 2 GB TVs and older graphics chips.
            "performance" -> { imageQuality = "high"; prefetchDepth = 2; transitionMs = 300; refraction = 0; heroGallery = false; glassRes = 480
                pageScale = 0.667f; slideAnim = "fade"; memTier = 0; saverQuality = "1080-H264"; cacheMb = 1024 }
            "balanced" -> writeValues(AppleTvDesign.balanced)
            "quality" -> { imageQuality = "max"; prefetchDepth = 6; transitionMs = 700; refraction = 14; heroGallery = true; galleryShots = 3; glassRes = 720
                pageScale = 1f; memTier = 2 }
        }
        perfMode = m
    }

    /** Apple TV style: each title shows a few of its backdrops before the carousel moves on. */
    var heroGallery: Boolean get() = b("heroGallery", AppleTvDesign.value("heroGallery")); set(v) = putB("heroGallery", v)
    var galleryShots: Int get() = i("galleryShots", AppleTvDesign.value("galleryShots")); set(v) = putI("galleryShots", v.coerceIn(2, 6))
    var heroIntervalSec: Int get() = i("heroIntervalSec", 10); set(v) = putI("heroIntervalSec", v.coerceIn(3, 60))
    /** logo | text | hidden */
    var titleMode: String get() = s("titleMode", AppleTvDesign.value("titleMode")); set(v) = putS("titleMode", v)
    var titleScale: Float get() = f("titleScale", AppleTvDesign.value("titleScale")); set(v) = putF("titleScale", v.coerceIn(0.5f, 2.2f))
    var showMeta: Boolean get() = b("showMeta", AppleTvDesign.value("showMeta")); set(v) = putB("showMeta", v)
    var showGenres: Boolean get() = b("showGenres", AppleTvDesign.value("showGenres")); set(v) = putB("showGenres", v)
    var showSynopsis: Boolean get() = b("showSynopsis", AppleTvDesign.value("showSynopsis")); set(v) = putB("showSynopsis", v)
    /** Synopsis appears only while the carousel is selected. */
    var synopsisOnFocus: Boolean get() = b("synopsisOnFocus", AppleTvDesign.value("synopsisOnFocus")); set(v) = putB("synopsisOnFocus", v)
    /** Back on the home screen starts the screensaver (like Apple TV). */
    var backSaver: Boolean get() = b("backSaver", AppleTvDesign.value("backSaver")); set(v) = putB("backSaver", v)
    var synopsisScale: Float get() = f("synopsisScale", AppleTvDesign.value("synopsisScale")); set(v) = putF("synopsisScale", v.coerceIn(0.6f, 2.0f))
    var synopsisLines: Int get() = i("synopsisLines", AppleTvDesign.value("synopsisLines")); set(v) = putI("synopsisLines", v.coerceIn(1, 8))
    /** window (dots on a pill) | liquid (liquid-glass drops, no pill) | progress | hidden */
    var dotsMode: String get() = s("dotsMode", AppleTvDesign.value("dotsMode")); set(v) = putS("dotsMode", v)
    /** apple (Apple TV: the new picture opens up from the side with parallax) | slide (push) | fade */
    var slideAnim: String get() = s("slideAnim", AppleTvDesign.value("slideAnim")); set(v) = putS("slideAnim", v)
    // ---- Your own background
    /** carousel (each title's backdrop) | image | video */
    var bgMode: String get() = s("bgMode", AppleTvDesign.value("bgMode")); set(v) = putS("bgMode", v)
    /** A link or a file path. */
    var bgImage: String get() = s("bgImage", ""); set(v) = putS("bgImage", v)
    var bgVideo: String get() = s("bgVideo", ""); set(v) = putS("bgVideo", v)

    // ---- Menu (from the settings button) and Library
    /** auto (opens when you go up to the settings button) | manual (OK on it) | off (it opens Settings) */
    var menuMode: String get() = s("menuMode", AppleTvDesign.value("menuMode")); set(v) = putS("menuMode", v)
    var menuClock: Boolean get() = b("menuClock", AppleTvDesign.value("menuClock")); set(v) = putB("menuClock", v)
    var menuDate: Boolean get() = b("menuDate", AppleTvDesign.value("menuDate")); set(v) = putB("menuDate", v)
    var menuWeather: Boolean get() = b("menuWeather", AppleTvDesign.value("menuWeather")); set(v) = putB("menuWeather", v)
    var menuCity: Boolean get() = b("menuCity", AppleTvDesign.value("menuCity")); set(v) = putB("menuCity", v)
    var menuLibrary: Boolean get() = b("menuLibrary", AppleTvDesign.value("menuLibrary")); set(v) = putB("menuLibrary", v)
    /** The menu's rows in your order (keys: library, sliders, gear, wifi, theme, camera, saver, background). */
    var menuOrder: List<String>
        get() = (if (sp.contains("menuOrder")) list("menuOrder") else MENU_DEFAULT).filter { it in MENU_KEYS }.let { it + MENU_KEYS.filter { k -> k !in it } }
        set(v) = putList("menuOrder", v)
    var menuAndroid: Boolean get() = b("menuAndroid", AppleTvDesign.value("menuAndroid")); set(v) = putB("menuAndroid", v)
    var menuWifi: Boolean get() = b("menuWifi", AppleTvDesign.value("menuWifi")); set(v) = putB("menuWifi", v)
    var menuThemes: Boolean get() = b("menuThemes", AppleTvDesign.value("menuThemes")); set(v) = putB("menuThemes", v)
    var menuBackground: Boolean get() = b("menuBackground", AppleTvDesign.value("menuBackground")); set(v) = putB("menuBackground", v)
    /** Size of the menu (0.8 – 1.3). */
    var menuScale: Float get() = f("menuScale", AppleTvDesign.value("menuScale")); set(v) = putF("menuScale", v.coerceIn(0.8f, 1.3f))
    /** The TV's numbers under the weather in the menu. */
    var menuSys: Boolean get() = b("menuSys", AppleTvDesign.value("menuSys")); set(v) = putB("menuSys", v)
    var sysIp: Boolean get() = b("sysIp", false); set(v) = putB("sysIp", v)
    var sysCpu: Boolean get() = b("sysCpu", AppleTvDesign.value("sysCpu")); set(v) = putB("sysCpu", v)
    var sysGpu: Boolean get() = b("sysGpu", AppleTvDesign.value("sysGpu")); set(v) = putB("sysGpu", v)
    var sysRam: Boolean get() = b("sysRam", true); set(v) = putB("sysRam", v)
    var sysStorage: Boolean get() = b("sysStorage", true); set(v) = putB("sysStorage", v)
    /** Cameras, one per line: "Name|rtsp://…" (RTSP, HLS or a plain video link). */
    var cameras: List<String> get() = list("cameras"); set(v) = putList("cameras", v)
    var menuCamera: Boolean get() = b("menuCamera", AppleTvDesign.value("menuCamera")); set(v) = putB("menuCamera", v)
    var menuSaver: Boolean get() = b("menuSaver", AppleTvDesign.value("menuSaver")); set(v) = putB("menuSaver", v)
    /** Look of the menu: same choices as the dock (set by Themes). */
    var menuStyle: String get() = LauncherThemes.normalize(s("menuStyle", AppleTvDesign.value("menuStyle"))); set(v) = putS("menuStyle", LauncherThemes.normalize(v))
    /** The + button next to Play (save to Library). */
    var heroPlus: Boolean get() = b("heroPlus", AppleTvDesign.value("heroPlus")); set(v) = putB("heroPlus", v)
    var libraryTrakt: Boolean get() = b("libraryTrakt", AppleTvDesign.value("libraryTrakt")); set(v) = putB("libraryTrakt", v)
    /** The Nuvio account's Library (Nuvio Legacy, official Nuvio and forks that sync with the account). */
    var libraryNuvio: Boolean get() = b("libraryNuvio", AppleTvDesign.value("libraryNuvio")); set(v) = putB("libraryNuvio", v)
    /** Catalogs also come from the add-ons installed on the Nuvio account (merged with the TV's own, never twice). */
    var catalogsNuvio: Boolean get() = b("catalogsNuvio", AppleTvDesign.value("catalogsNuvio")); set(v) = putB("catalogsNuvio", v)
    /** The + button also saves to the Nuvio account's Library. */
    var libraryToNuvio: Boolean get() = b("libraryToNuvio", true); set(v) = putB("libraryToNuvio", v)
    var libraryToTrakt: Boolean get() = b("libraryToTrakt", true); set(v) = putB("libraryToTrakt", v)
    var libraryTitles: Boolean get() = b("libraryTitles", AppleTvDesign.value("libraryTitles")); set(v) = putB("libraryTitles", v)
    /** water_still | water | liquid; blank means individually customized surfaces. */
    var theme: String get() = LauncherThemes.normalize(s("theme", AppleTvDesign.value("theme")), allowCustom = true); set(v) = putS("theme", LauncherThemes.normalize(v, allowCustom = true))

    /** One selectable look for the dock, icons and menu; settings retain their internal surface. */
    fun applyTheme(t: String) {
        val style = LauncherThemes.normalize(t)
        writeValues(mapOf("theme" to style, "dockStyle" to style, "tileStyle" to style, "menuStyle" to style))
    }

    /** Buttons under the title: apple (Play + round Next, like Apple TV) | play (Play only, when selected) | hidden */
    var heroButtons: String get() = s("heroButtons", AppleTvDesign.value("heroButtons")); set(v) = putS("heroButtons", v)
    var dotsMax: Int get() = i("dotsMax", AppleTvDesign.value("dotsMax")); set(v) = putI("dotsMax", v.coerceIn(3, 20))
    var perCatalog: Int get() = i("perCatalog", AppleTvDesign.value("perCatalog")); set(v) = putI("perCatalog", v.coerceIn(5, 500))
    /** 0 = unlimited */
    var maxItems: Int get() = i("maxItems", AppleTvDesign.value("maxItems")); set(v) = putI("maxItems", v.coerceAtLeast(0))
    /** Ranked discovery feeds; manual catalog selections remain stored separately. */
    var popularCarousel: Boolean get() = b("popularCarousel", AppleTvDesign.value("popularCarousel")); set(v) = putB("popularCarousel", v)
    var shuffle: Boolean get() = b("shuffle", AppleTvDesign.value("shuffle")); set(v) = putB("shuffle", v)
    /** Surprise me: a random mix from all the catalogs you chose (different pages each time), fetched again every [surpriseMin]. */
    var surprise: Boolean get() = b("surprise", AppleTvDesign.value("surprise")); set(v) = putB("surprise", v)
    var surpriseMin: Int get() = i("surpriseMin", AppleTvDesign.value("surpriseMin")); set(v) = putI("surpriseMin", v.coerceIn(10, 1440))
    /** Apple style: the title's logo all in white. */
    var logoWhite: Boolean get() = b("logoWhite", AppleTvDesign.value("logoWhite")); set(v) = putB("logoWhite", v)
    /** Picture cache on the TV, in MB (applies after Glassio restarts). */
    var cacheMb: Int get() = i("cacheMb", AppleTvDesign.value("cacheMb")); set(v) = putI("cacheMb", v.coerceIn(256, 8192))
    /** Language of synopses and titles from TMDB: en | ar | … */
    var descLang: String get() = s("descLang", AppleTvDesign.value("descLang")); set(v) = putS("descLang", v)
    var tmdbToken: String get() = s("tmdbToken", "").ifBlank { com.glasslauncher.tv.BuildConfig.TMDB_READ_ACCESS_TOKEN }; set(v) = putS("tmdbToken", v.trim())

    var transitionMs: Int get() = i("transitionMs", AppleTvDesign.value("transitionMs")); set(v) = putI("transitionMs", v.coerceIn(100, 2000))
    var dotsAutoHide: Boolean get() = b("dotsAutoHide", AppleTvDesign.value("dotsAutoHide")); set(v) = putB("dotsAutoHide", v)

    // ---- Clock / dock look
    var topAutoHide: Boolean get() = b("topAutoHide", AppleTvDesign.value("topAutoHide")); set(v) = putB("topAutoHide", v)
    var topHideSec: Int get() = i("topHideSec", AppleTvDesign.value("topHideSec")); set(v) = putI("topHideSec", v.coerceIn(2, 30))
    var cornerRadius: Int get() = i("cornerRadius", AppleTvDesign.value("cornerRadius")); set(v) = putI("cornerRadius", v.coerceIn(0, 40))
    /** Index of the Apps folder among the dock apps; -1 = after the last app. */
    var folderPos: Int get() = i("folderPos", -1); set(v) = putI("folderPos", v)
    /** Continue Watching: off | first (at the start of the carousel) | mixed (among the catalogs) */
    var continueMode: String get() = s("continueMode", AppleTvDesign.value("continueMode")); set(v) = putS("continueMode", v)
    /** Continue Watching also from the Trakt account (when one is connected). */
    var cwTrakt: Boolean get() = b("cwTrakt", AppleTvDesign.value("cwTrakt")); set(v) = putB("cwTrakt", v)
    var cwStremio: Boolean get() = b("cwStremio", true); set(v) = putB("cwStremio", v)
    var cwSimkl: Boolean get() = b("cwSimkl", true); set(v) = putB("cwSimkl", v)
    /** What you watch on the Nuvio account (Legacy, the official app and forks push it there). */
    var cwNuvioAccount: Boolean get() = b("cwNuvioAccount", true); set(v) = putB("cwNuvioAccount", v)
    /** Catalogs from the add-ons on the Stremio account. */
    var catalogsStremio: Boolean get() = b("catalogsStremio", false); set(v) = putB("catalogsStremio", v)
    /** Continue Watching at the top of the Library: on, card shape (backdrop | poster), size, where it came from. */
    var libraryCw: Boolean get() = b("libraryCw", AppleTvDesign.value("libraryCw")); set(v) = putB("libraryCw", v)
    var libraryCwShape: String get() = s("libraryCwShape", AppleTvDesign.value("libraryCwShape")); set(v) = putS("libraryCwShape", v)
    var libraryCwSize: Float get() = f("libraryCwSize", AppleTvDesign.value("libraryCwSize")); set(v) = putF("libraryCwSize", v.coerceIn(0.7f, 1.5f))
    /** Library posters: size (1 = fills the row), how many in a row, and the space between them (1 = normal). */
    var libraryPosterSize: Float get() = f("libraryPosterSize", AppleTvDesign.value("libraryPosterSize")); set(v) = putF("libraryPosterSize", v.coerceIn(0.6f, 1.0f))
    var libraryColumns: Int get() = i("libraryColumns", AppleTvDesign.value("libraryColumns")); set(v) = putI("libraryColumns", v.coerceIn(3, 8))
    var librarySpacing: Float get() = f("librarySpacing", AppleTvDesign.value("librarySpacing")); set(v) = putF("librarySpacing", v.coerceIn(0.5f, 2.0f))
    var libraryCwSource: Boolean get() = b("libraryCwSource", AppleTvDesign.value("libraryCwSource")); set(v) = putB("libraryCwSource", v)
    /** The Simkl "Plan to watch" list in the Library, and + saves there too. */
    var librarySimkl: Boolean get() = b("librarySimkl", true); set(v) = putB("librarySimkl", v)
    var libraryToSimkl: Boolean get() = b("libraryToSimkl", true); set(v) = putB("libraryToSimkl", v)
    /** How many titles in progress open the carousel (0 = all); the rest mix in with the catalogs. */
    var cwLead: Int get() = i("cwLead", AppleTvDesign.value("cwLead")); set(v) = putI("cwLead", v)
    /** After you leave the player, the carousel opens on what you just watched. */
    var continueJump: Boolean get() = b("continueJump", true); set(v) = putB("continueJump", v)
    var cwLegacy: Boolean get() = b("cwLegacy", AppleTvDesign.value("cwLegacy")); set(v) = putB("cwLegacy", v)
    var cwProgress: Boolean get() = b("cwProgress", AppleTvDesign.value("cwProgress")); set(v) = putB("cwProgress", v)
    /** 0.6 .. 1.0 (1.0 = full width) */
    var dockScale: Float get() = f("dockScale", AppleTvDesign.value("dockScale")); set(v) = putF("dockScale", v.coerceIn(0.6f, 1.0f))
    /** Background of the dock and Apps folder: water_still | water | liquid. */
    var dockStyle: String get() = LauncherThemes.normalize(s("dockStyle", AppleTvDesign.value("dockStyle"))); set(v) = putS("dockStyle", LauncherThemes.normalize(v))
    /** Background of settings and the icon picker: liquid | clear | amoled */
    var panelStyle: String get() = s("panelStyle", AppleTvDesign.value("panelStyle")); set(v) = putS("panelStyle", v)
    /** Icon plates: water_still | water | liquid. */
    var tileStyle: String get() = LauncherThemes.normalize(s("tileStyle", AppleTvDesign.value("tileStyle"))); set(v) = putS("tileStyle", LauncherThemes.normalize(v))
    /** How far the glass rim bends the background, dp. */
    var refraction: Int get() = i("refraction", AppleTvDesign.value("refraction")); set(v) = putI("refraction", v.coerceIn(0, 40))
    var metaScale: Float get() = f("metaScale", AppleTvDesign.value("metaScale")); set(v) = putF("metaScale", v.coerceIn(0.6f, 2.0f))
    var genresScale: Float get() = f("genresScale", AppleTvDesign.value("genresScale")); set(v) = putF("genresScale", v.coerceIn(0.6f, 2.0f))
    /** Opaque dark plate behind every icon, so transparent pack icons don't show the poster through. */
    var tileBacking: Boolean get() = b("tileBacking", true); set(v) = putB("tileBacking", v)
    /** package name of the selected icon pack, "" = none */
    var iconPack: String get() = s("iconPack", ""); set(v) = putS("iconPack", v)
    /** Weather in the top bar: off | temp | condition | city | all */
    var weatherMode: String get() = s("weatherMode", AppleTvDesign.value("weatherMode")); set(v) = putS("weatherMode", v)
    var weatherCity: String get() = s("weatherCity", ""); set(v) = putS("weatherCity", v.trim())
    var weatherF: Boolean get() = b("weatherF", false); set(v) = putB("weatherF", v)
    var showDate: Boolean get() = b("showDate", AppleTvDesign.value("showDate")); set(v) = putB("showDate", v)
    /** tvOS look: icons are white until selected, then show their colours on a light plate. */
    var focusColor: Boolean get() = b("focusColor", AppleTvDesign.value("focusColor")); set(v) = putB("focusColor", v)
    /** Plate behind the selected icon in "Colour on focus": light | dark | glass | amoled (pure black) */
    var focusPlate: String get() = s("focusPlate", AppleTvDesign.value("focusPlate")); set(v) = putS("focusPlate", v)
    /** Clock: left (Apple TV) | right (next to the settings button, weather under it) */
    var clockPos: String get() = s("clockPos", AppleTvDesign.value("clockPos")); set(v) = putS("clockPos", v)
    /** Pressing further at the edge of the dock nudges the icon, like tvOS. */
    var edgeBounce: Boolean get() = b("edgeBounce", AppleTvDesign.value("edgeBounce")); set(v) = putB("edgeBounce", v)
    /** The selected icon rises a little, like tvOS. */
    var tileLift: Boolean get() = b("tileLift", AppleTvDesign.value("tileLift")); set(v) = putB("tileLift", v)
    var showClock: Boolean get() = b("showClock", AppleTvDesign.value("showClock")); set(v) = putB("showClock", v)
    var clock24h: Boolean get() = b("clock24h", AppleTvDesign.value("clock24h")); set(v) = putB("clock24h", v)
    var blurRadius: Int get() = i("blurRadius", AppleTvDesign.value("blurRadius")); set(v) = putI("blurRadius", v.coerceIn(0, 80))
    var glassTint: Int get() = i("glassTint", AppleTvDesign.value("glassTint")); set(v) = putI("glassTint", v.coerceIn(0, 40))
    var showMobileApps: Boolean get() = b("showMobileApps", true); set(v) = putB("showMobileApps", v)
    /** Pinned dock entries, separate from all installed apps and their ordering. */
    var dockApps: List<String>
        get() = if (sp.contains("dockApps")) list("dockApps") else DockLayout.defaults
        set(v) = putList("dockApps", v.distinct())
    var dockOrder: List<String> get() = list("dockOrder"); set(v) = putList("dockOrder", v)
    var hiddenApps: List<String> get() = list("hiddenApps"); set(v) = putList("hiddenApps", v)
    /** The Apps folder tile in the dock. */
    var showFolder: Boolean get() = b("showFolder", AppleTvDesign.value("showFolder")); set(v) = putB("showFolder", v)
    /** With fewer apps than a full row, the dock shrinks around them. */
    var dockFit: Boolean get() = b("dockFit", AppleTvDesign.value("dockFit")); set(v) = putB("dockFit", v)
    /** The dock hides below the screen and comes up when you go down to it. */
    var dockHide: Boolean get() = b("dockHide", AppleTvDesign.value("dockHide")); set(v) = putB("dockHide", v)
    /** With your own background (picture, video, screensaver): bottom | center | left (a column on the left). */
    var dockPos: String get() = s("dockPos", AppleTvDesign.value("dockPos")); set(v) = putS("dockPos", v)
    /** Down from the dock's last row opens the Library. */
    var libraryDown: Boolean get() = b("libraryDown", AppleTvDesign.value("libraryDown")); set(v) = putB("libraryDown", v)

    // ---- Nuvio / Home Assistant
    /** auto (first installed: Legacy, official, Arc, Re-Shaped) | legacy | official | arc | reshaped */
    var nuvioTarget: String get() = s("nuvioTarget", "auto"); set(v) = putS("nuvioTarget", v)
    var haWebhook: String get() = s("haWebhook", ""); set(v) = putS("haWebhook", v.trim())
    var haOnLaunch: Boolean get() = b("haOnLaunch", false); set(v) = putB("haOnLaunch", v)

    // ---- Screensaver
    var idleMinutes: Int get() = i("idleMinutes", 0); set(v) = putI("idleMinutes", v.coerceAtLeast(0))
    /** Idle time before the screensaver, in seconds (0 = off). Older versions kept whole minutes in idleMinutes. */
    var idleSeconds: Int get() = if (sp.contains("idleSeconds")) i("idleSeconds", AppleTvDesign.value("idleSeconds")) else if (sp.contains("idleMinutes")) i("idleMinutes", 0) * 60 else AppleTvDesign.value("idleSeconds")
        set(v) = putI("idleSeconds", v.coerceAtLeast(0))
    /** aerial (Apple videos) | slideshow (posters with logos) | system (the TV's own screensaver, e.g. Aerial Views) */
    var saverMode: String get() = s("saverMode", AppleTvDesign.value("saverMode")); set(v) = putS("saverMode", v)
    var useAppleAerials: Boolean get() = b("useAppleAerials", AppleTvDesign.value("useAppleAerials")); set(v) = putB("useAppleAerials", v)
    var aerialUrls: List<String> get() = list("aerialUrls"); set(v) = putList("aerialUrls", v)
    var saverClock: Boolean get() = b("saverClock", AppleTvDesign.value("saverClock")); set(v) = putB("saverClock", v)
    /** Collections of aerial videos that are on (apple, amazon, jetson, robin); see AerialSource. */
    var saverSources: List<String> get() = if (sp.contains("saverSources")) list("saverSources").filter { it.isNotBlank() } else if (sp.contains("useAppleAerials")) (if (useAppleAerials) listOf("apple") else emptyList()) else listOf(AppleTvDesign.value<String>("saverSources"))
        set(v) = putList("saverSources", v)
    /** Your own videos (links and the backgrounds folder) in the screensaver. */
    var saverMine: Boolean get() = b("saverMine", AppleTvDesign.value("saverMine")); set(v) = putB("saverMine", v)
    var saverQuality: String get() = s("saverQuality", AppleTvDesign.value("saverQuality")); set(v) = putS("saverQuality", v)
    var saverShuffle: Boolean get() = b("saverShuffle", AppleTvDesign.value("saverShuffle")); set(v) = putB("saverShuffle", v)
    /** One video only (its id), or "" for all of them. */
    var saverPick: String get() = s("saverPick", ""); set(v) = putS("saverPick", v)
    /** Videos are kept on the TV as they play (any link), up to [saverCacheGb]. */
    var saverCache: Boolean get() = b("saverCache", AppleTvDesign.value("saverCache")); set(v) = putB("saverCache", v)
    var saverCacheGb: Int get() = i("saverCacheGb", AppleTvDesign.value("saverCacheGb")); set(v) = putI("saverCacheGb", v.coerceIn(1, 32))
    /** Cross-fade from one video to the next. */
    var saverFade: Boolean get() = b("saverFade", AppleTvDesign.value("saverFade")); set(v) = putB("saverFade", v)
    var saverDate: Boolean get() = b("saverDate", AppleTvDesign.value("saverDate")); set(v) = putB("saverDate", v)
    var saverWeather: Boolean get() = b("saverWeather", AppleTvDesign.value("saverWeather")); set(v) = putB("saverWeather", v)
    /** The place the video shows (Apple, Amazon and community videos have one). */
    var saverPlace: Boolean get() = b("saverPlace", AppleTvDesign.value("saverPlace")); set(v) = putB("saverPlace", v)
    var saverSys: Boolean get() = b("saverSys", AppleTvDesign.value("saverSys")); set(v) = putB("saverSys", v)
    /** apple (clock in a corner, small text) | netflix (lines appear one by one at the top left, like the notes before a film) */
    var saverStyle: String get() = s("saverStyle", AppleTvDesign.value("saverStyle")); set(v) = putS("saverStyle", v)
    /** bl | br | tl | tr | move (another corner with every video) */
    var saverPos: String get() = s("saverPos", AppleTvDesign.value("saverPos")); set(v) = putS("saverPos", v)
    var saverSize: Float get() = f("saverSize", AppleTvDesign.value("saverSize")); set(v) = putF("saverSize", v.coerceIn(0.6f, 1.8f))
    /** Seconds the info stays after each new video (0 = always). */
    var saverShowSec: Int get() = i("saverShowSec", AppleTvDesign.value("saverShowSec")); set(v) = putI("saverShowSec", v.coerceAtLeast(0))
    /** Netflix style: the lines go away a few seconds after the last one came in. */
    var saverNetflixHide: Boolean get() = b("saverNetflixHide", true); set(v) = putB("saverNetflixHide", v)
    /** Netflix style: the bar before each line — "red", "white", "black" or "off". */
    var saverBar: String get() = s("saverBar", "white"); set(v) = putS("saverBar", v)
    /** How far and how strong the glow is (0.4 … 1.6). */
    var glowSize: Float get() = f("glowSize", AppleTvDesign.value("glowSize")); set(v) { putF("glowSize", v.coerceIn(0.3f, 1.6f)); com.glasslauncher.tv.ui.Glow.amount = v.coerceIn(0.3f, 1.6f) }
    /** Dock: on focus the whole tile grows ("tile"), only the icon inside the glass ("icon"), or nothing ("off"). */
    var tileZoom: String get() = s("tileZoom", AppleTvDesign.value("tileZoom")); set(v) = putS("tileZoom", v)
    /** A very soft glow around the selected icon / poster: "off", "color" (its own colour) or "white". */
    var glow: String get() = s("glow", AppleTvDesign.value("glow")); set(v) = putS("glow", v)
    /** Hold Home: "recents", "camera" (live camera over any app), "library", "menu" or "off". */
    var homeLong: String get() = s("homeLong", if (b("homeRecents", false)) "recents" else "off"); set(v) = putS("homeLong", v)
    /** Ask before removing a title from the Library. */
    var libraryConfirm: Boolean get() = b("libraryConfirm", true); set(v) = putB("libraryConfirm", v)
    /** Background video: a new one when each ends (else the same one loops). */
    var bgShuffle: Boolean get() = b("bgShuffle", true); set(v) = putB("bgShuffle", v)

    /** Existing installs skip the first-run setup (it is for new TVs). */
    fun firstRun() { if (!sp.contains("setupDone")) putB("setupDone", sp.all.size > 8) }

    /** Testing over adb (see DevHooks): key=value — true/false, a number, or text; lists with \n. */
    fun debugSet(kv: String) {
        val k = kv.substringBefore('='); val v = kv.substringAfter('=')
        when {
            v == "true" || v == "false" -> putB(k, v == "true")
            v.toIntOrNull() != null && !k.endsWith("Pick") -> putI(k, v.toInt())
            v.toFloatOrNull() != null && v.contains('.') -> putF(k, v.toFloat())
            else -> putS(k, v.replace("\\n", "\n"))
        }
    }

    // ---- System
    var interceptHome: Boolean get() = b("interceptHome", false); set(v) = putB("interceptHome", v)
    /** Opens Glassio when the TV starts (needs the "display over other apps" right, given over ADB). */
    var bootStart: Boolean get() = b("bootStart", AppleTvDesign.value("bootStart")); set(v) = putB("bootStart", v)
    /** Package opened when the TV starts; blank opens Glassio itself. */
    var bootApp: String get() = s("bootApp", ""); set(v) = putS("bootApp", v)
    /** Remote buttons remapped in System, one "keyCode|package" line each (see RemoteKeys). */
    var remoteKeys: List<String> get() = list("remoteKeys"); set(v) = putList("remoteKeys", v)
    /** Fire TV app buttons redirected in System, one "sourcePackage|targetPackage" line each (see AppButtons). */
    var appButtons: List<String> get() = list("appButtons"); set(v) = putList("appButtons", v)
    /** Manage from phone: minutes the page stays on after you leave that screen (0 = stops at once). */
    var phoneMinutes: Int get() = i("phoneMinutes", 30); set(v) = putI("phoneMinutes", v.coerceIn(0, 720))
    /** App language, a key of I18n.LANGS. */
    var lang: String get() = s("lang", AppleTvDesign.value("lang")); set(v) { putS("lang", v); I18n.lang = v }
    /** The first-run setup was finished (or skipped with Later). */
    var setupDone: Boolean get() = b("setupDone", false); set(v) = putB("setupDone", v)
}
