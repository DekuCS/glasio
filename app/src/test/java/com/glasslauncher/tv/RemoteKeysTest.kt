package com.glasslauncher.tv

import android.view.KeyEvent
import com.glasslauncher.tv.data.RemoteKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Remote-button mappings survive the settings file (one "keyCode|package" line each). */
class RemoteKeysTest {
    @Test fun formatAndParseRoundTrip() {
        val lines = listOf(RemoteKeys.format(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "com.example.tv"), RemoteKeys.format(KeyEvent.KEYCODE_PROG_RED, "org.other"))
        assertEquals(mapOf(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to "com.example.tv", KeyEvent.KEYCODE_PROG_RED to "org.other"), RemoteKeys.parse(lines))
    }

    @Test fun brokenLinesAreSkipped() {
        val map = RemoteKeys.parse(listOf("", "junk", "85|", "notanumber|com.example.tv", "85|com.example.tv"))
        assertEquals(mapOf(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to "com.example.tv"), map)
        assertNull(map[99])
    }

    @Test fun theLastMappingForAKeyWins() {
        assertEquals(mapOf(85 to "b"), RemoteKeys.parse(listOf("85|a", "85|b")))
    }

    @Test fun navigationAndVolumeKeysCannotBeTakenOver() {
        for (code in listOf(KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_POWER, 0, -1)) {
            assertFalse("key $code must stay usable", RemoteKeys.canBind(code))
        }
        assertTrue(RemoteKeys.canBind(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        assertTrue(RemoteKeys.canBind(KeyEvent.KEYCODE_PROG_RED))
        assertTrue(RemoteKeys.canBind(KeyEvent.KEYCODE_0))
    }

    @Test fun knownKeysHaveReadableNames() {
        assertEquals("Play/Pause", RemoteKeys.label(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        assertEquals("Live TV", RemoteKeys.label(KeyEvent.KEYCODE_TV))
        assertEquals("7", RemoteKeys.label(KeyEvent.KEYCODE_7))
    }
}
