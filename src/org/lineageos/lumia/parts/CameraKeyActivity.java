/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lumia.parts;

import androidx.fragment.app.Fragment;

/** Camera button page, from the main page. */
public class CameraKeyActivity extends SettingsActivity {

    @Override
    protected Fragment createFragment() {
        return new CameraKeyFragment();
    }
}
