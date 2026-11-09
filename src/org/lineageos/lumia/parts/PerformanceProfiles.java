/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lumia.parts;

import static lineageos.power.PerformanceManager.PROFILE_BALANCED;
import static lineageos.power.PerformanceManager.PROFILE_BIAS_PERFORMANCE;
import static lineageos.power.PerformanceManager.PROFILE_BIAS_POWER_SAVE;
import static lineageos.power.PerformanceManager.PROFILE_HIGH_PERFORMANCE;
import static lineageos.power.PerformanceManager.PROFILE_POWER_SAVE;

import android.content.Context;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lineageos.power.PerformanceManager;
import lineageos.providers.LineageSettings;

/**
 * The LineageOS performance profiles. PerformanceManagerService (lineage-sdk)
 * stores the choice in LineageSettings.Secure.PERFORMANCE_PROFILE and sends
 * it to the power HAL (android.hardware.power-service-qti, power-8974.c,
 * vendor.lineage.power SET_PROFILE), which asks the device's perf lock
 * library for the CPU limits. Nothing is written to sysfs here.
 */
final class PerformanceProfiles {

    private static final String TAG = "LumiaParts";

    /* Lowest to highest power, as in lineage-sdk perf_profile_values */
    private static final int[] ORDER = {
        PROFILE_POWER_SAVE,
        PROFILE_BIAS_POWER_SAVE,
        PROFILE_BALANCED,
        PROFILE_BIAS_PERFORMANCE,
        PROFILE_HIGH_PERFORMANCE,
    };

    static final Uri SETTING_URI =
            LineageSettings.Secure.getUriFor(LineageSettings.Secure.PERFORMANCE_PROFILE);

    private PerformanceProfiles() {}

    private static PerformanceManager manager(Context context) {
        return PerformanceManager.getInstance(context.getApplicationContext());
    }

    /** Profiles offered by the power HAL, lowest power first. */
    static List<Integer> available(Context context) {
        List<Integer> profiles = new ArrayList<>();
        PerformanceManager pm = manager(context);
        if (pm.getNumberOfProfiles() <= 0) {
            return profiles;
        }
        for (int id : ORDER) {
            if (pm.getPowerProfile(id) != null) {
                profiles.add(id);
            }
        }
        return profiles;
    }

    static boolean isSupported(Context context) {
        return !available(context).isEmpty();
    }

    /** The profile chosen by the user (Battery Saver may override it). */
    static int get(Context context) {
        int profile = manager(context).getPowerProfile();
        return profile >= 0 ? profile : PROFILE_BALANCED;
    }

    /**
     * Applies and stores a profile. Leaving Power save while Battery Saver is
     * on turns Battery Saver off (PerformanceManagerService does that).
     */
    static void set(Context context, int profile) {
        try {
            manager(context).setPowerProfile(profile);
        } catch (IllegalArgumentException | SecurityException e) {
            Log.e(TAG, "Cannot set performance profile " + profile, e);
        }
        SummaryProvider.notifyChange(context);
    }

    static int next(Context context, int profile) {
        List<Integer> profiles = available(context);
        if (profiles.isEmpty()) {
            return profile;
        }
        int index = profiles.indexOf(profile);
        return profiles.get((index + 1) % profiles.size());
    }

    /** Battery Saver makes PerformanceManagerService use Power save. */
    static boolean isForcedByBatterySaver(Context context) {
        PowerManager pm = context.getSystemService(PowerManager.class);
        return pm != null && pm.isPowerSaveMode();
    }

    /** The profile the system is using now. */
    static int active(Context context) {
        return isForcedByBatterySaver(context) ? PROFILE_POWER_SAVE : get(context);
    }

    static String name(Context context, int profile) {
        switch (profile) {
            case PROFILE_POWER_SAVE:
                return context.getString(R.string.profile_power_save);
            case PROFILE_BIAS_POWER_SAVE:
                return context.getString(R.string.profile_efficiency);
            case PROFILE_BIAS_PERFORMANCE:
                return context.getString(R.string.profile_quick);
            case PROFILE_HIGH_PERFORMANCE:
                return context.getString(R.string.profile_performance);
            default:
                return context.getString(R.string.profile_balanced);
        }
    }

    /** What the profile does on these phones, with the frequencies of this CPU. */
    static String description(Context context, int profile) {
        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        switch (profile) {
            case PROFILE_POWER_SAVE:
            case PROFILE_BIAS_POWER_SAVE: {
                String cap = CpuFrequencies.format(locale,
                        CpuFrequencies.forRequest(CpuFrequencies.REQUEST_NONTURBO_MAX));
                if (cap == null) {
                    cap = CpuFrequencies.format(locale, 1000000);
                }
                return context.getString(profile == PROFILE_POWER_SAVE
                        ? R.string.profile_power_save_summary
                        : R.string.profile_efficiency_summary, cap);
            }
            case PROFILE_BIAS_PERFORMANCE: {
                String floor = CpuFrequencies.format(locale,
                        CpuFrequencies.forRequest(CpuFrequencies.REQUEST_NONTURBO_MAX_PLUS_ONE));
                if (floor == null) {
                    floor = CpuFrequencies.format(locale, 1100000);
                }
                return context.getString(R.string.profile_quick_summary, floor);
            }
            case PROFILE_HIGH_PERFORMANCE: {
                String max = CpuFrequencies.format(locale, CpuFrequencies.max());
                return max != null
                        ? context.getString(R.string.profile_performance_summary, max)
                        : context.getString(R.string.profile_performance_summary_no_freq);
            }
            default:
                return context.getString(R.string.profile_balanced_summary);
        }
    }

    /** One line for the Settings entry, the tile and the main page. */
    static String summary(Context context) {
        if (isForcedByBatterySaver(context)) {
            return context.getString(R.string.profile_battery_saver_summary,
                    name(context, PROFILE_POWER_SAVE));
        }
        return name(context, get(context));
    }
}
