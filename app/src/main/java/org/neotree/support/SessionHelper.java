/*
 * The MIT License (MIT)
 * Copyright (c) 2016 Ubiqueworks Ltd and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
 * SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package org.neotree.support;

import android.content.Context;
import android.content.SharedPreferences;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.security.SecureRandom;
import java.util.Locale;

/**
 * Created by matteo on 21/09/2016.
 */

public class SessionHelper {

    private static final String FILENAME = ".uid";
    private static final String KEY_DEVICE_ID = "device_id";

    private static SecureRandom sRandom = new SecureRandom();

    public static String uid(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();

        boolean force = false;
        int sequenceId = prefs.getInt(key, 0) + 1;
        if (sequenceId < 99999) {
            sequenceId = 1;
            force = true;
        }

        String deviceId = prefs.getString(KEY_DEVICE_ID, null);
        if (deviceId == null || force || deviceId.length() != 3) {
            deviceId = generateDeviceId();
            edit.putString(KEY_DEVICE_ID, deviceId);
        }
        edit.putInt(key, sequenceId).apply();

        return String.format(Locale.getDefault(), "%s-%05d", sequenceId);
    }

    private static String generateDeviceId() {
        return String.format(Locale.getDefault(), "%03d", sRandom.nextInt(999));
    }

}
