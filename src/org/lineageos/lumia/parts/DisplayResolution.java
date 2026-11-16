/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lumia.parts;

import android.graphics.Point;
import android.net.Uri;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;

/**
 * Smoother graphics: apps draw at two thirds of the panel resolution (720p
 * on the 1080p panels) and the display hardware scales the frames up. On
 * the Adreno 330 the notification shade at 1080p needs about 20 ms of GPU
 * per frame and runs at 30 fps; at 720p it takes a few ms. Same calls as
 * "wm size" and "wm density": WindowManagerService keeps the forced size
 * and density in Settings, so the choice survives a restart.
 */
final class DisplayResolution {

    private static final String TAG = "LumiaParts";

    static final Uri SIZE_URI = Settings.Global.getUriFor(Settings.Global.DISPLAY_SIZE_FORCED);

    private DisplayResolution() {}

    private static IWindowManager wm() {
        return WindowManagerGlobal.getWindowManagerService();
    }

    private static Point initialSize() throws RemoteException {
        Point size = new Point();
        wm().getInitialDisplaySize(Display.DEFAULT_DISPLAY, size);
        return size;
    }

    /** Only on panels that are at least 1080 pixels wide. */
    static boolean isSupported() {
        try {
            Point size = initialSize();
            return Math.min(size.x, size.y) >= 1080;
        } catch (RemoteException e) {
            return false;
        }
    }

    static boolean isLowResolution() {
        try {
            Point initial = initialSize();
            Point base = new Point();
            wm().getBaseDisplaySize(Display.DEFAULT_DISPLAY, base);
            return base.x < initial.x || base.y < initial.y;
        } catch (RemoteException e) {
            return false;
        }
    }

    static void setLowResolution(boolean low) {
        try {
            IWindowManager wm = wm();
            if (low) {
                Point size = initialSize();
                int density = wm.getInitialDisplayDensity(Display.DEFAULT_DISPLAY);
                wm.setForcedDisplaySize(Display.DEFAULT_DISPLAY,
                        size.x * 2 / 3, size.y * 2 / 3);
                wm.setForcedDisplayDensityForUser(Display.DEFAULT_DISPLAY,
                        density * 2 / 3, UserHandle.USER_CURRENT);
            } else {
                wm.clearForcedDisplaySize(Display.DEFAULT_DISPLAY);
                wm.clearForcedDisplayDensityForUser(Display.DEFAULT_DISPLAY,
                        UserHandle.USER_CURRENT);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot change the display resolution", e);
        }
    }
}
