package com.glasslauncher.tv.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Process
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

/**
 * The desktop app's way in, over ADB
 * (`content call --uri content://com.glasslauncher.tv.config --method get`):
 *
 *   get                        every setting Glassio holds, as JSON
 *   set64 <base64 JSON>        merge settings (base64, so nothing needs shell quoting)
 *   apps                       the apps the dock can hold, with the names the TV uses
 *
 * Only the shell user (ADB) and root may call it, so no other app on the TV can restyle Glassio.
 * Values keep the type they arrive with: true/false, numbers, text. A JSON array of strings becomes
 * the newline-separated text Glassio stores its lists in (dock, add-ons, folders).
 */
class ConfigProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        val uid = Binder.getCallingUid()
        if (uid != SHELL_UID && uid != ROOT_UID && uid != Process.myUid()) {
            throw SecurityException("only ADB may change Glassio")
        }
        val ctx = requireNotNull(context)
        val out = Bundle()
        when (method) {
            "apps" -> {
                out.putString("apps", listApps(ctx).toString())
                return out
            }
            "get" -> Unit
            "set64" -> {
                val json = runCatching { JSONObject(String(Base64.decode(arg ?: "", Base64.DEFAULT), Charsets.UTF_8)) }
                    .getOrElse { throw IllegalArgumentException("arg must be a base64 JSON object") }
                apply(ctx, json)
            }
            else -> throw IllegalArgumentException("unknown method: $method")
        }
        out.putString("settings", prefs(ctx).toString())
        return out
    }

    private fun prefs(ctx: android.content.Context): JSONObject {
        val sp = ctx.getSharedPreferences("glass", android.content.Context.MODE_PRIVATE)
        val o = JSONObject()
        sp.all.forEach { (k, v) -> if (k.length <= 120) when (v) {
            is Boolean, is Int, is Float, is String -> o.put(k, v)
            is Long -> o.put(k, v.toDouble())
            is Set<*> -> o.put(k, v.joinToString("\n"))
            else -> Unit
        } }
        return o
    }

    /** Writes every key it understands and leaves the rest of Glassio's settings alone. */
    private fun apply(ctx: android.content.Context, json: JSONObject) {
        val sp = ctx.getSharedPreferences("glass", android.content.Context.MODE_PRIVATE)
        val editor = sp.edit()
        var n = 0
        for (key in json.keys()) {
            if (n++ >= MAX_KEYS) break
            if (key.length !in 1..120 || !KEY.matches(key)) continue
            when (val v = json.opt(key)) {
                is Boolean -> editor.putBoolean(key, v)
                is Number -> {
                    val d = v.toDouble()
                    if (!d.isFinite()) continue
                    // Whole numbers stay Int: Glassio reads most of them with getInt, which throws on a Float.
                    if (d == Math.floor(d) && d >= Int.MIN_VALUE && d <= Int.MAX_VALUE) editor.putInt(key, d.toInt())
                    else editor.putFloat(key, d.toFloat())
                }
                is String -> editor.putString(key, v.take(MAX_TEXT).replace("\u0000", ""))
                is JSONArray -> editor.putString(key, (0 until minOf(v.length(), MAX_LIST)).mapNotNull {
                    (v.opt(it) as? String)?.trim()?.takeIf { s -> s.isNotEmpty() && s.length <= 500 }
                }.joinToString("\n").take(MAX_TEXT))
                else -> Unit
            }
        }
        check(editor.commit()) { "could not save the settings" }
        // The provider runs inside Glassio's own process, so the launcher can redraw straight away.
        (ctx.applicationContext as? com.glasslauncher.tv.GlassApp)?.settings?.refresh()
    }

    /** What the dock can hold: the TV apps first, then the phone-style ones, each named as on the TV. */
    private fun listApps(ctx: android.content.Context): JSONArray {
        val pm = ctx.packageManager
        val found = LinkedHashMap<String, JSONObject>()
        for (category in CATEGORIES) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (info in pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)) {
                val pkg = info.activityInfo.applicationInfo.packageName
                if (pkg == ctx.packageName) continue
                val tv = category == Intent.CATEGORY_LEANBACK_LAUNCHER
                val old = found[pkg]
                if (old != null && !tv) continue
                found[pkg] = JSONObject()
                    .put("pkg", pkg)
                    .put("label", info.loadLabel(pm).toString().replace(Regex("[\\p{Cntrl}]"), "").trim().take(60).ifEmpty { pkg })
                    .put("tv", tv)
            }
        }
        val list = found.values.sortedBy { it.optString("label").lowercase() }
        return JSONArray(list)
    }

    override fun query(uri: Uri, p: Array<String>?, s: String?, a: Array<String>?, o: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<String>?): Int = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<String>?): Int = 0

    private companion object {
        const val SHELL_UID = 2000
        const val ROOT_UID = 0
        const val MAX_KEYS = 2000
        const val MAX_TEXT = 20000
        const val MAX_LIST = 500
        val KEY = Regex("^[A-Za-z0-9_.]{1,120}$")
        val CATEGORIES = listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)
    }
}
