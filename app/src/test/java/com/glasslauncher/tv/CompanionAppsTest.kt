package com.glasslauncher.tv

import com.glasslauncher.tv.data.CompanionApps
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.net.URL
import java.security.MessageDigest

class CompanionAppsTest {
    private fun asset(repo: String, name: String) = CompanionApps.Asset(name,
        "https://github.com/$repo/releases/download/v1/$name", 12, "")

    @Test fun nuvioSelectsUniversalFullAndroidApkInsteadOfLiteOrOtherPlatforms() {
        val app = CompanionApps.nuvio
        val candidates = listOf("libnuvio.so", "Nuvio-2.0.8-android-lite-armv7.apk", "Nuvio-2.0.8-android-lite-arm64.apk",
            "Nuvio-2.0.8-NuvioTpk.tpk", "NuvioTV.wgt", "Nuvio-2.0.8-android.apk").map { asset(app.repo, it) }
        assertEquals(candidates.last(), CompanionApps.selectAsset(app, candidates))
        assertNull(CompanionApps.selectAsset(app, candidates.dropLast(1)))
    }

    @Test fun fixerPrefersThePublishersStableAssetName() {
        val app = CompanionApps.fixer
        val candidates = listOf(asset(app.repo, "firestick-fixer-3.1.0.apk"), asset(app.repo, "firestick-fixer.apk"))
        assertEquals(candidates.last(), CompanionApps.selectAsset(app, candidates))
        assertEquals(candidates.first(), CompanionApps.selectAsset(app, candidates.take(1)))
    }

    @Test fun wrongRepositoriesAndInsecureOrLookalikeDownloadsAreRejected() {
        val app = CompanionApps.fixer
        val good = asset(app.repo, "firestick-fixer.apk")
        val invalid = listOf(good.copy(url = good.url.replace("https:", "http:")),
            good.copy(url = good.url.replace("github.com", "github.com.example.org")),
            asset("someone/other", good.name))
        assertNull(CompanionApps.selectAsset(app, invalid))
        assertTrue(CompanionApps.acceptsRedirect(URL("https://release-assets.githubusercontent.com/asset")))
        assertFalse(CompanionApps.acceptsRedirect(URL("https://example.org/asset.apk")))
        assertFalse(CompanionApps.acceptsRedirect(URL("http://github.com/asset.apk")))
    }

    @Test fun fireOnlySetupButtonDoesNotAppearOnGoogleOrAndroidTv() {
        assertTrue(CompanionApps.isFireTv("Amazon", "AFTKRT"))
        assertTrue(CompanionApps.isFireTv("Other", "AFTKRT"))
        assertFalse(CompanionApps.isFireTv("Google", "Google TV Streamer"))
        assertFalse(CompanionApps.isFireTv("Sony", "BRAVIA"))
    }

    @Test fun incompleteOrModifiedDownloadsCannotBeInstalled() {
        val file = File.createTempFile("glassio-download", ".apk")
        try {
            val bytes = "verified-apk-bytes".toByteArray(); file.writeBytes(bytes)
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            val good = asset(CompanionApps.fixer.repo, "firestick-fixer.apk").copy(size = file.length(), digest = "sha256:$hash")
            CompanionApps.verify(file, good)
            assertThrows(IllegalStateException::class.java) { CompanionApps.verify(file, good.copy(size = file.length() + 1)) }
            assertThrows(IllegalStateException::class.java) { CompanionApps.verify(file, good.copy(digest = "sha256:" + "0".repeat(64))) }
        } finally { file.delete() }
    }
}
