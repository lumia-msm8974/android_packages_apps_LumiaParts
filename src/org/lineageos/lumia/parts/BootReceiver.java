/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lumia.parts;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * Hides the Quick Settings tiles of features this build does not have (no
 * profiles from the power HAL, no double tap to wake in the overlay), so
 * they are not offered in the tile editor. The settings services are all
 * up at boot completed.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        setEnabled(context, PerformanceProfileTileService.class,
                PerformanceProfiles.isSupported(context));
        setEnabled(context, DoubleTapTileService.class, DoubleTapToWake.isSupported(context));
    }

    private static void setEnabled(Context context, Class<?> component, boolean enabled) {
        context.getPackageManager().setComponentEnabledSetting(
                new ComponentName(context, component),
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
