package org.candyos.experience.updates

import android.util.Log
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal GitHub Releases client on the framework's HttpURLConnection +
 * org.json — the Experience app carries no androidx/OkHttp dependency
 * (see Android.bp), so the poll is plain REST against api.github.com.
 *
 * Only the /releases/latest endpoint is used: one release, one apk asset,
 * no pagination. An ETag is cached per repo so an unchanged release
 * answers 304 and costs the daily check almost nothing.
 */
object GithubClient {

    private const val TAG = "CandyUpdates"
    private const val API = "https://api.github.com"
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 15_000
    private const val USER_AGENT = "CandyOS-Experience"

    data class Release(
        val tag: String,
        val assetName: String,
        val assetUrl: String,
        val assetSize: Long,
        /** "sha256:<hex>" from the API digest field, when present. */
        val sha256: String?,
    )

    /**
     * Fetch the latest release of owner/repo. Returns null when the repo
     * has no releases (404) or the release ships no .apk asset matching
     * [assetFilter]; the ETag round-trip (304 => null with
     * [unchanged]=true) is surfaced through [state].
     */
    fun latest(
        owner: String,
        repo: String,
        etag: String?,
        state: MutableMap<String, String>,
        assetFilter: String? = null,
    ): Pair<Release?, Boolean> {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL("$API/repos/$owner/$repo/releases/latest")
                .openConnection() as HttpURLConnection)
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", USER_AGENT)
            etag?.let { conn.setRequestProperty("If-None-Match", it) }

            when (conn.responseCode) {
                HttpURLConnection.HTTP_NOT_MODIFIED -> {
                    conn.disconnect()
                    return null to true
                }
                HttpURLConnection.HTTP_NOT_FOUND -> null to false
                HttpURLConnection.HTTP_OK -> {
                    val etagHeader = conn.getHeaderField("ETag")
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    val parsed = parseRelease(body, assetFilter)
                    if (etagHeader != null) state["etag"] = etagHeader
                    parsed
                }
                else -> {
                    Log.w(TAG, "GitHub poll $owner/$repo failed: HTTP ${conn.responseCode}")
                    null to false
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "GitHub poll $owner/$repo failed: $e")
            null to false
        } finally {
            conn?.disconnect()
        }
    }

    /** Download [url] to a caller-managed stream, returning the sha256 hex. */
    fun download(url: String, size: Long, out: java.io.OutputStream): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        var conn: HttpURLConnection? = null
        try {
            conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = 60_000
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.instanceFollowRedirects = true
            check(conn.responseCode == HttpURLConnection.HTTP_OK) {
                "download failed: HTTP ${conn.responseCode}"
            }
            val expected = size
            var total = 0L
            conn.inputStream.use { input ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    total += n
                    check(total <= expected) { "download larger than declared size" }
                    digest.update(buf, 0, n)
                    out.write(buf, 0, n)
                }
            }
            check(total == expected) { "truncated download: $total/$expected" }
        } finally {
            conn?.disconnect()
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun parseRelease(body: String, assetFilter: String?): Pair<Release?, Boolean> {
        return try {
            val json = JSONObject(body)
            val tag = json.optString("tag_name").ifBlank { return null to false }
            val assets = json.optJSONArray("assets") ?: return null to false
            val filter = assetFilter?.toRegex()
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.optString("name")
                if (!name.endsWith(".apk", ignoreCase = true)) continue
                if (filter != null && !filter.matches(name)) continue
                val sha = a.optString("digest").takeIf {
                    it.startsWith("sha256:", ignoreCase = true)
                }?.substring(7)?.lowercase()
                return Release(
                    tag = tag,
                    assetName = name,
                    assetUrl = a.optString("browser_download_url"),
                    assetSize = a.optLong("size"),
                    sha256 = sha,
                ) to false
            }
            null to false
        } catch (e: org.json.JSONException) {
            Log.w(TAG, "release parse failed: $e")
            null to false
        }
    }
}