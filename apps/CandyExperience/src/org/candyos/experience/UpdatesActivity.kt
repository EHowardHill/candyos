package org.candyos.experience

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.candyos.experience.updates.TrackedApps
import org.candyos.experience.updates.UpdateEngine
import org.candyos.experience.updates.UpdatesScheduler
import java.text.DateFormat
import java.util.Date

/**
 * The Updates screen - the user-visible side of the release updater. The
 * daily JobService pass keeps the system current on its own; this screen
 * exists to show what is tracked and what happened, and to force a check
 * on demand (which, unlike the automatic pass, will run on any network).
 * Same plain-Holo list style as the rest of the app.
 */
class UpdatesActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var rows: LinearLayout
    private lateinit var checkButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.updates_activity)
        UpdatesScheduler.ensureScheduled(this)

        status = requireViewById(R.id.updates_status)
        rows = requireViewById(R.id.updates_rows)
        checkButton = requireViewById(R.id.updates_check_now)

        checkButton.setOnClickListener {
            runCheck()
        }
        render(emptyList())
    }

    override fun onResume() {
        super.onResume()
        render(emptyList())
        runCheck()
    }

    private fun runCheck() {
        checkButton.isEnabled = false
        status.text = getText(R.string.updates_checking)
        UpdateEngine.checkAsync(this) { results ->
            runOnUiThread {
                checkButton.isEnabled = true
                render(results)
            }
        }
    }

    private fun render(results: List<UpdateEngine.Result>) {
        val pm = packageManager
        val inflater = layoutInflater
        val df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM)
        rows.removeAllViews()

        val lastCheck = TrackedApps.prefs(this).getLong("last_check", 0L)
        val knownResults = results.associateBy { it.app.packageName }

        for (app in TrackedApps.TRACKED) {
            val row = inflater.inflate(R.layout.update_row, rows, false) as LinearLayout
            row.requireViewById<TextView>(R.id.update_row_label).text =
                app.label ?: run {
                    try {
                        pm.getApplicationInfo(app.packageName, 0).loadLabel(pm)
                    } catch (e: PackageManager.NameNotFoundException) {
                        app.packageName
                    }
                }
            val detail = row.requireViewById<TextView>(R.id.update_row_detail)
            val installed = TrackedApps.installedInfo(this, app.packageName)
            val result = knownResults[app.packageName]
            detail.text = when (result) {
                is UpdateEngine.Result.Installed ->
                    getString(R.string.updates_row_installed, result.tag)
                is UpdateEngine.Result.Failed ->
                    getString(R.string.updates_row_failed, result.reason)
                is UpdateEngine.Result.UpToDate,
                null,
                -> when {
                    installed == null -> getString(R.string.updates_row_missing)
                    else -> getString(R.string.updates_row_current, installed.versionName)
                }
            }
            rows.addView(row)
        }

        status.text = when {
            results.isNotEmpty() -> getString(R.string.updates_last_check, df.format(Date()))
            lastCheck != 0L -> getString(R.string.updates_last_check, df.format(Date(lastCheck)))
            else -> getText(R.string.updates_never_checked)
        }
    }
}