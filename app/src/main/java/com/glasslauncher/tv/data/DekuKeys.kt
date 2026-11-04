package com.glasslauncher.tv.data

import org.json.JSONObject

/**
 * The account of the fork's owner and the keys the build already carries. Signing in with that GitHub account —
 * in the first-run setup, or later from Settings → System — writes them into the settings, so nobody has to
 * paste them by hand. Trakt, Simkl and Stremio are per-account sign-ins and are not part of this.
 */
object DekuKeys {
    const val GITHUB = "DekuCS"

    /** What the import writes, by name. */
    val names: List<String> get() = listOf("TMDB token").filter { value(it).isNotBlank() }

    private fun value(name: String): String = when (name) {
        "TMDB token" -> com.glasslauncher.tv.BuildConfig.TMDB_READ_ACCESS_TOKEN
        else -> ""
    }

    /**
     * True = that is the owner, false = somebody else, null = GitHub couldn't be asked.
     * A personal access token is checked against api.github.com, so the answer comes from GitHub itself.
     */
    fun verify(input: String): Boolean? {
        val v = input.trim()
        if (v.isBlank()) return false
        val isToken = v.startsWith("ghp_") || v.startsWith("gho_") || v.startsWith("github_pat_")
        if (!isToken) return v.equals(GITHUB, ignoreCase = true)
        return runCatching {
            val c = java.net.URL("https://api.github.com/user").openConnection() as java.net.HttpURLConnection
            c.connectTimeout = 8000; c.readTimeout = 8000
            c.setRequestProperty("Authorization", "Bearer $v")
            c.setRequestProperty("Accept", "application/vnd.github+json")
            try {
                when (c.responseCode) {
                    in 200..299 -> JSONObject(c.inputStream.bufferedReader().use { it.readText() })
                        .optString("login").equals(GITHUB, ignoreCase = true)
                    401, 403, 404 -> false
                    else -> null
                }
            } finally { c.disconnect() }
        }.getOrNull()
    }

    /** Writes the keys into the settings; returns how many were set. */
    fun import(s: Settings): Int {
        var n = 0
        names.forEach { name ->
            when (name) {
                "TMDB token" -> { s.tmdbToken = value(name); n++ }
            }
        }
        return n
    }
}
