/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lumia.keyhandler;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.UserHandle;
import android.provider.Settings;

/**
 * What the camera button does, shared by the key handler (system_server)
 * and the settings page. Stored in Settings.Secure, so it survives reboots.
 * An action is "none", "camera", "torch" or "app:<flattened component>".
 */
public final class CameraKeyConfig {

    public static final String SETTING_SHORT = "lumia_camera_key_short";
    public static final String SETTING_LONG = "lumia_camera_key_long";
    public static final String SETTING_SCREEN_OFF = "lumia_camera_key_screen_off";
    public static final String SETTING_CLOSE_ON_SCREEN_OFF =
            "lumia_camera_key_close_on_screen_off";

    public static final String ACTION_NONE = "none";
    public static final String ACTION_CAMERA = "camera";
    public static final String ACTION_TORCH = "torch";
    public static final String ACTION_APP = "app";

    private static final String APP_PREFIX = ACTION_APP + ":";

    public static final String DEFAULT_SHORT = ACTION_CAMERA;
    public static final String DEFAULT_LONG = ACTION_NONE;

    private CameraKeyConfig() {}

    public static String getShort(ContentResolver resolver) {
        return get(resolver, SETTING_SHORT, DEFAULT_SHORT);
    }

    public static String getLong(ContentResolver resolver) {
        return get(resolver, SETTING_LONG, DEFAULT_LONG);
    }

    private static String get(ContentResolver resolver, String setting, String fallback) {
        String value = Settings.Secure.getStringForUser(resolver, setting,
                UserHandle.USER_CURRENT);
        return value == null || value.isEmpty() ? fallback : value;
    }

    public static void set(ContentResolver resolver, String setting, String action) {
        Settings.Secure.putStringForUser(resolver, setting, action, UserHandle.USER_CURRENT);
    }

    public static boolean isScreenOffEnabled(ContentResolver resolver) {
        return Settings.Secure.getIntForUser(resolver, SETTING_SCREEN_OFF, 1,
                UserHandle.USER_CURRENT) != 0;
    }

    public static void setScreenOffEnabled(ContentResolver resolver, boolean enabled) {
        Settings.Secure.putIntForUser(resolver, SETTING_SCREEN_OFF, enabled ? 1 : 0,
                UserHandle.USER_CURRENT);
    }

    public static boolean isCloseOnScreenOffEnabled(ContentResolver resolver) {
        return Settings.Secure.getIntForUser(resolver, SETTING_CLOSE_ON_SCREEN_OFF, 1,
                UserHandle.USER_CURRENT) != 0;
    }

    public static void setCloseOnScreenOffEnabled(ContentResolver resolver, boolean enabled) {
        Settings.Secure.putIntForUser(resolver, SETTING_CLOSE_ON_SCREEN_OFF, enabled ? 1 : 0,
                UserHandle.USER_CURRENT);
    }

    public static Uri uri(String setting) {
        return Settings.Secure.getUriFor(setting);
    }

    public static String appAction(ComponentName component) {
        return APP_PREFIX + component.flattenToString();
    }

    public static boolean isApp(String action) {
        return action.startsWith(APP_PREFIX);
    }

    /** The app of an "app:" action, null if the value is malformed. */
    public static ComponentName appComponent(String action) {
        return isApp(action)
                ? ComponentName.unflattenFromString(action.substring(APP_PREFIX.length()))
                : null;
    }
}
