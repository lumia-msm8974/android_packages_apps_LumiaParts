/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lumia.parts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.UserHandle;

/**
 * Calls back on the main thread when one of the settings shown by a page or
 * a tile changes, from here, LineageParts, AOSP Settings or Battery Saver.
 */
final class SettingsObserver {

    interface Listener {
        void onSettingsChanged();
    }

    private final Context mContext;
    private final Listener mListener;
    private final Uri[] mUris;
    private boolean mRegistered;

    private final ContentObserver mObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange) {
            mListener.onSettingsChanged();
        }
    };

    private final BroadcastReceiver mPowerSaveReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mListener.onSettingsChanged();
        }
    };

    SettingsObserver(Context context, Listener listener, Uri... uris) {
        mContext = context;
        mListener = listener;
        mUris = uris;
    }

    void register() {
        if (mRegistered) {
            return;
        }
        for (Uri uri : mUris) {
            mContext.getContentResolver().registerContentObserver(uri, false, mObserver,
                    UserHandle.USER_ALL);
        }
        mContext.registerReceiver(mPowerSaveReceiver,
                new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
        mRegistered = true;
    }

    void unregister() {
        if (!mRegistered) {
            return;
        }
        mContext.getContentResolver().unregisterContentObserver(mObserver);
        mContext.unregisterReceiver(mPowerSaveReceiver);
        mRegistered = false;
    }
}
