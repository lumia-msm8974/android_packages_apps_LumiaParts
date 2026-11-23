/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lumia.parts;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreference;

import org.lineageos.lumia.keyhandler.CameraKeyConfig;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

/** Chooses what the camera button does on a press and on a long press. */
public class CameraKeyFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener, SettingsObserver.Listener {

    private ListPreference mShort;
    private ListPreference mLong;
    private SwitchPreference mScreenOff;
    private SwitchPreference mCloseOnScreenOff;
    private SettingsObserver mObserver;

    private static final class AppEntry {
        final ComponentName component;
        final String label;
        final Drawable icon;

        AppEntry(ComponentName component, String label, Drawable icon) {
            this.component = component;
            this.label = label;
            this.icon = icon;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.camera_key_settings, rootKey);
        Context context = requireContext();

        List<CharSequence> entries = new ArrayList<>();
        List<CharSequence> values = new ArrayList<>();
        addAction(entries, values, context.getString(R.string.camera_key_action_camera),
                CameraKeyConfig.ACTION_CAMERA);
        if (CameraKey.hasTorch(context)) {
            addAction(entries, values, context.getString(R.string.camera_key_action_torch),
                    CameraKeyConfig.ACTION_TORCH);
        }
        addAction(entries, values, context.getString(R.string.camera_key_action_app),
                CameraKeyConfig.ACTION_APP);
        addAction(entries, values, context.getString(R.string.camera_key_action_none),
                CameraKeyConfig.ACTION_NONE);
        CharSequence[] entryArray = entries.toArray(new CharSequence[0]);
        CharSequence[] valueArray = values.toArray(new CharSequence[0]);

        mShort = findPreference("camera_key_short");
        mLong = findPreference("camera_key_long");
        mScreenOff = findPreference("camera_key_screen_off");
        mCloseOnScreenOff = findPreference("camera_key_close_on_screen_off");
        for (ListPreference list : new ListPreference[] {mShort, mLong}) {
            list.setEntries(entryArray);
            list.setEntryValues(valueArray);
            list.setOnPreferenceChangeListener(this);
        }
        mScreenOff.setOnPreferenceChangeListener(this);
        mCloseOnScreenOff.setOnPreferenceChangeListener(this);

        mObserver = new SettingsObserver(context, this,
                CameraKeyConfig.uri(CameraKeyConfig.SETTING_SHORT),
                CameraKeyConfig.uri(CameraKeyConfig.SETTING_LONG),
                CameraKeyConfig.uri(CameraKeyConfig.SETTING_SCREEN_OFF),
                CameraKeyConfig.uri(CameraKeyConfig.SETTING_CLOSE_ON_SCREEN_OFF));
    }

    private static void addAction(List<CharSequence> entries, List<CharSequence> values,
            String entry, String value) {
        entries.add(entry);
        values.add(value);
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
    public void onDisplayPreferenceDialog(Preference preference) {
        if (preference instanceof ListPreference) {
            ListPreferenceDialog.show(requireActivity(), (ListPreference) preference);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
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
        ContentResolver resolver = context.getContentResolver();
        show(context, mShort, CameraKeyConfig.getShort(resolver));
        show(context, mLong, CameraKeyConfig.getLong(resolver));
        mScreenOff.setChecked(CameraKeyConfig.isScreenOffEnabled(resolver));
        mCloseOnScreenOff.setChecked(CameraKeyConfig.isCloseOnScreenOffEnabled(resolver));
    }

    private static void show(Context context, ListPreference list, String action) {
        list.setValue(CameraKeyConfig.isApp(action) ? CameraKeyConfig.ACTION_APP : action);
        list.setSummary(CameraKey.label(context, action));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = requireContext().getContentResolver();
        if (preference == mScreenOff) {
            CameraKeyConfig.setScreenOffEnabled(resolver, (Boolean) newValue);
            return true;
        }
        if (preference == mCloseOnScreenOff) {
            CameraKeyConfig.setCloseOnScreenOffEnabled(resolver, (Boolean) newValue);
            return true;
        }
        String setting = preference == mShort
                ? CameraKeyConfig.SETTING_SHORT : CameraKeyConfig.SETTING_LONG;
        if (CameraKeyConfig.ACTION_APP.equals(newValue)) {
            pickApp(setting);
            // The list follows the setting once an app is chosen
            return false;
        }
        CameraKeyConfig.set(resolver, setting, (String) newValue);
        return true;
    }

    private void pickApp(String setting) {
        final Activity activity = requireActivity();
        final PackageManager pm = activity.getPackageManager();
        new Thread(() -> {
            final List<AppEntry> apps = loadApps(activity, pm);
            activity.runOnUiThread(() -> {
                if (isAdded() && !activity.isFinishing()) {
                    showPicker(activity, apps, setting);
                }
            });
        }, "LumiaPartsApps").start();
    }

    private static List<AppEntry> loadApps(Context context, PackageManager pm) {
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        int size = Math.round(40 * context.getResources().getDisplayMetrics().density);
        List<AppEntry> apps = new ArrayList<>();
        for (ResolveInfo info : pm.queryIntentActivities(launcher, 0)) {
            Drawable icon = info.loadIcon(pm);
            icon.setBounds(0, 0, size, size);
            apps.add(new AppEntry(
                    new ComponentName(info.activityInfo.packageName, info.activityInfo.name),
                    info.loadLabel(pm).toString(), icon));
        }
        Collator collator = Collator.getInstance();
        apps.sort((a, b) -> collator.compare(a.label, b.label));
        return apps;
    }

    private void showPicker(Activity activity, List<AppEntry> apps, String setting) {
        ArrayAdapter<AppEntry> adapter = new ArrayAdapter<AppEntry>(activity,
                android.R.layout.select_dialog_item, android.R.id.text1, apps) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setCompoundDrawablesRelative(getItem(position).icon, null, null, null);
                view.setCompoundDrawablePadding(Math.round(
                        16 * view.getResources().getDisplayMetrics().density));
                return view;
            }
        };
        new AlertDialog.Builder(activity)
                .setTitle(R.string.camera_key_pick_app)
                .setAdapter(adapter, (dialog, which) -> CameraKeyConfig.set(
                        activity.getContentResolver(), setting,
                        CameraKeyConfig.appAction(apps.get(which).component)))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
