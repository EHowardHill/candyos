package org.candyos.experience

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.ViewFlipper

class SetupActivity : Activity() {

    private lateinit var flipper: ViewFlipper
    private lateinit var back: Button
    private lateinit var next: Button
    private lateinit var networkIcon: ImageView
    private lateinit var networkStatus: TextView
    private lateinit var navGroup: RadioGroup
    private lateinit var navHint: TextView
    private lateinit var dots: LinearLayout
    private var appRows: Map<String, android.widget.CheckBox> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If the device is already provisioned, this activity was reached by
        // a stale HOME resolution: the wizard is done, the menu is the app.
        if (Settings.Global.getInt(
                contentResolver, Settings.Global.DEVICE_PROVISIONED, 0) == 1) {
            startActivity(Intent(this, MenuActivity::class.java))
            finish()
            return
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        setContentView(R.layout.setup_activity)

        flipper = requireViewById(R.id.steps)
        back = requireViewById(R.id.back)
        next = requireViewById(R.id.next)
        networkIcon = requireViewById(R.id.network_icon)
        networkStatus = requireViewById(R.id.network_status)
        navGroup = requireViewById(R.id.nav_group)
        navHint = requireViewById(R.id.nav_hint)
        dots = requireViewById(R.id.dots)

        requireViewById<Button>(R.id.pick_wifi).setOnClickListener {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }

        back.setOnClickListener {
            if (flipper.displayedChild > 0) {
                flipper.showPrevious()
                updateChrome()
            } else {
                moveTaskToBack(true)
            }
        }
        next.setOnClickListener {
            if (flipper.displayedChild < flipper.childCount - 1) {
                flipper.showNext()
                updateChrome()
            } else {
                finishSetup()
            }
        }

        navGroup.setOnCheckedChangeListener { _, _ -> updateNavHint() }

        buildDots()
        NavConfig.addRadioGroup(this, navGroup, R.id.nav_three, R.id.nav_gesture)
        appRows = AppsConfig.addRows(this, requireViewById(R.id.apps_rows))
        updateChrome()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    override fun onResume() {
        super.onResume()
        appRows = AppsConfig.addRows(this, requireViewById(R.id.apps_rows))
        if (::flipper.isInitialized) {
            updateNetworkStatus()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        back.performClick()
    }

    private fun buildDots() {
        dots.removeAllViews()
        val dp = resources.displayMetrics.density
        for (i in 0 until flipper.childCount) {
            val dot = ImageView(this).apply {
                setImageResource(R.drawable.ic_dot)
                alpha = DOT_ALPHA_INACTIVE
                val side = (10 * dp).toInt()
                layoutParams = LinearLayout.LayoutParams(side, side).apply {
                    setMargins((3 * dp).toInt(), 0, (3 * dp).toInt(), 0)
                }
            }
            dots.addView(dot)
        }
    }

    private fun updateChrome() {
        val step = flipper.displayedChild
        back.visibility = if (step == 0) View.INVISIBLE else View.VISIBLE
        if (step == flipper.childCount - 1) {
            next.setText(R.string.start_label)
        } else {
            next.setText(R.string.next_label)
        }
        for (i in 0 until dots.childCount) {
            dots.getChildAt(i).alpha =
                if (i == step) DOT_ALPHA_ACTIVE else DOT_ALPHA_INACTIVE
        }
        when (step) {
            STEP_NETWORK -> updateNetworkStatus()
            STEP_APPS -> appRows = AppsConfig.addRows(this, requireViewById(R.id.apps_rows))
            STEP_NAV -> updateNavHint()
        }
    }

    private fun updateNavHint() {
        navHint.setText(
            if (navGroup.checkedRadioButtonId == R.id.nav_gesture) {
                R.string.setup_nav_gesture_body
            } else {
                R.string.setup_nav_three_body
            })
    }

    private fun updateNetworkStatus() {
        val cm = getSystemService(ConnectivityManager::class.java)
        val network = cm?.activeNetwork
        val info = network?.let { cm.getNetworkInfo(it) }
        if (info != null && info.isConnected) {
            val name = info.extraInfo?.takeIf { it.isNotBlank() } ?: info.typeName
            networkStatus.text = getString(R.string.setup_network_connected, name)
            networkIcon.setImageResource(R.drawable.ic_wifi_on)
        } else {
            networkStatus.setText(R.string.setup_network_not_connected)
            networkIcon.setImageResource(R.drawable.ic_wifi_off)
        }
    }

    private fun finishSetup() {
        AppsConfig.applySelections(this, appRows)
        NavConfig.applyMode(this, NavConfig.selectedMode(navGroup, R.id.nav_gesture))
        Settings.Global.putInt(contentResolver, Settings.Global.DEVICE_PROVISIONED, 1)
        Settings.Secure.putInt(contentResolver, Settings.Secure.USER_SETUP_COMPLETE, 1)

        // Retire the wizard: with DEVICE_PROVISIONED set this component must
        // stop answering HOME, or the system resolves Home straight back into
        // the finished wizard (the reported reopen bug). Disabling our own
        // setup activity is what the stock SUWs do post-provisioning too; the
        // MenuActivity (the drawer entry) carries the app from here on.
        packageManager.setComponentEnabledSetting(
            ComponentName(this, SetupActivity::class.java),
            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            android.content.pm.PackageManager.DONT_KILL_APP)

        // Land on the Launcher - the menu is only reached when the user
        // explicitly opens CandyOS Experience from the drawer afterwards.
        finishAndRemoveTask()
    }

    companion object {
        private const val STEP_NETWORK = 1
        private const val STEP_APPS = 2
        private const val STEP_NAV = 3
        private const val DOT_ALPHA_ACTIVE = 1f
        private const val DOT_ALPHA_INACTIVE = 0.3f
    }
}