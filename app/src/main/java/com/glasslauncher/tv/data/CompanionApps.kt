package com.glasslauncher.tv.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipFile

/** Optional setup downloads. Only the Android installer can approve installing either app. */
object CompanionApps {
    data class App(val name: String, val pkg: String, val repo: String, val fireOnly: Boolean = false)
    data class Asset(val name: String, val url: String, val size: Long, val digest: String)
    val nuvio = App("Nuvio Legacy", "space.nuvio.nativelegacy", "iqui27/nuvio-native-legacy")
    val fixer = App("FireStick Fixer", "io.github.hatim.firestickfixer", "Hornet34/firestick-fixer", true)

    fun isFireTv(manufacturer: String = Build.MANUFACTURER, model: String = Build.MODEL) =
        manufacturer.equals("Amazon", true) || model.startsWith("AFT", true)

    fun installed(ctx: Context, app: App) = runCatching { ctx.packageManager.getPackageInfo(app.pkg, 0) }.isSuccess

    fun open(ctx: Context, app: App) {
        val pm = ctx.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(app.pkg) ?: pm.getLaunchIntentForPackage(app.pkg)
            ?: error("${app.name} lässt sich nicht öffnen")
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    internal fun selectAsset(app: App, assets: List<Asset>): Asset? = assets.filter {
        it.url.startsWith("https://github.com/${app.repo}/releases/download/") &&
            if (app == nuvio) it.name.startsWith("Nuvio-", true) && it.name.endsWith("-android.apk", true)
            else it.name.startsWith("firestick-fixer", true) && it.name.endsWith(".apk", true)
    }.sortedBy { if (it.name == "firestick-fixer.apk") 0 else 1 }.firstOrNull()

    internal fun acceptsRedirect(url: URL) = url.protocol == "https" &&
        url.host in setOf("github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com")

    private fun connection(url: URL): HttpURLConnection = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000; readTimeout = 30_000; instanceFollowRedirects = false
        setRequestProperty("User-Agent", "Glassio-Setup")
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) { val n = input.read(buf); if (n < 0) break; digest.update(buf, 0, n) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    internal fun verify(file: File, asset: Asset) {
        check(asset.size <= 0 || file.length() == asset.size) { "Download ist unvollständig" }
        if (asset.digest.startsWith("sha256:")) check(sha256(file).equals(asset.digest.removePrefix("sha256:"), true)) {
            "Die Download-Prüfsumme stimmt nicht"
        }
    }

    suspend fun download(ctx: Context, app: App, status: (String) -> Unit): File = withContext(Dispatchers.IO) {
        check(!app.fireOnly || isFireTv()) { "FireStick Fixer ist für Amazon Fire TV vorgesehen" }
        val api = connection(URL("https://api.github.com/repos/${app.repo}/releases/latest"))
        val release = try {
            api.setRequestProperty("Accept", "application/vnd.github+json")
            check(api.responseCode == 200) { "GitHub: HTTP ${api.responseCode}. Bitte später erneut versuchen." }
            JSONObject(api.inputStream.bufferedReader().use { it.readText() })
        } finally { api.disconnect() }
        val all = release.getJSONArray("assets")
        val asset = selectAsset(app, (0 until all.length()).map { i -> all.getJSONObject(i).let {
            Asset(it.optString("name"), it.optString("browser_download_url"), it.optLong("size"), it.optString("digest"))
        } }) ?: error("Keine passende Android-APK im aktuellen Release gefunden")
        val file = File(ctx.cacheDir, "setup-${app.pkg}.apk")
        if (file.exists() && asset.digest.startsWith("sha256:") && runCatching { verify(file, asset) }.isSuccess) return@withContext file
        val coroutine = currentCoroutineContext()
        try {
            var url = URL(asset.url)
            var conn: HttpURLConnection? = null
            try {
                for (hop in 0..5) {
                    check(acceptsRedirect(url)) { "Nicht unterstützte Download-Adresse" }
                    coroutine.ensureActive()
                    val c = connection(url); conn = c
                    val code = c.responseCode
                    if (code in listOf(301, 302, 303, 307, 308)) {
                        check(hop < 5) { "Zu viele Download-Weiterleitungen" }
                        url = URL(url, c.getHeaderField("Location") ?: error("Download-Adresse fehlt"))
                        c.disconnect(); conn = null
                    } else {
                        check(code == 200) { "Download: HTTP $code" }
                        break
                    }
                }
                val c = conn ?: error("Download nicht erreichbar")
                var done = 0L; var lastPercent = -1
                c.inputStream.use { input -> file.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        coroutine.ensureActive()
                        val n = input.read(buf); if (n < 0) break
                        done += n
                        check(done <= 200L * 1024 * 1024) { "Die APK ist zu groß" }
                        output.write(buf, 0, n)
                        val percent = if (asset.size > 0) (done * 100 / asset.size).toInt().coerceAtMost(100) else 0
                        if (percent != lastPercent) { status("${app.name}: Download $percent %"); lastPercent = percent }
                    }
                } }
                verify(file, asset)
            } finally { conn?.disconnect() }
            file
        } catch (e: Exception) { file.delete(); throw e }
    }

    /** Validate the actual APK, not just its filename, before handing it to Android. */
    suspend fun install(ctx: Context, app: App, file: File): String = withContext(Dispatchers.IO) {
        val coroutine = currentCoroutineContext()
        coroutine.ensureActive()
        val pm = ctx.packageManager
        val info = pm.getPackageArchiveInfo(file.absolutePath, 0) ?: error("Die Datei ist keine gültige APK")
        check(info.packageName == app.pkg) { "Die APK gehört nicht zu ${app.name}" }
        check((info.applicationInfo?.minSdkVersion ?: 0) <= Build.VERSION.SDK_INT) { "Diese APK benötigt eine neuere Android-Version" }
        val abis = ZipFile(file).use { zip -> zip.entries().asSequence().map { it.name }
            .filter { it.startsWith("lib/") && it.endsWith(".so") }.map { it.split('/')[1] }.toSet() }
        check(abis.isEmpty() || Build.SUPPORTED_ABIS.any { it in abis }) { "Diese APK unterstützt den Prozessor dieses Geräts nicht" }
        if (!pm.canRequestPackageInstalls()) {
            runCatching { ctx.startActivity(Intent(AndroidSettings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            return@withContext "Erlaube Installationen für Glassio in den TV-Einstellungen und wähle den Download-Button erneut."
        }
        val installer = pm.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(app.pkg)
            if (Build.VERSION.SDK_INT >= 31) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
        }
        val id = installer.createSession(params)
        try {
            installer.openSession(id).use { session ->
                session.openWrite("companion.apk", 0, file.length()).use { output ->
                    file.inputStream().use { input ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            coroutine.ensureActive()
                            val n = input.read(buf); if (n < 0) break
                            output.write(buf, 0, n)
                        }
                    }
                    session.fsync(output)
                }
                val result = Intent(ctx, CompanionInstallReceiver::class.java)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
                coroutine.ensureActive()
                session.commit(PendingIntent.getBroadcast(ctx, app.pkg.hashCode(), result, flags).intentSender)
            }
        } catch (e: Exception) { installer.abandonSession(id); throw e }
        "Bestätige die Installation von ${app.name} im Android-Installer."
    }
}

class CompanionInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirmation = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                    else intent.getParcelableExtra(Intent.EXTRA_INTENT)
                if (confirmation != null) runCatching { context.startActivity(confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            PackageInstaller.STATUS_SUCCESS -> android.widget.Toast.makeText(context, "App installiert", android.widget.Toast.LENGTH_SHORT).show()
            else -> android.widget.Toast.makeText(context, "Installation nicht abgeschlossen", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
