package org.candyos.experience.updates

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Bundle
import android.util.Log
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The update engine: poll GitHub for every tracked bundled app, and when a
 * release is newer than the installed build, download the apk asset
 * (sha256-verified against the API digest) and commit it through
 * PackageInstaller.
 *
 * CandyOS Experience is a privileged system_ext app on the platform
 * certificate with INSTALL_PACKAGES in its privapp whitelist, so the
 * install session commits silently in the background - which is the whole
 * point of the daily check: bundled apps update themselves without user
 * babysitting. A notification is posted per result (the same POST_NOTIFICATIONS
 * grant + guard the Testing bench uses).
 *
 * One engine instance at a time (see [isRunning]); the JobService and the
 * Updates screen share this object.
 */
object UpdateEngine {

    private const val TAG = "CandyUpdates"
    private const val CHANNEL_ID = "candy_updates"
    private const val SESSION_ACTION = "org.candyos.experience.UPDATE_SESSION"
    private const val NOTIF_ID = 100

    sealed class Result {
        abstract val app: TrackedApp

        data class Installed(override val app: TrackedApp, val tag: String) : Result()
        data class UpToDate(override val app: TrackedApp) : Result()
        data class Failed(override val app: TrackedApp, val reason: String) : Result()
    }

    @Volatile
    var isRunning = false
        private set

    private var executor: ExecutorService = Executors.newSingleThreadExecutor()

    /** Run one full check/install pass for all tracked apps, off the main thread. */
    fun checkAsync(context: Context, onDone: (List<Result>) -> Unit) {
        val app = context.applicationContext
        if (isRunning) {
            onDone(emptyList())
            return
        }
        isRunning = true
        executor.execute {
            val results = mutableListOf<Result>()
            for (tracked in TrackedApps.TRACKED) {
                try {
                    results += checkOne(app, tracked)
                } catch (e: Exception) {
                    Log.w(TAG, "update check failed for ${tracked.packageName}: $e")
                    results += Result.Failed(tracked, e.message ?: e.javaClass.simpleName)
                }
            }
            TrackedApps.prefs(app).edit()
                .putLong("last_check", System.currentTimeMillis())
                .apply()
            isRunning = false
            notifyResults(app, results)
            onDone(results)
        }
    }

    private fun checkOne(context: Context, tracked: TrackedApp): Result {
        val prefs = TrackedApps.prefs(context)
        val etagKey = "etag.${tracked.repoOwner}/${tracked.repoName}"
        val etag = prefs.getString(etagKey, null)

        val state = mutableMapOf<String, String>()
        val (release, unchanged) =
            GithubClient.latest(tracked.repoOwner, tracked.repoName, etag, state)
        state["etag"]?.let { prefs.edit().putString(etagKey, it).apply() }

        // 304: the release we already know about is still the latest.
        if (unchanged) {
            val known = prefs.getString("tag.${tracked.packageName}", null)
            return if (known != null) Result.UpToDate(tracked) else Result.Failed(
                tracked, "etag hit with no cached release",
            )
        }
        if (release == null) {
            return Result.UpToDate(tracked) // no release / no apk asset: nothing to do
        }
        prefs.edit()
            .putString("tag.${tracked.packageName}", release.tag)
            .putLong("seen.${tracked.packageName}", System.currentTimeMillis())
            .apply()

        val installed = TrackedApps.installedInfo(context, tracked.packageName)
            ?: return Result.Failed(tracked, "not installed")
        if (!isTagNewer(release.tag, installed.versionName ?: "")) {
            return Result.UpToDate(tracked)
        }

        install(context, tracked, release)
        return Result.Installed(tracked, release.tag)
    }

    /** Download the asset to the app cache dir, then commit an install session. */
    private fun install(context: Context, tracked: TrackedApp, release: GithubClient.Release) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val tmp = File(dir, "${tracked.packageName}.apk")
        val sha = tmp.outputStream().use {
            GithubClient.download(release.assetUrl, release.assetSize, it)
        }
        try {
            if (release.sha256 != null && !release.sha256.equals(sha, ignoreCase = true)) {
                throw SecurityException("sha256 mismatch for ${release.assetName}")
            }
            commitSession(context, tmp)
        } finally {
            tmp.delete()
        }
    }

    private fun commitSession(context: Context, apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(apk.name.removeSuffix(".apk"))
        // Platform certificate + INSTALL_PACKAGES: no user confirmation.
        params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        val sessionId = installer.createSession(params)
        val session = installer.openSession(sessionId)
        try {
            session.openWrite("apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
            }
            val statusIntent = Intent(SESSION_ACTION).setPackage(context.packageName)
            val pi = PendingIntent.getBroadcast(
                context, sessionId, statusIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            session.commit(pi.intentSender)
        } finally {
            session.close()
        }
    }

    private fun notifyResults(context: Context, results: List<Result>) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, context.getString(org.candyos.experience.R.string.updates_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT))
        if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val installed = results.filterIsInstance<Result.Installed>()
        val failed = results.filterIsInstance<Result.Failed>()
        if (installed.isEmpty() && failed.isEmpty()) return
        val title = when {
            installed.isNotEmpty() -> context.getString(
                org.candyos.experience.R.string.updates_notif_installed_title,
                installed.size)
            else -> context.getString(org.candyos.experience.R.string.updates_notif_failed_title)
        }
        val text = (installed.map { "${it.app.packageName} → ${it.tag}" } +
            failed.map { "${it.app.packageName}: ${it.reason}" })
            .joinToString("\n")
        nm.notify(
            NOTIF_ID,
            Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(org.candyos.experience.R.drawable.ic_stat_testing)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .build())
    }
}