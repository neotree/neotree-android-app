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
import android.provider.Settings;
import android.util.Log;
import org.neotree.support.datastore.FirebaseStore;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

import static android.provider.Settings.Secure.ANDROID_ID;
import static org.neotree.ui.activity.MainActivity.getStringToken;
/**
 * Created by matteo on 21/09/2016.
 */

public class SessionHelper {
    private static String DEVICEHASH = "devicehash";
    public  CompositeSubscription mSubscription;
    private static final String FILENAME = ".uid";
    private static final String KEY_DEVICE_ID = "device_id";
    private static String SERVER_SESSIONID = "serversessionid";

    private static String uniqueID = null;

    private static String newDeviceHash = null;

    private static String devHash = null;

    private static String neoID = null;

    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";

    private static SecureRandom sRandom = new SecureRandom();

    private static String ALPHABET = "AC2345679"; // "ACDEFGHIJKLMNPQRSTUVWXYZ12345679";

    public static String uid(Context context, String key) {

        SharedPreferences prefs = context.getSharedPreferences(FILENAME, Context.MODE_PRIVATE);

        SharedPreferences.Editor edit = prefs.edit();
        int incrementPart = 0;
        String deviceHash =  prefs.getString(DEVICEHASH,null);
        String cdeviceHash =  prefs.getString(DEVICEHASH,null);
        boolean force = false;
        int sharedPrefIncrementValue = prefs.getInt(SERVER_SESSIONID,0 );
        if( sharedPrefIncrementValue== 0){
            incrementPart = 1;
            edit.putInt(SERVER_SESSIONID,incrementPart);
        }else{
            if(sharedPrefIncrementValue >9999){
                incrementPart = 1;
                edit.putInt(SERVER_SESSIONID,incrementPart);
            }else{
                incrementPart = sharedPrefIncrementValue +1;
                edit.putInt(SERVER_SESSIONID,incrementPart);

            }
        }
        edit.commit();
        String incrementId = String.format(Locale.getDefault(), "%04d", incrementPart);
        String deviceId = prefs.getString(KEY_DEVICE_ID, null);
        if (deviceId == null || force || deviceId.length() != 3) {
            deviceId = generateDeviceId();
            edit.putString(KEY_DEVICE_ID, deviceId);
        }

        //uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
        //clean the hash, D8B0O
        //does the current hash contain unwanted characters, if yes regenerate and store value on
        String forbidden = "dDeEFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz018";
        Boolean deviceHashContainsForbidden = false;
        for (int i = 0; i < forbidden.length(); i++) {
            if (deviceHash.contains(Character.toString(forbidden.charAt(i)))) deviceHashContainsForbidden = true;
        }
        if (deviceHashContainsForbidden) {
            newDeviceHash = generateString(4);

            deviceHash = newDeviceHash;

            SharedPreferences.Editor ed = prefs.edit();
            ed.remove(DEVICEHASH).commit(); //remove old
            ed.putString(DEVICEHASH, newDeviceHash.toString() ).commit();

            //update firebase
            FirebaseStore fiStore = new FirebaseStore();
            fiStore.adddeviceScriptDeviceHash(newDeviceHash.toString(), Settings.Secure.getString(context.getContentResolver(),
                    ANDROID_ID));
            neoID = " contains, generate ";
        }else{
            neoID = " not contain";
        }

        devHash = prefs.getString(DEVICEHASH,null);

        String diddeviceHash =  getStringToken(deviceId);

        String neotreeId = deviceHash.toUpperCase()+" - "+incrementId;
        //String neotreeId = generateString(4).toUpperCase();
        FirebaseStore firreStore = new FirebaseStore();
        firreStore.adddeviceScriptIncrementId(incrementId.toString(),Settings.Secure.getString(context.getContentResolver(),
                ANDROID_ID));
        return neotreeId;
    }

    private static String generateString(int length) {
        Random random = new Random();
        StringBuilder builder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }

    private static String generateDeviceId() {
        return String.format(Locale.getDefault(), "%03d", sRandom.nextInt(999));
    }


    public  void addSubscription(Subscription subscription) {
        if (mSubscription == null) {
            mSubscription = new CompositeSubscription();
        }
        mSubscription.add(subscription);
    }
}