/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lumia.parts;

import androidx.fragment.app.Fragment;

/** Performance profile page, from the main page or a long press on the tile. */
public class PerformanceProfileActivity extends SettingsActivity {

    @Override
    protected Fragment createFragment() {
        return new PerformanceProfileFragment();
    }
}
