/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.music;

import com.android.music.MusicUtils.ServiceToken;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;
import android.os.IBinder;
import android.os.RemoteException;

public class MusicBrowserActivity extends Activity
    implements MusicUtils.Defs {

    private ServiceToken mToken;

    public MusicBrowserActivity() {
    }

    /**
     * Called when the activity is first created.
     */
    // KitKat UI P16 modernization: 4.4 predates runtime permissions; the
    // library browser cannot read MediaStore without a READ_EXTERNAL_STORAGE
    // grant on API 23+. Request before the first query; render normally
    // when granted (MusicUtils activates tabs lazily on resume, so the
    // grant lands before the first cursor runs).
    private static final int PERMISSION_REQUEST_STORAGE = 1;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        if (requestCode != PERMISSION_REQUEST_STORAGE) {
            return;
        }
        if (grantResults.length == 0
                || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.storage_permission_denied,
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[] { android.Manifest.permission.READ_EXTERNAL_STORAGE },
                    PERMISSION_REQUEST_STORAGE);
        }
        int activeTab = MusicUtils.getIntPref(this, "activetab", R.id.artisttab);
        if (activeTab != R.id.artisttab
                && activeTab != R.id.albumtab
                && activeTab != R.id.songtab
                && activeTab != R.id.playlisttab) {
            activeTab = R.id.artisttab;
        }
        MusicUtils.activateTab(this, activeTab);
        
        String shuf = getIntent().getStringExtra("autoshuffle");
        if ("true".equals(shuf)) {
            mToken = MusicUtils.bindToService(this, autoshuffle);
        }
    }

    @Override
    public void onDestroy() {
        if (mToken != null) {
            MusicUtils.unbindFromService(mToken);
        }
        super.onDestroy();
    }

    private ServiceConnection autoshuffle = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            // we need to be able to bind again, so unbind
            try {
                unbindService(this);
            } catch (IllegalArgumentException e) {
            }
            IMediaPlaybackService serv = IMediaPlaybackService.Stub.asInterface(obj);
            if (serv != null) {
                try {
                    serv.setShuffleMode(MediaPlaybackService.SHUFFLE_AUTO);
                } catch (RemoteException ex) {
                }
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
        }
    };

}

