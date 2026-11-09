/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lumia.parts;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic summary of the Settings > System entry: Settings calls
 * "getDynamicSummary" on the com.android.settings.summary_uri of the tile
 * and refreshes it when the URI changes.
 */
public class SummaryProvider extends ContentProvider {

    private static final String AUTHORITY = "org.lineageos.lumia.parts.summary";
    private static final String METHOD_GET_DYNAMIC_SUMMARY = "getDynamicSummary";
    private static final String KEY_SUMMARY = "com.android.settings.summary";

    static final Uri SUMMARY_URI =
            Uri.parse("content://" + AUTHORITY + "/" + METHOD_GET_DYNAMIC_SUMMARY);

    static void notifyChange(Context context) {
        context.getContentResolver().notifyChange(SUMMARY_URI, null);
    }

    static String summary(Context context) {
        List<String> parts = new ArrayList<>();
        if (PerformanceProfiles.isSupported(context)) {
            parts.add(context.getString(R.string.summary_profile,
                    PerformanceProfiles.summary(context)));
        }
        if (DoubleTapToWake.isSupported(context)) {
            parts.add(context.getString(DoubleTapToWake.isEnabled(context)
                    ? R.string.summary_double_tap_on : R.string.summary_double_tap_off));
        }
        if (parts.isEmpty()) {
            return context.getString(R.string.summary_default);
        }
        return String.join(" · ", parts);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String uri, Bundle extras) {
        if (!METHOD_GET_DYNAMIC_SUMMARY.equals(method)) {
            return null;
        }
        Bundle bundle = new Bundle();
        bundle.putString(KEY_SUMMARY, summary(getContext()));
        return bundle;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
