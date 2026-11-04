package com.glasslauncher.tv.data

/** Stable roles for the compact dock; installed apps remain independently available below it. */
object DockLayout {
    const val SETTINGS = "glassio.settings"
    const val YOUTUBE = "com.google.android.youtube.tv"
    const val SMARTTUBE = "io.github.ivanosh.smarttubematerial"
    val defaults = listOf("space.nuvio.nativelegacy", SMARTTUBE, "com.spotify.tv.android", SETTINGS)
    private val youtubePackages = listOf(YOUTUBE, "com.amazon.firetv.youtube")

    fun resolve(selected: List<String>, installed: Set<String>, hidden: Set<String>): List<String> =
        selected.mapNotNull { key ->
            when {
                key == SETTINGS -> key
                key in installed -> key
                key in youtubePackages -> youtubePackages.firstOrNull { it in installed }
                else -> null
            }
        }.distinct().filter { it !in hidden }

    fun isNuvio(source: String) = source == "Nuvio" || source == "Nuvio Legacy"
}
