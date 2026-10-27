package org.candyos.experience

import android.app.Activity
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast

/**
 * The Testing bench - ported from the sunset vendor/testing app (P23 ->
 * P25). Runs on Theme.DeviceDefault (== Holo via the P1 series), so the
 * widget gallery doubles as a theme-cascade test. See TASKS.md Phase 23
 * for the full test matrix, Phase 25 for the port.
 */
class TestingActivity : Activity() {

    private lateinit var nm: NotificationManager
    private var light = false
    private var immersive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        light = savedInstanceState?.getBoolean(KEY_LIGHT) ?: false
        immersive = savedInstanceState?.getBoolean(KEY_IMMERSIVE) ?: false
        setTheme(
            if (light) android.R.style.Theme_DeviceDefault_Light
            else android.R.style.Theme_DeviceDefault,
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.testing_activity)

        nm = getSystemService(NotificationManager::class.java) ?: throw IllegalStateException("no NotificationManager")
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID, getString(R.string.test_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT))

        val spinner = requireViewById<Spinner>(R.id.test_spinner)
        val adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.test_spinner_entries))
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val am = getSystemService(AudioManager::class.java)
        wire(R.id.test_notif_long) { if (ensureNotifPermission()) post(1, R.string.test_ticker_long_title, R.string.test_ticker_long_text) }
        wire(R.id.test_notif_short) { if (ensureNotifPermission()) post(2, R.string.test_ticker_short_title, R.string.test_ticker_short_text) }
        wire(R.id.test_notif_burst) {
            if (ensureNotifPermission()) {
                post(3, R.string.test_chain1_title, R.string.test_chain1_text)
                post(4, R.string.test_chain2_title, R.string.test_chain2_text)
                post(5, R.string.test_chain3_title, R.string.test_chain3_text)
            }
        }
        wire(R.id.test_notif_group) { if (ensureNotifPermission()) postGroup() }
        wire(R.id.test_notif_ongoing) { if (ensureNotifPermission()) post(8, R.string.test_ongoing_title, R.string.test_ongoing_text, ongoing = true) }
        wire(R.id.test_notif_cancel) { nm.cancelAll() }
        wire(R.id.test_dialog) {
            AlertDialog.Builder(this)
                .setTitle(R.string.test_dialog_title)
                .setMessage(R.string.test_dialog_msg)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
        wire(R.id.test_toast) { Toast.makeText(this, R.string.test_toast_msg, Toast.LENGTH_SHORT).show() }
        wire(R.id.test_second) { startActivity(Intent(this, TestSecondActivity::class.java)) }
        wire(R.id.test_volume) {
            am?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI)
        }
        wire(R.id.test_immersive) { immersive = !immersive; applyImmersive() }
        wire(R.id.test_rotate) {
            val landscape = resources.configuration.orientation ==
                Configuration.ORIENTATION_LANDSCAPE
            setRequestedOrientation(
                if (landscape) ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                else ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE)
        }
        wire(R.id.test_theme) { light = !light; recreate() }

        if (immersive) applyImmersive()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_LIGHT, light)
        outState.putBoolean(KEY_IMMERSIVE, immersive)
    }

    private fun wire(id: Int, action: () -> Unit) {
        requireViewById<View>(id).setOnClickListener { action() }
    }

    /**
     * targetSdk 34: without POST_NOTIFICATIONS the NMS ranks the app
     * IMPORTANCE_NONE - no shade entries, and the SystemUI listener (the
     * 4.4 ticker) never sees the posts. Guard every posting path. (The
     * permission ships pre-granted via the default-permissions XML; the
     * runtime request is the revocation fallback.)
     */
    private fun ensureNotifPermission(): Boolean {
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED) {
            return true
        }
        Toast.makeText(this, R.string.test_perm_needed, Toast.LENGTH_LONG).show()
        requestPermissions(
            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
            REQ_POST_NOTIFICATIONS)
        return false
    }

    private fun post(id: Int, titleRes: Int, textRes: Int, ongoing: Boolean = false) {
        val text = getString(textRes)
        nm.notify(
            id,
            Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_testing)
                .setContentTitle(getString(titleRes))
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setOngoing(ongoing)
                .setAutoCancel(!ongoing)
                .build())
    }

    /**
     * Group test: the child must tick, the summary must not (the ticker
     * filters FLAG_GROUP_SUMMARY - see Ticker.add).
     */
    private fun postGroup() {
        val group = "org.candyos.experience.TEST_GROUP"
        val child = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_testing)
            .setContentTitle(getString(R.string.test_group_child_title))
            .setContentText(getString(R.string.test_group_child_text))
            .setGroup(group)
            .setAutoCancel(true)
            .build()
        val summary = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_testing)
            .setContentTitle(getString(R.string.test_group_summary_title))
            .setContentText(getString(R.string.test_group_summary_text))
            .setGroup(group)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        nm.notify(6, child)
        nm.notify(7, summary)
    }

    private fun applyImmersive() {
        val decor = window.decorView
        if (immersive) {
            decor.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        } else {
            decor.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    companion object {
        private const val CHANNEL_ID = "ticker_tests"
        private const val KEY_LIGHT = "light"
        private const val KEY_IMMERSIVE = "immersive"
        private const val REQ_POST_NOTIFICATIONS = 1
    }
}