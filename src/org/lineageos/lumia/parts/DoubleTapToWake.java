/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lumia.parts;

import android.content.Context;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;

/**
 * Double tap to wake, the AOSP setting (also Settings > Display > Tap to
 * wake). PowerManagerService sends it to the power HAL as
 * Mode::DOUBLE_TAP_TO_WAKE, and the HAL writes the wake_gesture switch of
 * the Synaptics touch driver (TARGET_TAP_TO_WAKE_NODE of the device tree).
 */
final class DoubleTapToWake {

    static final Uri SETTING_URI = Settings.Secure.getUriFor(Settings.Secure.DOUBLE_TAP_TO_WAKE);

    private DoubleTapToWake() {}

    /** The device overlay declares support (config_supportDoubleTapWake). */
    static boolean isSupported(Context context) {
        return context.getResources().getBoolean(
                com.android.internal.R.bool.config_supportDoubleTapWake);
    }

    static boolean isEnabled(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(),
                Settings.Secure.DOUBLE_TAP_TO_WAKE, 0, UserHandle.USER_CURRENT) != 0;
    }

    static void setEnabled(Context context, boolean enabled) {
        Settings.Secure.putIntForUser(context.getContentResolver(),
                Settings.Secure.DOUBLE_TAP_TO_WAKE, enabled ? 1 : 0, UserHandle.USER_CURRENT);
        SummaryProvider.notifyChange(context);
    }
}
