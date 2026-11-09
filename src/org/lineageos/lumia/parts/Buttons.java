/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lumia.parts;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.UserHandle;

import lineageos.hardware.LineageHardwareManager;
import lineageos.providers.LineageSettings;

/**
 * The capacitive keys (Back, Start, Search: Synaptics F1A buttons) through
 * the LineageOS settings that LineageParts > Buttons also uses:
 *  - FORCE_SHOW_NAVBAR: PhoneWindowManager shows the navigation bar and
 *    turns the keys off with the key disabler of the device's touch HAL
 *    (vendor.lineage.touch, LineageHardwareManager.FEATURE_KEY_DISABLE);
 *  - BUTTON_BRIGHTNESS, BUTTON_BACKLIGHT_TIMEOUT and
 *    BUTTON_BACKLIGHT_ONLY_WHEN_PRESSED: PowerManagerService drives the
 *    buttons light of the lights HAL (PM8941 KPDBL rows).
 */
final class Buttons {

    /* config_deviceHardwareKeys bits that can have a backlight */
    private static final int KEY_MASK_BACKLIT = 0x01 | 0x02 | 0x04 | 0x08 | 0x10;

    /* config_deviceSupportsButtonBrightnessControl: 1 = on/off only */
    private static final int BRIGHTNESS_TOGGLE_ONLY = 1;

    /* LineageParts defaults */
    static final int DEFAULT_TIMEOUT_MS = 5000;

    static final Uri NAVBAR_URI =
            LineageSettings.System.getUriFor(LineageSettings.System.FORCE_SHOW_NAVBAR);
    static final Uri BRIGHTNESS_URI =
            LineageSettings.Secure.getUriFor(LineageSettings.Secure.BUTTON_BRIGHTNESS);
    static final Uri TIMEOUT_URI =
            LineageSettings.Secure.getUriFor(LineageSettings.Secure.BUTTON_BACKLIGHT_TIMEOUT);
    static final Uri ONLY_WHEN_PRESSED_URI = LineageSettings.System.getUriFor(
            LineageSettings.System.BUTTON_BACKLIGHT_ONLY_WHEN_PRESSED);

    private Buttons() {}

    static boolean hasKeyDisabler(Context context) {
        LineageHardwareManager hardware = LineageHardwareManager.getInstance(context);
        return hardware != null && hardware.isSupported(LineageHardwareManager.FEATURE_KEY_DISABLE);
    }

    static boolean hasBacklight(Context context) {
        int keys = context.getResources().getInteger(
                org.lineageos.platform.internal.R.integer.config_deviceHardwareKeys);
        return brightnessControl(context) != 0 && (keys & KEY_MASK_BACKLIT) != 0;
    }

    private static int brightnessControl(Context context) {
        return context.getResources().getInteger(org.lineageos.platform.internal.R.integer
                .config_deviceSupportsButtonBrightnessControl);
    }

    /** The light has no levels, only on and off. */
    static boolean isBacklightToggleOnly(Context context) {
        return brightnessControl(context) == BRIGHTNESS_TOGGLE_ONLY;
    }

    static float defaultBrightness(Context context) {
        return context.getResources().getFloat(org.lineageos.platform.internal.R.dimen
                .config_buttonBrightnessSettingDefaultFloat);
    }

    static boolean isNavbarEnabled(Context context) {
        return LineageSettings.System.getIntForUser(context.getContentResolver(),
                LineageSettings.System.FORCE_SHOW_NAVBAR, 0, UserHandle.USER_CURRENT) != 0;
    }

    static void setNavbarEnabled(Context context, boolean enabled) {
        LineageSettings.System.putIntForUser(context.getContentResolver(),
                LineageSettings.System.FORCE_SHOW_NAVBAR, enabled ? 1 : 0,
                UserHandle.USER_CURRENT);
    }

    /** 0.0 (off) to 1.0 */
    static float getBrightness(Context context) {
        return LineageSettings.Secure.getFloat(context.getContentResolver(),
                LineageSettings.Secure.BUTTON_BRIGHTNESS, defaultBrightness(context));
    }

    static void setBrightness(Context context, float brightness) {
        LineageSettings.Secure.putFloat(context.getContentResolver(),
                LineageSettings.Secure.BUTTON_BRIGHTNESS, brightness);
    }

    /** Milliseconds, 0 = the light stays on while the screen is on */
    static int getTimeout(Context context) {
        return LineageSettings.Secure.getInt(context.getContentResolver(),
                LineageSettings.Secure.BUTTON_BACKLIGHT_TIMEOUT, DEFAULT_TIMEOUT_MS);
    }

    static void setTimeout(Context context, int timeoutMs) {
        LineageSettings.Secure.putInt(context.getContentResolver(),
                LineageSettings.Secure.BUTTON_BACKLIGHT_TIMEOUT, timeoutMs);
    }

    static boolean isOnlyWhenPressed(Context context) {
        ContentResolver resolver = context.getContentResolver();
        return LineageSettings.System.getInt(resolver,
                LineageSettings.System.BUTTON_BACKLIGHT_ONLY_WHEN_PRESSED, 0) == 1;
    }

    static void setOnlyWhenPressed(Context context, boolean enabled) {
        LineageSettings.System.putInt(context.getContentResolver(),
                LineageSettings.System.BUTTON_BACKLIGHT_ONLY_WHEN_PRESSED, enabled ? 1 : 0);
    }
}
