/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lumia.keyhandler;

import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.KeyguardManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManagerGlobal;

import com.android.internal.os.BackgroundThread;
import com.android.internal.os.DeviceKeyHandler;

/**
 * The two-stage camera button of the MSM8974 Lumias (PM8941 GPIOs 3 and 4).
 *
 * With a camera app in front the key events go to it, so half press focuses
 * and full press takes the photo. Otherwise the button runs the actions
 * chosen in Lumia settings. With the screen off only a long press counts,
 * so the button does not fire in a pocket.
 *
 * PhoneWindowManager calls this twice for a key it passes on (before
 * queueing and before dispatching): the down time tells the calls apart.
 * Runs on the input threads, so the actions go to the background thread.
 */
public final class KeyHandler implements DeviceKeyHandler {

    private static final String TAG = "LumiaKeyHandler";

    /* Linux KEY_CAMERA_FOCUS and KEY_CAMERA_SNAPSHOT */
    private static final int SCAN_FOCUS = 0x210;
    private static final int SCAN_SHUTTER = 0x2fe;

    private static final long LONG_PRESS_MS = 500;
    private static final long LONG_PRESS_SCREEN_OFF_MS = 800;
    private static final long WAKE_LOCK_MS = 3000;

    private static final class Press {
        long downTime = -1;
        boolean held;
        boolean passed;
    }

    private final Context mContext;
    private final Handler mHandler = BackgroundThread.getHandler();
    private final Object mLock = new Object();
    private final Press mFocus = new Press();
    private final Press mShutter = new Press();
    private final Runnable mLongPress = this::onLongPress;

    private boolean mPassToApp;
    private boolean mInteractive;
    private boolean mLongFired;

    private PowerManager.WakeLock mWakeLock;
    private CameraManager mCameraManager;
    private boolean mTorchCallbackRegistered;
    private String mTorchCameraId;
    private volatile boolean mTorchOn;

    private final CameraManager.TorchCallback mTorchCallback =
            new CameraManager.TorchCallback() {
        @Override
        public void onTorchModeChanged(String cameraId, boolean enabled) {
            if (cameraId.equals(mTorchCameraId)) {
                mTorchOn = enabled;
            }
        }

        @Override
        public void onTorchModeUnavailable(String cameraId) {
            if (cameraId.equals(mTorchCameraId)) {
                mTorchOn = false;
            }
        }
    };

    public KeyHandler(Context context) {
        mContext = context;
    }

    @Override
    public KeyEvent handleKeyEvent(KeyEvent event) {
        final int scanCode = event.getScanCode();
        if (scanCode != SCAN_FOCUS && scanCode != SCAN_SHUTTER) {
            return event;
        }
        final boolean shutter = scanCode == SCAN_SHUTTER;
        final Press press = shutter ? mShutter : mFocus;
        switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN:
                return onDown(event, press, shutter);
            case KeyEvent.ACTION_UP:
                return onUp(event, press, shutter);
            default:
                return event;
        }
    }

    private KeyEvent onDown(KeyEvent event, Press press, boolean shutter) {
        final long downTime = event.getDownTime();
        final boolean first;
        synchronized (mLock) {
            if (press.downTime == downTime) {
                return press.passed ? event : null;
            }
            if (event.getRepeatCount() > 0) {
                return event;
            }
            first = !mFocus.held && !mShutter.held;
        }

        // Outside the lock: these call into the power and activity services
        boolean interactive = false;
        boolean passToApp = false;
        if (first) {
            interactive = mContext.getSystemService(PowerManager.class).isInteractive();
            passToApp = interactive && isCameraInFront();
            if (!interactive) {
                // The timers below do not run while the phone is suspended
                acquireWakeLock();
            }
            if (!passToApp) {
                mHandler.post(this::registerTorchCallback);
            }
        }

        synchronized (mLock) {
            if (first) {
                mInteractive = interactive;
                mPassToApp = passToApp;
                mLongFired = false;
            }
            press.downTime = downTime;
            press.held = true;
            press.passed = mPassToApp;
            if (shutter && !mPassToApp) {
                mHandler.removeCallbacks(mLongPress);
                mHandler.postDelayed(mLongPress,
                        mInteractive ? LONG_PRESS_MS : LONG_PRESS_SCREEN_OFF_MS);
            }
            return mPassToApp ? event : null;
        }
    }

    private KeyEvent onUp(KeyEvent event, Press press, boolean shutter) {
        final boolean runShort;
        final boolean ended;
        final boolean passed;
        synchronized (mLock) {
            // Not matched on the down time: the input reader keeps one per
            // device, so the focus key's up carries the shutter's down time.
            if (press.downTime < 0) {
                return event;
            }
            passed = press.passed;
            if (!press.held) {
                return passed ? event : null;
            }
            press.held = false;
            runShort = shutter && !passed && mInteractive && !mLongFired && !event.isCanceled();
            if (shutter && !passed) {
                mHandler.removeCallbacks(mLongPress);
            }
            ended = !mFocus.held && !mShutter.held;
        }
        if (runShort) {
            mHandler.post(() -> perform(CameraKeyConfig.getShort(mContext.getContentResolver())));
        }
        if (ended) {
            mHandler.post(this::releaseWakeLock);
        }
        return passed ? event : null;
    }

    private void onLongPress() {
        final boolean interactive;
        synchronized (mLock) {
            if (!mShutter.held || mPassToApp) {
                return;
            }
            interactive = mInteractive;
        }
        final ContentResolver resolver = mContext.getContentResolver();
        String action = CameraKeyConfig.getLong(resolver);
        final boolean noLongAction = CameraKeyConfig.ACTION_NONE.equals(action);
        if (interactive) {
            if (noLongAction) {
                // Nothing on hold: releasing the button still counts as a press
                return;
            }
        } else {
            if (!CameraKeyConfig.isScreenOffEnabled(resolver)) {
                return;
            }
            if (noLongAction) {
                action = CameraKeyConfig.getShort(resolver);
            }
        }
        synchronized (mLock) {
            if (!mShutter.held) {
                return;
            }
            mLongFired = true;
        }
        perform(action);
    }

    private void perform(String action) {
        if (CameraKeyConfig.ACTION_CAMERA.equals(action)) {
            launchCamera();
        } else if (CameraKeyConfig.ACTION_TORCH.equals(action)) {
            toggleTorch();
        } else if (CameraKeyConfig.isApp(action)) {
            launchApp(CameraKeyConfig.appComponent(action));
        }
    }

    /** A camera app is on screen, not just the last app behind the lock screen. */
    private boolean isCameraInFront() {
        try {
            ActivityManager.StackInfo stack = ActivityTaskManager.getService()
                    .getFocusedStackInfo();
            if (stack == null || stack.topActivity == null) {
                return false;
            }
            final String packageName = stack.topActivity.getPackageName();
            final int userId = ActivityManager.getCurrentUser();
            final PackageManager pm = mContext.getPackageManager();
            if (!handles(pm, MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA, packageName, userId)
                    && !handles(pm, MediaStore.ACTION_IMAGE_CAPTURE, packageName, userId)) {
                return false;
            }
            final int uid = pm.getPackageUidAsUser(packageName, userId);
            return ActivityManager.getService().getUidProcessState(uid,
                    mContext.getOpPackageName()) == ActivityManager.PROCESS_STATE_TOP;
        } catch (RemoteException | PackageManager.NameNotFoundException | SecurityException e) {
            Log.w(TAG, "Could not read the app in front", e);
            return false;
        }
    }

    private static boolean handles(PackageManager pm, String action, String packageName,
            int userId) {
        return !pm.queryIntentActivitiesAsUser(new Intent(action).setPackage(packageName), 0,
                userId).isEmpty();
    }

    private boolean isLocked() {
        return mContext.getSystemService(KeyguardManager.class).isKeyguardLocked();
    }

    private void wakeUp() {
        mContext.getSystemService(PowerManager.class).wakeUp(SystemClock.uptimeMillis(),
                PowerManager.WAKE_REASON_CAMERA_LAUNCH, "org.lineageos.lumia:CAMERA_KEY");
    }

    private void launchCamera() {
        final Intent intent = new Intent(isLocked()
                ? MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE
                : MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        wakeUp();
        start(intent);
    }

    private void launchApp(ComponentName component) {
        if (component == null) {
            return;
        }
        final Intent intent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(component)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        final boolean locked = isLocked();
        wakeUp();
        if (start(intent) && locked) {
            try {
                // The app shows up once the phone is unlocked
                WindowManagerGlobal.getWindowManagerService().dismissKeyguard(null, null);
            } catch (RemoteException e) {
                Log.w(TAG, "Could not dismiss the lock screen", e);
            }
        }
    }

    private boolean start(Intent intent) {
        try {
            mContext.startActivityAsUser(intent, UserHandle.CURRENT);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            Log.w(TAG, "Could not start " + intent, e);
            return false;
        }
    }

    private void registerTorchCallback() {
        if (mTorchCallbackRegistered) {
            return;
        }
        final String cameraId = torchCameraId();
        if (cameraId == null) {
            return;
        }
        mCameraManager.registerTorchCallback(mTorchCallback, mHandler);
        mTorchCallbackRegistered = true;
    }

    private String torchCameraId() {
        if (mTorchCameraId != null) {
            return mTorchCameraId;
        }
        if (mCameraManager == null) {
            mCameraManager = mContext.getSystemService(CameraManager.class);
        }
        try {
            for (String id : mCameraManager.getCameraIdList()) {
                CameraCharacteristics c = mCameraManager.getCameraCharacteristics(id);
                Boolean flash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing = c.get(CameraCharacteristics.LENS_FACING);
                if (Boolean.TRUE.equals(flash) && facing != null
                        && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    mTorchCameraId = id;
                    return id;
                }
            }
        } catch (CameraAccessException | IllegalArgumentException e) {
            Log.w(TAG, "Could not find the flash", e);
        }
        return null;
    }

    private void toggleTorch() {
        registerTorchCallback();
        final String cameraId = torchCameraId();
        if (cameraId == null) {
            return;
        }
        try {
            mCameraManager.setTorchMode(cameraId, !mTorchOn);
        } catch (CameraAccessException | IllegalArgumentException e) {
            // In use by a camera app
            Log.w(TAG, "Could not switch the torch", e);
        }
    }

    private void acquireWakeLock() {
        synchronized (mLock) {
            if (mWakeLock == null) {
                mWakeLock = mContext.getSystemService(PowerManager.class)
                        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
                mWakeLock.setReferenceCounted(false);
            }
            mWakeLock.acquire(WAKE_LOCK_MS);
        }
    }

    private void releaseWakeLock() {
        synchronized (mLock) {
            if (mWakeLock != null && mWakeLock.isHeld() && !mFocus.held && !mShutter.held) {
                mWakeLock.release();
            }
        }
    }
}
