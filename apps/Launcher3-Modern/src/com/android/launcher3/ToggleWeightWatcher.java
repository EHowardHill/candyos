package com.android.launcher3;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

public class ToggleWeightWatcher extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sp = getSharedPreferences(
                LauncherAppState.getSharedPreferencesKey(), MODE_PRIVATE);
        boolean show = sp.getBoolean(
                Launcher.SHOW_WEIGHT_WATCHER, Launcher.SHOW_WEIGHT_WATCHER_DEFAULT);
        sp.edit().putBoolean(Launcher.SHOW_WEIGHT_WATCHER, !show).commit();
        finish();
    }
}
