package com.glasslauncher.tv

import com.glasslauncher.tv.data.AppButtons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** App-button redirects survive the settings file (one "source|target" line each). */
class AppButtonsTest {
    @Test fun formatAndParseRoundTrip() {
        val lines = listOf(AppButtons.format("com.netflix.ninja", "com.example.tv"), AppButtons.format("com.amazon.firebat", "org.other"))
        assertEquals(mapOf("com.netflix.ninja" to "com.example.tv", "com.amazon.firebat" to "org.other"), AppButtons.parse(lines))
    }

    @Test fun brokenLinesAreSkipped() {
        assertTrue(AppButtons.parse(listOf("", "junk", "com.netflix.ninja|", "|com.example.tv")).isEmpty())
    }

    @Test fun theLastMappingForTheSameSourceWins() {
        assertEquals(mapOf("com.netflix.ninja" to "b"), AppButtons.parse(listOf("com.netflix.ninja|a", "com.netflix.ninja|b")))
    }
}
