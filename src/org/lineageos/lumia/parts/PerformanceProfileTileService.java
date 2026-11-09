/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lumia.parts;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import lineageos.power.PerformanceManager;

/**
 * Quick Settings tile: a tap moves to the next profile, lowest power first;
 * a long press opens the profile page. While Battery Saver is on the tile
 * shows Power save and does nothing, so that a tap never turns Battery
 * Saver off.
 */
public class PerformanceProfileTileService extends TileService
        implements SettingsObserver.Listener {

    private SettingsObserver mObserver;

    @Override
    public void onCreate() {
        super.onCreate();
        mObserver = new SettingsObserver(this, this, PerformanceProfiles.SETTING_URI);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        mObserver.register();
        update();
    }

    @Override
    public void onStopListening() {
        mObserver.unregister();
        super.onStopListening();
    }

    @Override
    public void onSettingsChanged() {
        update();
    }

    @Override
    public void onClick() {
        super.onClick();
        if (!PerformanceProfiles.isSupported(this)
                || PerformanceProfiles.isForcedByBatterySaver(this)) {
            return;
        }
        PerformanceProfiles.set(this,
                PerformanceProfiles.next(this, PerformanceProfiles.get(this)));
        update();
    }

    private void update() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        if (!PerformanceProfiles.isSupported(this)) {
            tile.setState(Tile.STATE_UNAVAILABLE);
            tile.setSubtitle(null);
        } else if (PerformanceProfiles.isForcedByBatterySaver(this)) {
            tile.setState(Tile.STATE_UNAVAILABLE);
            tile.setSubtitle(PerformanceProfiles.summary(this));
        } else {
            int profile = PerformanceProfiles.get(this);
            // Highlighted when the profile is not the default one
            tile.setState(profile == PerformanceManager.PROFILE_BALANCED
                    ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
            tile.setSubtitle(PerformanceProfiles.name(this, profile));
        }
        tile.updateTile();
    }
}
