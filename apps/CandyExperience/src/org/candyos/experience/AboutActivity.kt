package org.candyos.experience

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class AboutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about_activity)

        requireViewById<TextView>(R.id.candy_version).text =
            SysInfo.candyVersion().ifBlank { getText(R.string.about_unknown) }
        requireViewById<TextView>(R.id.android_version).text = SysInfo.androidVersion()
        requireViewById<TextView>(R.id.patch_level).text = SysInfo.patchLevel()
        requireViewById<TextView>(R.id.build_date).text =
            SysInfo.buildDate().ifBlank { getText(R.string.about_unknown) }
    }
}