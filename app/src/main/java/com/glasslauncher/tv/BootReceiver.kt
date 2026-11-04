package com.glasslauncher.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Opens the start-up app when the TV starts (System → "Open an app when the TV starts"). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED && intent?.action != "android.intent.action.QUICKBOOT_POWERON") return
        val st = (ctx.applicationContext as? GlassApp)?.settings ?: return
        if (!st.bootStart) return
        val pkg = st.bootApp
        runCatching {
            val target = if (pkg.isNotBlank() && pkg != ctx.packageName) {
                val pm = ctx.packageManager
                pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg) ?: return@runCatching
            } else {
                Intent(ctx, MainActivity::class.java).setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            }
            ctx.startActivity(target.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
    }
}
