/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lumia.parts;

import android.content.ComponentName;
import android.content.Intent;
import android.service.quicksettings.TileService;

import androidx.fragment.app.Fragment;

/**
 * Main page: launcher icon, Settings > System > Lumia settings and the long
 * press on the tiles (the performance tile opens its own page).
 */
public class LumiaSettingsActivity extends SettingsActivity {

    @Override
    protected boolean redirect() {
        Intent intent = getIntent();
        if (intent == null
                || !TileService.ACTION_QS_TILE_PREFERENCES.equals(intent.getAction())) {
            return false;
        }
        ComponentName tile = intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME);
        if (tile == null
                || !PerformanceProfileTileService.class.getName().equals(tile.getClassName())) {
            return false;
        }
        startActivity(new Intent(this, PerformanceProfileActivity.class));
        return true;
    }

    @Override
    protected Fragment createFragment() {
        return new LumiaSettingsFragment();
    }

    @Override
    protected boolean showUpArrow() {
        // Not from the launcher: back to Settings or Quick Settings
        Intent intent = getIntent();
        return intent == null || !Intent.ACTION_MAIN.equals(intent.getAction());
    }
}
