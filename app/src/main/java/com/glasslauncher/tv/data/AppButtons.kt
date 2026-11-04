package com.glasslauncher.tv.data

/**
 * Settings → System → "App buttons": a remote button opens its own app, and Glasio opens a different
 * app instead. Stored as one "sourcePackage|targetPackage" line per mapping.
 */
object AppButtons {
    fun format(source: String, target: String) = "$source|$target"

    fun parse(lines: List<String>): Map<String, String> = buildMap {
        for (line in lines) {
            val source = line.substringBefore('|', "")
            val target = line.substringAfter('|', "")
            if (source.isNotBlank() && target.isNotBlank()) put(source, target)
        }
    }
}
