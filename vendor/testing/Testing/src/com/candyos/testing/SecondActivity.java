/*
 * CandyOS: the Testing app - second activity. Exists to exercise the
 * ported 4.4 activity transitions, to put a card into the SystemUI-side
 * KitKatRecentsActivity, and to test the close animation on finish.
 */
package com.candyos.testing;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class SecondActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.second);
    }

    public void onClick(View view) {
        finish();
    }
}
