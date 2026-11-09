/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lumia.parts;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Quick Settings tile for double tap to wake; a long press opens the main page. */
public class DoubleTapTileService extends TileService implements SettingsObserver.Listener {

    private SettingsObserver mObserver;

    @Override
    public void onCreate() {
        super.onCreate();
        mObserver = new SettingsObserver(this, this, DoubleTapToWake.SETTING_URI);
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
        if (!DoubleTapToWake.isSupported(this)) {
            return;
        }
        DoubleTapToWake.setEnabled(this, !DoubleTapToWake.isEnabled(this));
        update();
    }

    private void update() {
        Tile tile = getQsTile();
        if (tile == null) {
            return;
        }
        if (!DoubleTapToWake.isSupported(this)) {
            tile.setState(Tile.STATE_UNAVAILABLE);
        } else {
            boolean on = DoubleTapToWake.isEnabled(this);
            tile.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
            tile.setSubtitle(getString(on ? R.string.tile_on : R.string.tile_off));
        }
        tile.updateTile();
    }
}
