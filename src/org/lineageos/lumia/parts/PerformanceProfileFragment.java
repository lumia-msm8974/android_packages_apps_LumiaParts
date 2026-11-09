/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lumia.parts;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.RadioButtonPreference;

import java.util.ArrayList;
import java.util.List;

/** One radio button per profile of the power HAL, lowest power first. */
public class PerformanceProfileFragment extends PreferenceFragmentCompat
        implements RadioButtonPreference.OnClickListener, SettingsObserver.Listener {

    private final List<RadioButtonPreference> mButtons = new ArrayList<>();
    private final List<Integer> mProfiles = new ArrayList<>();
    private FooterPreference mBatterySaverNote;
    private SettingsObserver mObserver;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = requireContext();
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
        setPreferenceScreen(screen);

        mBatterySaverNote = new FooterPreference(context);
        mBatterySaverNote.setKey("battery_saver_note");
        mBatterySaverNote.setTitle(R.string.profile_battery_saver_note);
        mBatterySaverNote.setOrder(0);
        mBatterySaverNote.setVisible(false);
        screen.addPreference(mBatterySaverNote);

        int order = 1;
        for (int profile : PerformanceProfiles.available(context)) {
            RadioButtonPreference button = new RadioButtonPreference(context);
            button.setKey("profile_" + profile);
            button.setTitle(PerformanceProfiles.name(context, profile));
            button.setSummary(PerformanceProfiles.description(context, profile));
            button.setPersistent(false);
            button.setOrder(order++);
            button.setOnClickListener(this);
            screen.addPreference(button);
            mButtons.add(button);
            mProfiles.add(profile);
        }

        FooterPreference footer = new FooterPreference(context);
        footer.setKey("profiles_note");
        footer.setTitle(mProfiles.isEmpty()
                ? R.string.profile_unavailable : R.string.profile_footer);
        screen.addPreference(footer);

        mObserver = new SettingsObserver(context, this, PerformanceProfiles.SETTING_URI);
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

    @Override
    public void onRadioButtonClicked(RadioButtonPreference clicked) {
        int index = mButtons.indexOf(clicked);
        if (index >= 0) {
            PerformanceProfiles.set(requireContext(), mProfiles.get(index));
        }
        update();
    }

    private void update() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        int active = PerformanceProfiles.active(context);
        for (int i = 0; i < mButtons.size(); i++) {
            mButtons.get(i).setChecked(mProfiles.get(i) == active);
        }
        mBatterySaverNote.setVisible(PerformanceProfiles.isForcedByBatterySaver(context));
    }
}
