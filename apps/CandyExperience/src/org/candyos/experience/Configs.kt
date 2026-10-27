package org.candyos.experience

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.SystemProperties
import android.provider.Settings
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RadioGroup

class AppsConfig private constructor(
    val packageName: String,
    val label: CharSequence,
    val icon: Drawable?,
) {
    companion object {
        private val OPTIONAL_PACKAGES = listOf(
            "com.android.calculator2",
            "com.android.calendar",
            "com.android.deskclock",
            "com.android.music",
            "com.android.soundrecorder",
            "com.aurora.store",
        )

        fun optionalApps(context: Context): List<AppsConfig> {
            val pm = context.packageManager
            // MATCH_UNINSTALLED_PACKAGES so already-removed (disabled-user)
            // apps still show up in the list to be brought back.
            val flags = PackageManager.MATCH_UNINSTALLED_PACKAGES
            return OPTIONAL_PACKAGES.mapNotNull { pkg ->
                try {
                    val info = pm.getApplicationInfo(pkg, flags)
                    AppsConfig(pkg, info.loadLabel(pm), info.loadIcon(pm))
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
        }

        fun isEnabled(context: Context, packageName: String): Boolean =
            context.packageManager.getApplicationEnabledSetting(packageName) !in intArrayOf(
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            )

        fun addRows(context: Context, container: LinearLayout): Map<String, CheckBox> {
            container.removeAllViews()
            val rows = mutableMapOf<String, CheckBox>()
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
                as android.view.LayoutInflater
            for (app in optionalApps(context)) {
                val row = inflater.inflate(R.layout.app_row, container, false) as LinearLayout
                val icon = row.requireViewById<android.widget.ImageView>(R.id.app_icon)
                val label = row.requireViewById<android.widget.TextView>(R.id.app_label)
                val check = row.requireViewById<CheckBox>(R.id.app_check)
                icon.setImageDrawable(app.icon)
                label.text = app.label
                check.isChecked = isEnabled(context, app.packageName)
                // The whole row toggles, KitKat Settings style - no need to
                // aim for the little box.
                row.setOnClickListener { check.toggle() }
                container.addView(row)
                rows[app.packageName] = check
            }
            return rows
        }

        fun applySelections(context: Context, rows: Map<String, CheckBox>) {
            val pm = context.packageManager
            for ((pkg, box) in rows) {
                val state = if (box.isChecked) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                }
                pm.setApplicationEnabledSetting(pkg, state, 0)
            }
        }
    }
}

object NavConfig {
    const val MODE_THREE_BUTTON = 0
    const val MODE_GESTURE = 2
    private const val NAVIGATION_MODE = "navigation_mode"

    fun currentMode(context: Context): Int =
        Settings.Global.getInt(context.contentResolver, NAVIGATION_MODE, MODE_THREE_BUTTON)

    fun applyMode(context: Context, mode: Int) {
        Settings.Global.putInt(context.contentResolver, NAVIGATION_MODE, mode)
    }

    fun addRadioGroup(
        context: Context,
        group: RadioGroup,
        threeId: Int,
        gestureId: Int,
    ) {
        val current = currentMode(context)
        group.check(
            if (current == MODE_GESTURE) gestureId else threeId,
        )
    }

    fun selectedMode(group: RadioGroup, gestureId: Int): Int =
        if (group.checkedRadioButtonId == gestureId) MODE_GESTURE else MODE_THREE_BUTTON
}

object SysInfo {
    fun candyVersion(): String =
        SystemProperties.get("ro.lineage.version")
            .ifBlank { SystemProperties.get("ro.lineage.display.version", "") }

    fun androidVersion(): String = android.os.Build.VERSION.RELEASE

    fun patchLevel(): String = android.os.Build.VERSION.SECURITY_PATCH

    fun buildDate(): String = SystemProperties.get("ro.build.date", "")
}