package org.candyos.experience

import android.app.Activity
import android.os.Bundle

/**
 * The Testing second activity - ported from the sunset vendor/testing app.
 * Exists to exercise the ported 4.4 activity transitions, to put a card
 * into the SystemUI-side KitKatRecentsActivity, and to test the close
 * animation on finish.
 */
class TestSecondActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test_second)
        requireViewById<android.view.View>(R.id.test_second_finish)
            .setOnClickListener { finish() }
    }
}