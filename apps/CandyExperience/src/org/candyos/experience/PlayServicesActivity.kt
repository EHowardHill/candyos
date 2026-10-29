package org.candyos.experience

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView

/**
 * "Google services" — the Experience menu screen where the microG +
 * Aurora Store preloads are turned on and off at any time, mirroring
 * what the OTE wizard's play step decided on first boot. Toggles apply
 * immediately (enabled-state flips): off is instant, and a freshly
 * enabled microG wants a reboot for a clean GCM start (play_hint says
 * so).
 */
class PlayServicesActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.play_activity)

        val rows = requireViewById<android.widget.LinearLayout>(R.id.play_rows)
        requireViewById<TextView>(R.id.play_status).setText(
            if (PlayConfig.ALL.any { present(it) }) R.string.play_body else R.string.play_missing)

        PlayConfig.addRows(this, rows) { key, checked ->
            val packages = if (key == "microg") {
                listOf(PlayConfig.GMS, PlayConfig.VENDING)
            } else {
                listOf(PlayConfig.AURORA)
            }
            val state = if (checked) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
            }
            for (pkg in packages) {
                packageManager.setApplicationEnabledSetting(pkg, state, 0)
            }
        }
    }

    private fun present(pkg: String): Boolean =
        try {
            packageManager.getApplicationInfo(pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
}
