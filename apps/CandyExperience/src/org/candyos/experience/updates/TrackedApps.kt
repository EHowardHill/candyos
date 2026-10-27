package org.candyos.experience.updates

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager

/**
 * The registry of CandyOS bundled apps that track GitHub releases.
 *
 * Every app we bundle as a prebuilt and that also publishes on GitHub gets
 * one entry here: packageName (the APK's manifest package, used to read the
 * installed version) and the owner/repo pair (used to poll
 * .../releases/latest). When a new bundled APK is added to the tree, add
 * its entry below and the updater picks it up on the next daily check.
 *
 * State (etag, last seen release, last check time) lives in a private
 * SharedPreferences file so the JobService and the Updates screen share it.
 */
data class TrackedApp(
    val packageName: String,
    val repoOwner: String,
    val repoName: String,
    /** Optional label override; defaults to the installed app's label. */
    val label: String? = null,
)

object TrackedApps {

    /**
     * The bundled-in APKs that publish releases on GitHub. Empty until the
     * first such app lands; adding one entry here is the whole wiring:
     *   TrackedApp("org.example.app", "Example", "example-android"),
     * (see docs/updater.md for the release-tag conventions).
     */
    val TRACKED: List<TrackedApp> = listOf(
        // TrackedApp("org.example.app", "Example", "example-android"),
    )

    private const val PREFS = "candy_updates"

    fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Installed versionName for a package, or null if not installed. */
    fun installedInfo(context: Context, packageName: String): PackageInfo? =
        try {
            context.packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
}

/**
 * Semver-ish comparison of a release tag against the installed versionName:
 * tags like "v1.2.3" and "1.2.3" both normalize, dot-separated numeric
 * segments compare numerically, and a trailing non-numeric segment
 * ("1.2.3-beta1") compares lexically. Returns true when [tag] is newer.
 */
fun isTagNewer(tag: String, installedVersion: String): Boolean {
    val t = tag.trim().removePrefix("v").removePrefix("V")
    if (t.isBlank()) return false
    val a = t.split('.')
    val b = installedVersion.split('.')
    for (i in 0 until maxOf(a.size, b.size)) {
        val sa = a.getOrNull(i) ?: ""
        val sb = b.getOrNull(i) ?: ""
        val na = sa.toIntOrNull()
        val nb = sb.toIntOrNull()
        val cmp = when {
            na != null && nb != null -> na.compareTo(nb)
            na != null -> 1
            nb != null -> -1
            else -> sa.compareTo(sb)
        }
        if (cmp != 0) return cmp > 0
    }
    return false
}