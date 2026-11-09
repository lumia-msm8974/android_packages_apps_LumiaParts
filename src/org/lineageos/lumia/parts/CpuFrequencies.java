/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lumia.parts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * CPU frequencies of the profiles, for the descriptions only. The device's
 * perf lock library turns the profile requests of the power HAL
 * (power-8974.c) into cpufreq limits: a request of VV x 100 MHz is rounded
 * up to the next available frequency. All four cores share the same table.
 */
final class CpuFrequencies {

    private static final String CPUFREQ = "/sys/devices/system/cpu/cpu0/cpufreq/";

    /* power-8974.c: NONTURBO_MAX = 0x0A (1.0 GHz); bias performance asks for 0x0B */
    static final int REQUEST_NONTURBO_MAX = 10;
    static final int REQUEST_NONTURBO_MAX_PLUS_ONE = 11;

    private static List<Integer> sFrequencies;
    private static int sMax;

    private CpuFrequencies() {}

    private static synchronized void load() {
        if (sFrequencies != null) {
            return;
        }
        sFrequencies = new ArrayList<>();
        String line = readLine(CPUFREQ + "scaling_available_frequencies");
        if (line != null) {
            for (String token : line.trim().split("\\s+")) {
                try {
                    sFrequencies.add(Integer.parseInt(token));
                } catch (NumberFormatException e) {
                    // skip
                }
            }
        }
        String max = readLine(CPUFREQ + "cpuinfo_max_freq");
        try {
            sMax = max != null ? Integer.parseInt(max.trim()) : 0;
        } catch (NumberFormatException e) {
            sMax = 0;
        }
        if (sMax == 0 && !sFrequencies.isEmpty()) {
            sMax = sFrequencies.get(sFrequencies.size() - 1);
        }
    }

    private static String readLine(String path) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            return reader.readLine();
        } catch (IOException | SecurityException e) {
            return null;
        }
    }

    /** Frequency in kHz chosen for a request of value x 100 MHz, 0 if unknown. */
    static int forRequest(int value) {
        load();
        int khz = value * 100000;
        for (int freq : sFrequencies) {
            if (freq >= khz) {
                return freq;
            }
        }
        return sMax;
    }

    /** Highest frequency in kHz, 0 if unknown. */
    static int max() {
        load();
        return sMax;
    }

    /** "1.04 GHz", or null when the frequency is unknown. */
    static String format(Locale locale, int khz) {
        if (khz <= 0) {
            return null;
        }
        return String.format(locale, "%.2f GHz", khz / 1000000.0);
    }
}
