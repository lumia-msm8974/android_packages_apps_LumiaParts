/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lumia.parts;

import android.app.AlertDialog;
import android.content.Context;

import androidx.preference.ListPreference;

/**
 * Shows a ListPreference with the platform dialog. The dialog of androidx
 * preference is an AppCompat one and cannot open under Theme.Main, which
 * derives from Theme.DeviceDefault.Settings.
 */
final class ListPreferenceDialog {

    private ListPreferenceDialog() {
    }

    static void show(Context context, ListPreference preference) {
        final CharSequence[] entries = preference.getEntries();
        final CharSequence[] values = preference.getEntryValues();
        if (entries == null || values == null) {
            return;
        }
        int checked = preference.findIndexOfValue(preference.getValue());
        new AlertDialog.Builder(context)
                .setTitle(preference.getDialogTitle())
                .setSingleChoiceItems(entries, checked, (dialog, which) -> {
                    dialog.dismiss();
                    String value = values[which].toString();
                    if (preference.callChangeListener(value)) {
                        preference.setValue(value);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
