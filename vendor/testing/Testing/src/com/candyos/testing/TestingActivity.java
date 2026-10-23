/*
 * CandyOS: the Testing app (P23) - a KitKat-styled test bench for the
 * CandyOS surfaces. The activity runs on Theme.DeviceDefault (== Holo via
 * the P1 series), so the widget gallery doubles as a theme-cascade test.
 *
 * See docs/phases/TASKS.md, Phase 23 for the full test matrix.
 */
package com.candyos.testing;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

public class TestingActivity extends Activity {

    private static final String CHANNEL_ID = "ticker_tests";
    private static final String KEY_LIGHT = "light";
    private static final String KEY_IMMERSIVE = "immersive";
    private static final int REQ_POST_NOTIFICATIONS = 1;

    private NotificationManager mNm;
    private boolean mLight;
    private boolean mImmersive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mLight = savedInstanceState.getBoolean(KEY_LIGHT);
            mImmersive = savedInstanceState.getBoolean(KEY_IMMERSIVE);
        }
        setTheme(mLight
                ? android.R.style.Theme_DeviceDefault_Light
                : android.R.style.Theme_DeviceDefault);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mNm = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                getString(R.string.channel_name), NotificationManager.IMPORTANCE_DEFAULT);
        mNm.createNotificationChannel(channel);

        Spinner spinner = findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                getResources().getStringArray(R.array.spinner_entries));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (mImmersive) {
            applyImmersive();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_LIGHT, mLight);
        outState.putBoolean(KEY_IMMERSIVE, mImmersive);
    }

    /**
     * targetSdk 34: without POST_NOTIFICATIONS the NMS ranks the app
     * IMPORTANCE_NONE - no shade entries, and the SystemUI listener (the
     * 4.4 ticker) never sees the posts. Guard every posting path.
     */
    private boolean ensureNotifPermission() {
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        Toast.makeText(this, R.string.perm_needed, Toast.LENGTH_LONG).show();
        requestPermissions(
                new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                REQ_POST_NOTIFICATIONS);
        return false;
    }

    /** android:onClick target for every button in main.xml. */
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_notif_long:
            case R.id.btn_notif_short:
            case R.id.btn_notif_burst:
            case R.id.btn_notif_group:
            case R.id.btn_notif_ongoing:
                if (!ensureNotifPermission()) {
                    return;
                }
                break;
            default:
                break;
        }

        switch (view.getId()) {
            case R.id.btn_notif_long:
                post(1, R.string.ticker_long_title, R.string.ticker_long_text);
                break;
            case R.id.btn_notif_short:
                post(2, R.string.ticker_short_title, R.string.ticker_short_text);
                break;
            case R.id.btn_notif_burst:
                post(3, R.string.chain1_title, R.string.chain1_text);
                post(4, R.string.chain2_title, R.string.chain2_text);
                post(5, R.string.chain3_title, R.string.chain3_text);
                break;
            case R.id.btn_notif_group:
                postGroup();
                break;
            case R.id.btn_notif_ongoing:
                post(8, R.string.ongoing_title, R.string.ongoing_text, true);
                break;
            case R.id.btn_notif_cancel:
                mNm.cancelAll();
                break;
            case R.id.btn_dialog:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_title)
                        .setMessage(R.string.dialog_msg)
                        .setPositiveButton(android.R.string.ok, null)
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
                break;
            case R.id.btn_toast:
                Toast.makeText(this, R.string.toast_msg, Toast.LENGTH_SHORT).show();
                break;
            case R.id.btn_second:
                startActivity(new Intent(this, SecondActivity.class));
                break;
            case R.id.btn_volume:
                AudioManager am = getSystemService(AudioManager.class);
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                break;
            case R.id.btn_immersive:
                mImmersive = !mImmersive;
                applyImmersive();
                break;
            case R.id.btn_rotate:
                boolean landscape = getResources().getConfiguration().orientation
                        == Configuration.ORIENTATION_LANDSCAPE;
                setRequestedOrientation(landscape
                        ? ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                        : ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
                break;
            case R.id.btn_theme:
                mLight = !mLight;
                recreate();
                break;
            default:
                break;
        }
    }

    private void post(int id, int titleRes, int textRes) {
        post(id, titleRes, textRes, false);
    }

    private void post(int id, int titleRes, int textRes, boolean ongoing) {
        post(id, getString(titleRes), getString(textRes), ongoing);
    }

    private void post(int id, CharSequence title, CharSequence text, boolean ongoing) {
        Notification n = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_testing)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setOngoing(ongoing)
                .setAutoCancel(!ongoing)
                .build();
        mNm.notify(id, n);
    }

    /**
     * Group test: the child must tick, the summary must not (the ticker
     * filters FLAG_GROUP_SUMMARY - see Ticker.add).
     */
    private void postGroup() {
        String group = "com.candyos.testing.GROUP";
        Notification child = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_testing)
                .setContentTitle(getString(R.string.group_child_title))
                .setContentText(getString(R.string.group_child_text))
                .setGroup(group)
                .setAutoCancel(true)
                .build();
        Notification summary = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_testing)
                .setContentTitle(getString(R.string.group_summary_title))
                .setContentText(getString(R.string.group_summary_text))
                .setGroup(group)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .build();
        mNm.notify(6, child);
        mNm.notify(7, summary);
    }

    private void applyImmersive() {
        View decor = getWindow().getDecorView();
        if (mImmersive) {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        } else {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }
}
