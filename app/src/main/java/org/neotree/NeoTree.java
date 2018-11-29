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

package org.neotree;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.StrictMode;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.neotree.inject.ApplicationComponent;
import org.neotree.inject.DaggerApplicationComponent;
import org.neotree.inject.module.ApplicationModule;
import org.neotree.support.android.AndroidHelper;
import org.neotree.support.realm.EncryptionKeyStore;
import org.neotree.support.realm.NeoTreeRealmMigration;

import io.fabric.sdk.android.Fabric;
import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by matteo on 14/07/2016.
 */
public class NeoTree extends MultiDexApplication {

    private static final String TAG = NeoTree.class.getSimpleName();

    public static final String CONFIG_KEYS = "configkeys";
    public static final String SCREENS = "screens";
    public static final String SCRIPTS = "scripts";
    public static final String ORDER_KEY = "position";

    public static final String EXTRA_PREFIX = "org.neotree.extra.";
    public static final String EXTRA_CONFIDENTIAL = EXTRA_PREFIX + "confidential";
    public static final String EXTRA_EXPORT_MODE = EXTRA_PREFIX + "_export_mode";
    public static final String EXTRA_SCRIPT = EXTRA_PREFIX + "_script";
    public static final String EXTRA_SCRIPT_ID = EXTRA_PREFIX + "_script_id";
    public static final String EXTRA_SESSION_ID = EXTRA_PREFIX + "_session_id";

    public static final String EXTRA_SCREEN = EXTRA_PREFIX + "screen";

    private ApplicationComponent mApplicationComponent;
    private RealmConfiguration mRealmConfiguration;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.v(TAG, String.format("Launching \"%s\", Version \"%s\" (%d)",
                getString(R.string.app_name), BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

//        // Crashlytics
//        Fabric.with(this, new Crashlytics());
        if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            // only for gingerbread and newer versions
            MultiDex.install(this);
        }

        // Joda Time
        JodaTimeAndroid.init(this);

        // Firebase
//        FirebaseApp.initializeApp(this);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        if (BuildConfig.DEBUG) {
            configureDevTools();
        }
        configureDependencyInjector();
        configureDatastore();
    }

    public ApplicationComponent getComponent() {
        return mApplicationComponent;
    }

    public static SharedPreferences getConfigurationPreferences(Context context) {
        return context.getSharedPreferences(
                context.getString(R.string.shared_prefs_script_configuration),
                Context.MODE_PRIVATE);
    }

    //private void configureDevTools() {
//        Log.v(TAG, "Configuring strict mode for development...");
//
//        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//                .detectDiskReads()
//                .detectDiskWrites()
//                .detectAll()
//                .penaltyLog()
//                .build());
//
//        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//                .detectLeakedClosableObjects()
//                .penaltyLog()
//                .build());
   // }
    private void configureDevTools() {
        Log.v(TAG, "Configuring strict mode for development...");

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectAll()
                .penaltyLog()
                .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build());

        Log.v(TAG, "Configuring Stetho...");


    }

    private void configureDependencyInjector() {
        Log.v(TAG, "Configuring dependency injector...");
        mApplicationComponent = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .build();
    }

    private void configureDatastore() {
        Realm.init(this);

        byte[] encryptionKey = EncryptionKeyStore.generateOrGetRealmEncryptionKey(this);
        AndroidHelper.bytesToHex(encryptionKey);

        mRealmConfiguration = new RealmConfiguration.Builder()
                .name("neotree.realm")
                .encryptionKey(encryptionKey)
                .schemaVersion(getResources().getInteger(R.integer.realm_schema_version))
                .migration(new NeoTreeRealmMigration())
                .build();
        Realm.setDefaultConfiguration(mRealmConfiguration);
    }

    public RealmConfiguration getRealmConfiguration() {
        return mRealmConfiguration;
    }

    public static class NeoTreeFormat {

        public static final DateTimeFormatter DATETIME = DateTimeFormat.forPattern("dd MMM yyyy HH:mm");

        public static final DateTimeFormatter DATETIME_EXPORT = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm");

        public static final DateTimeFormatter DATE = DateTimeFormat.forPattern("dd MMM yyyy");

        public static final DateTimeFormatter DATE_EXPORT = DateTimeFormat.forPattern("dd/MM/yyyy");

        public static final DateTimeFormatter TIME = DateTimeFormat.forPattern("HH:mm");

        public static final PeriodFormatter PERIOD = new PeriodFormatterBuilder()
                .appendYears()
                .appendSuffix(" year", " years")
                .appendMonths()
                .appendSuffix(" month", " months")
                .appendDays()
                .appendSuffix(" day", " days")
                .appendSeparator(", ")
                .appendHours()
                .appendSuffix(" hour", " hours")
                .appendSeparator(", ")
                .toFormatter();
    }

}
