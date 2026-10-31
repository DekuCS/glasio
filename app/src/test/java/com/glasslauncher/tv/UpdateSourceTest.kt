package com.glasslauncher.tv

import com.glasslauncher.tv.data.Updater
import org.junit.Assert.*
import org.junit.Test

class UpdateSourceTest {
    @Test fun settingsTextIsValidUtf8() {
        java.io.File("src/main/java/com/glasslauncher/tv/ui/SettingsScreen.kt").readBytes()
            .decodeToString(throwOnInvalidSequence = true)
    }

    @Test fun releaseDownloadsBelongToTheOwnersFork() {
        assertEquals("DekuCS/glassio", Updater.REPO)
        assertEquals("https://github.com/DekuCS/glassio", Updater.REPO_URL)
        assertTrue(Updater.isOwnApk("https://github.com/DekuCS/glassio/releases/download/v2.1.0/Glassio-Deku-2.1.0.apk"))
        assertFalse(Updater.isOwnApk("https://github.com/mohammadmrh12/glassio/releases/download/v2.1.0/Glassio.apk"))
        assertFalse(Updater.isOwnApk("https://github.com/DekuCS/glassio-other/releases/download/v2.1.0/Glassio.apk"))
        assertFalse(Updater.isOwnApk("http://github.com/DekuCS/glassio/releases/download/v2.1.0/Glassio.apk"))
        assertFalse(Updater.isOwnApk("https://example.org/Glassio.apk"))
    }
}
