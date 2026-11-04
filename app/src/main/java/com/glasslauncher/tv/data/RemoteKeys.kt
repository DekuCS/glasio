package com.glasslauncher.tv.data

import android.view.KeyEvent

/**
 * Remote-button shortcuts (Settings → System → Remote buttons): one button opens one app.
 * Stored in Settings as one "keyCode|package" line per mapping.
 */
object RemoteKeys {
    private val labels: Map<Int, String> = mapOf(
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to "Play/Pause",
        KeyEvent.KEYCODE_MEDIA_PLAY to "Play",
        KeyEvent.KEYCODE_MEDIA_PAUSE to "Pause",
        KeyEvent.KEYCODE_MEDIA_NEXT to "Next",
        KeyEvent.KEYCODE_MEDIA_PREVIOUS to "Previous",
        KeyEvent.KEYCODE_MEDIA_REWIND to "Rewind",
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD to "Fast-forward",
        KeyEvent.KEYCODE_MEDIA_STOP to "Stop",
        KeyEvent.KEYCODE_PROG_RED to "Red",
        KeyEvent.KEYCODE_PROG_GREEN to "Green",
        KeyEvent.KEYCODE_PROG_YELLOW to "Yellow",
        KeyEvent.KEYCODE_PROG_BLUE to "Blue",
        KeyEvent.KEYCODE_GUIDE to "Guide",
        KeyEvent.KEYCODE_TV to "Live TV",
        KeyEvent.KEYCODE_INFO to "Info",
        KeyEvent.KEYCODE_CAPTIONS to "Subtitles",
        KeyEvent.KEYCODE_SETTINGS to "Settings",
        KeyEvent.KEYCODE_BOOKMARK to "Bookmark",
        KeyEvent.KEYCODE_CHANNEL_UP to "Channel +",
        KeyEvent.KEYCODE_CHANNEL_DOWN to "Channel -",
    ) + (0..9).associate { (KeyEvent.KEYCODE_0 + it) to "$it" }

    /** Keys a mapping may never take over: the remote would stop working. */
    private val blocked = setOf(
        KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_MENU,
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
        KeyEvent.KEYCODE_POWER, KeyEvent.KEYCODE_SLEEP, KeyEvent.KEYCODE_WAKEUP,
        KeyEvent.KEYCODE_SOFT_SLEEP, KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_ESCAPE,
        KeyEvent.KEYCODE_NOTIFICATION, KeyEvent.KEYCODE_ASSIST, KeyEvent.KEYCODE_VOICE_ASSIST,
        KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_CAMERA,
    )

    fun canBind(keyCode: Int): Boolean = keyCode > 0 && keyCode !in blocked

    fun label(keyCode: Int): String = labels[keyCode]
        ?: KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_").replace('_', ' ').lowercase()
            .replaceFirstChar { it.uppercase() }

    fun format(keyCode: Int, pkg: String) = "$keyCode|$pkg"

    /** keyCode → package, skipping unreadable lines instead of dropping every mapping. */
    fun parse(lines: List<String>): Map<Int, String> = buildMap {
        for (line in lines) {
            val code = line.substringBefore('|').toIntOrNull() ?: continue
            val pkg = line.substringAfter('|', "")
            if (pkg.isNotBlank()) put(code, pkg)
        }
    }
}
