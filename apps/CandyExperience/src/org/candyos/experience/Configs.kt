package org.candyos.experience

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.SystemProperties
import android.provider.Settings
import android.widget.CheckBox
import android.widget.LinearLayout

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

/**
 * The Google-services (microG + Aurora Store) toggle set — the dedicated
 * OTE step and the Experience menu "Google services" screen share it.
 *
 * The packages are preloaded disabled (vendor/partner_gms/microg); both
 * surfaces just flip their enabled state per user, so "installed" is
 * entirely the user's call, at first run and afterwards. The microG
 * Services + Companion pair is one logical row: microG's Play services
 * are what apps bind to, the Companion is the Play Store stub that
 * keeps their presence checks happy.
 */
object PlayConfig {
    const val GMS = "com.google.android.gms"
    const val VENDING = "com.android.vending"
    const val AURORA = "com.aurora.store"

    private val MICROG = listOf(GMS, VENDING)
    val ALL = MICROG + AURORA

    /** Rows: row key -> the packages the row toggles. */
    private val ROWS = linkedMapOf(
        "microg" to MICROG,
        "aurora" to listOf(AURORA),
    )

    private fun info(context: Context, pkg: String) =
        try {
            context.packageManager.getApplicationInfo(
                pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

    private fun rowEnabled(context: Context, packages: List<String>) =
        packages.all { AppsConfig.isEnabled(context, it) }

    fun addRows(
        context: Context,
        container: LinearLayout,
        onToggle: ((key: String, checked: Boolean) -> Unit)? = null,
    ): Map<String, CheckBox> {
        container.removeAllViews()
        val rows = mutableMapOf<String, CheckBox>()
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE)
            as android.view.LayoutInflater
        for ((key, packages) in ROWS) {
            val lead = info(context, packages.first()) ?: continue
            val row = inflater.inflate(R.layout.app_row, container, false) as LinearLayout
            val icon = row.requireViewById<android.widget.ImageView>(R.id.app_icon)
            val label = row.requireViewById<android.widget.TextView>(R.id.app_label)
            val check = row.requireViewById<CheckBox>(R.id.app_check)
            icon.setImageDrawable(lead.loadIcon(context.packageManager))
            label.text = if (key == "microg") {
                context.getString(R.string.play_microg_label)
            } else {
                context.getString(R.string.play_aurora_label)
            }
            check.isChecked = rowEnabled(context, packages)
            row.setOnClickListener {
                check.toggle()
                onToggle?.invoke(key, check.isChecked)
            }
            container.addView(row)
            rows[key] = check
        }
        return rows
    }

    fun applySelections(context: Context, rows: Map<String, CheckBox>) {
        val pm = context.packageManager
        for ((key, box) in rows) {
            val packages = ROWS[key] ?: continue
            val state = if (box.isChecked) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
            }
            for (pkg in packages) {
                if (info(context, pkg) != null) {
                    pm.setApplicationEnabledSetting(pkg, state, 0)
                }
            }
        }
    }

    /**
     * Fresh-boot default: the preloads are enabled at install, so a brand
     * new device would briefly run microG before the wizard asks. Called
     * from the (once-only) OTE wizard at startup to pin everything off
     * until the user opts in — the step checkboxes then show the truth.
     */
    fun ensureFreshDefaults(context: Context) {
        val pm = context.packageManager
        for (pkg in ALL) {
            if (info(context, pkg) != null) {
                pm.setApplicationEnabledSetting(
                    pkg, PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER, 0)
            }
        }
    }
}

object SysInfo {
    fun candyVersion(): String =
        SystemProperties.get("ro.lineage.version")
            .ifBlank { SystemProperties.get("ro.lineage.display.version", "") }

    fun androidVersion(): String = android.os.Build.VERSION.RELEASE

    fun patchLevel(): String = android.os.Build.VERSION.SECURITY_PATCH

    fun buildDate(): String = SystemProperties.get("ro.build.date", "")
}