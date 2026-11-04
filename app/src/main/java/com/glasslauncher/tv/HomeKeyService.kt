package com.glasslauncher.tv

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Home button → Glassio.
 *
 * Google TV sends Home to its own launcher no matter which app is the default home. This service
 * (switched on by the user under Settings > Accessibility) receives the Home key *before* the system
 * does and consumes it, so the stock launcher never appears and there is no flash.
 * The old window-change reaction is kept only as a fallback for devices that don't deliver key events.
 */
class HomeKeyService : AccessibilityService() {
    private var lastLaunch = 0L
    private var homeDownAt = 0L
    private var longDone = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var camera: CameraOverlay? = null
    /** Hold Home: what Settings → System → "Hold Home" says. */
    private val longPress = Runnable {
        longDone = true
        when ((application as? GlassApp)?.settings?.homeLong) {
            "recents" -> runCatching { startActivity(Intent(this, RecentsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            "library" -> openGlass("open_library")
            "menu" -> openGlass("open_menu")
            "camera" -> toggleCamera()
        }
    }

    private fun openGlass(extra: String) = runCatching {
        startActivity(Intent(this, MainActivity::class.java).putExtra(extra, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP))
    }

    private fun toggleCamera() {
        camera?.let { if (it.showing) { it.close(); camera = null; return } }
        val cams = (application as GlassApp).settings.cameras.mapNotNull { com.glasslauncher.tv.ui.Cam.parse(it) }
        if (cams.isEmpty()) { android.widget.Toast.makeText(this, "Add a camera first: Glassio settings → Connections → Cameras", android.widget.Toast.LENGTH_LONG).show(); return }
        camera = CameraOverlay(this, cams).also { it.show() }
    }

    private fun enabled(): Boolean = (application as? GlassApp)?.settings?.interceptHome == true

    private var lastRemap = 0L
    /** A remote app button opens its own app; when it is mapped, the chosen app opens instead. */
    private var lastRedirect = 0L
    private var redirectedFrom = ""
    private var redirectedAt = 0L

    /** Remote buttons mapped in System: the key opens its app, in every app. */
    private fun remapPackage(keyCode: Int): String? {
        val st = (application as? GlassApp)?.settings ?: return null
        return com.glasslauncher.tv.data.RemoteKeys.parse(st.remoteKeys)[keyCode]
    }

    private fun openApp(pkg: String) = runCatching {
        val pm = packageManager
        val i = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg) ?: return@runCatching
        startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Fire TV app buttons open their own app; when it is mapped in System, the chosen app opens instead. */
    private fun redirectAppButton(pkg: String) {
        val st = (application as? GlassApp)?.settings ?: return
        val target = com.glasslauncher.tv.data.AppButtons.parse(st.appButtons)[pkg] ?: return
        if (target == pkg) return
        val now = System.currentTimeMillis()
        // Back out of the chosen app lands in the source again; only a fresh button press redirects.
        // shortcut: this 15 s window cannot tell Back from a second press, upgrade with the window history if it annoys.
        if (pkg == redirectedFrom && now - redirectedAt < 15_000) return
        if (now - lastRedirect < 1500) return
        lastRedirect = now
        redirectedFrom = pkg
        redirectedAt = now
        openApp(target)
    }

    private fun showGlass() {
        val now = System.currentTimeMillis()
        if (now - lastLaunch < 1000) return
        lastLaunch = now
        startActivity(
            Intent(this, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
    }

    /** Home key events reach this service on this TV: the window-change fallback is then never needed. */
    private val sp by lazy { getSharedPreferences("glass_home_key", MODE_PRIVATE) }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // The camera in the corner: Back closes it (holding Home again too); every other key goes to the app as usual.
        camera?.takeIf { it.showing }?.let { c ->
            if (event.keyCode == KeyEvent.KEYCODE_BACK) { if (event.action == KeyEvent.ACTION_UP) { c.close(); camera = null }; return true }
        }
        // A mapped remote button opens its app and never reaches the app in front.
        remapPackage(event.keyCode)?.let { pkg ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                val now = System.currentTimeMillis()
                if (event.repeatCount == 0 && now - lastRemap > 700) { lastRemap = now; openApp(pkg) }
            }
            return true
        }
        if (event.keyCode != KeyEvent.KEYCODE_HOME || !enabled()) return false
        if (!sp.getBoolean("keys", false)) sp.edit().putBoolean("keys", true).apply()
        val recents = (application as? GlassApp)?.settings?.homeLong.let { it != null && it != "off" }
        when (event.action) {
            KeyEvent.ACTION_DOWN -> if (event.repeatCount == 0) {
                homeDownAt = System.currentTimeMillis(); longDone = false
                if (recents) handler.postDelayed(longPress, 600)
            } else if (recents && !longDone && (event.flags and KeyEvent.FLAG_LONG_PRESS != 0 || System.currentTimeMillis() - homeDownAt > 500)) {
                handler.removeCallbacks(longPress); longPress.run()
            }
            KeyEvent.ACTION_UP -> { handler.removeCallbacks(longPress); if (!longDone) showGlass(); longDone = false }
        }
        return true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        redirectAppButton(pkg)
        if (!enabled()) return
        val amazonHome = pkg == "com.amazon.tv.launcher"
        if (!amazonHome && pkg != "com.google.android.apps.tv.launcherx" && pkg != "com.google.android.tvlauncher") return
        // The fallback brought Glassio to the front whenever the Google TV launcher showed any window of its own
        // (for example when Wi-Fi dropped and came back), even while you were watching. Where the Home key
        // reaches this service it is never needed; elsewhere only the launcher's main screen counts.
        // Fire OS reserves Home even when a key was delivered earlier. Keep its window fallback active.
        if (!amazonHome && sp.getBoolean("keys", false)) return
        val cls = event.className?.toString().orEmpty()
        if (!cls.contains("MainActivity") && !cls.contains("HomeActivity") && !cls.contains("LauncherActivity")) return
        // Only reached when the Home key was not filtered above (e.g. Home from the Google TV remote app).
        if (System.currentTimeMillis() - homeDownAt < 1500) return
        showGlass()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Ask for key events explicitly; some TV builds ignore the flag from the XML alone.
        serviceInfo = serviceInfo.apply {
            flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
    }

    override fun onInterrupt() {}
}
