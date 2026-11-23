/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lumia.parts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

import org.lineageos.lumia.keyhandler.CameraKeyConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Main page. Every entry is the same setting as in LineageParts or AOSP
 * Settings, so both places always agree; entries the build or the phone
 * does not support are left out.
 */
public class LumiaSettingsFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener, SettingsObserver.Listener {

    /* Key backlight timeouts offered, in seconds (0 = while the screen is on) */
    private static final int[] TIMEOUTS_S = {0, 2, 5, 10, 15, 30};

    private Preference mPerfProfile;
    private SwitchPreference mLowResolution;
    private SwitchPreference mDoubleTap;
    private Preference mCameraKey;
    private SwitchPreference mNavbar;
    private SeekBarPreference mBrightness;
    private SwitchPreference mBacklight;
    private ListPreference mTimeout;
    private SwitchPreference mOnlyWhenPressed;

    private boolean mHasProfiles;
    private boolean mHasLowResolution;
    private boolean mHasDoubleTap;
    private boolean mHasCameraKey;
    private boolean mHasKeyDisabler;
    private boolean mHasBacklight;
    private boolean mBacklightToggleOnly;

    private SettingsObserver mObserver;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.lumia_settings, rootKey);
        Context context = requireContext();

        mHasProfiles = PerformanceProfiles.isSupported(context);
        mHasLowResolution = DisplayResolution.isSupported();
        mHasDoubleTap = DoubleTapToWake.isSupported(context);
        mHasCameraKey = CameraKey.isSupported(context);
        mHasKeyDisabler = Buttons.hasKeyDisabler(context);
        mHasBacklight = Buttons.hasBacklight(context);
        mBacklightToggleOnly = Buttons.isBacklightToggleOnly(context);

        mPerfProfile = findPreference("perf_profile");
        mPerfProfile.setIntent(new Intent(context, PerformanceProfileActivity.class));
        if (!mHasProfiles) {
            removeCategory("category_performance");
        }

        mLowResolution = findPreference("low_resolution");
        mLowResolution.setOnPreferenceChangeListener(this);
        if (!mHasLowResolution) {
            removeCategory("category_display");
        }

        mDoubleTap = findPreference("double_tap_to_wake");
        mDoubleTap.setOnPreferenceChangeListener(this);
        if (!mHasDoubleTap) {
            removeCategory("category_gestures");
        }

        mCameraKey = findPreference("camera_key");
        mCameraKey.setIntent(new Intent(context, CameraKeyActivity.class));
        if (!mHasCameraKey) {
            removeCategory("category_camera_key");
        }

        mNavbar = findPreference("navigation_bar");
        mBrightness = findPreference("button_brightness");
        mBacklight = findPreference("button_backlight");
        mTimeout = findPreference("button_backlight_timeout");
        mOnlyWhenPressed = findPreference("button_backlight_only_when_pressed");
        PreferenceCategory buttons = findPreference("category_buttons");

        if (mHasKeyDisabler) {
            mNavbar.setOnPreferenceChangeListener(this);
        } else {
            buttons.removePreference(mNavbar);
        }
        if (mHasBacklight) {
            // One control for the light: levels or on/off
            buttons.removePreference(mBacklightToggleOnly ? mBrightness : mBacklight);
            mBrightness.setOnPreferenceChangeListener(this);
            mBacklight.setOnPreferenceChangeListener(this);
            mTimeout.setOnPreferenceChangeListener(this);
            mOnlyWhenPressed.setOnPreferenceChangeListener(this);
        } else {
            buttons.removePreference(mBrightness);
            buttons.removePreference(mBacklight);
            buttons.removePreference(mTimeout);
            buttons.removePreference(mOnlyWhenPressed);
        }
        if (!mHasKeyDisabler && !mHasBacklight) {
            removeCategory("category_buttons");
        }

        mObserver = new SettingsObserver(context, this,
                PerformanceProfiles.SETTING_URI, DoubleTapToWake.SETTING_URI,
                DisplayResolution.SIZE_URI,
                CameraKeyConfig.uri(CameraKeyConfig.SETTING_SHORT),
                CameraKeyConfig.uri(CameraKeyConfig.SETTING_LONG),
                Buttons.NAVBAR_URI, Buttons.BRIGHTNESS_URI, Buttons.TIMEOUT_URI,
                Buttons.ONLY_WHEN_PRESSED_URI);
    }

    private void removeCategory(String key) {
        Preference category = findPreference(key);
        if (category != null) {
            getPreferenceScreen().removePreference(category);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mObserver.register();
        update();
    }

    @Override
    public void onStop() {
        mObserver.unregister();
        super.onStop();
    }

    @Override
    public void onSettingsChanged() {
        update();
    }

    private void update() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        if (mHasProfiles) {
            mPerfProfile.setSummary(PerformanceProfiles.summary(context));
        }
        if (mHasLowResolution) {
            mLowResolution.setChecked(DisplayResolution.isLowResolution());
        }
        if (mHasDoubleTap) {
            mDoubleTap.setChecked(DoubleTapToWake.isEnabled(context));
        }
        if (mHasCameraKey) {
            mCameraKey.setSummary(CameraKey.summary(context));
        }

        boolean navbar = mHasKeyDisabler && Buttons.isNavbarEnabled(context);
        if (mHasKeyDisabler) {
            mNavbar.setChecked(navbar);
        }
        if (mHasBacklight) {
            float brightness = Buttons.getBrightness(context);
            boolean lit = brightness > 0.0f;
            mBrightness.setValue(Math.round(brightness * 100.0f));
            mBacklight.setChecked(lit);
            updateTimeout(context);
            mOnlyWhenPressed.setChecked(Buttons.isOnlyWhenPressed(context));

            // The keys are off with the navigation bar: nothing to light
            mBrightness.setEnabled(!navbar);
            mBacklight.setEnabled(!navbar);
            mTimeout.setEnabled(!navbar && lit);
            mOnlyWhenPressed.setEnabled(!navbar && lit);
        }
    }

    private void updateTimeout(Context context) {
        int current = Buttons.getTimeout(context) / 1000;
        List<Integer> seconds = new ArrayList<>();
        for (int s : TIMEOUTS_S) {
            seconds.add(s);
        }
        // Keep a value chosen in LineageParts (any second from 0 to 30)
        if (!seconds.contains(current)) {
            int i = 0;
            while (i < seconds.size() && seconds.get(i) < current) {
                i++;
            }
            seconds.add(i, current);
        }
        CharSequence[] entries = new CharSequence[seconds.size()];
        CharSequence[] values = new CharSequence[seconds.size()];
        for (int i = 0; i < seconds.size(); i++) {
            entries[i] = timeoutText(context, seconds.get(i));
            values[i] = Integer.toString(seconds.get(i));
        }
        mTimeout.setEntries(entries);
        mTimeout.setEntryValues(values);
        mTimeout.setValue(Integer.toString(current));
        mTimeout.setSummary(timeoutText(context, current));
    }

    private static String timeoutText(Context context, int seconds) {
        if (seconds == 0) {
            return context.getString(R.string.button_timeout_never);
        }
        return context.getResources().getQuantityString(R.plurals.button_timeout_seconds,
                seconds, seconds);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Context context = requireContext();
        if (preference == mLowResolution) {
            DisplayResolution.setLowResolution((Boolean) newValue);
        } else if (preference == mDoubleTap) {
            DoubleTapToWake.setEnabled(context, (Boolean) newValue);
        } else if (preference == mNavbar) {
            Buttons.setNavbarEnabled(context, (Boolean) newValue);
        } else if (preference == mBrightness) {
            Buttons.setBrightness(context, (Integer) newValue / 100.0f);
        } else if (preference == mBacklight) {
            Buttons.setBrightness(context,
                    (Boolean) newValue ? Buttons.defaultBrightness(context) : 0.0f);
        } else if (preference == mTimeout) {
            Buttons.setTimeout(context, Integer.parseInt((String) newValue) * 1000);
        } else if (preference == mOnlyWhenPressed) {
            Buttons.setOnlyWhenPressed(context, (Boolean) newValue);
        } else {
            return false;
        }
        // The observer refreshes the page; return true so the widget follows now
        return true;
    }
}
