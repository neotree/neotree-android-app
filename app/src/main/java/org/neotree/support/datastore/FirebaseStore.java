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

package org.neotree.support.datastore;

import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;

import org.neotree.model.firebase.AdminPassword;
import org.neotree.model.firebase.ConfigKey;
import org.neotree.model.firebase.Diagnosis;
import org.neotree.model.firebase.Screen;
import org.neotree.model.firebase.Script;
import org.neotree.support.rx.RxFirebase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by matteo on 21/08/2016.
 */
public class FirebaseStore {

    private static final String TAG = FirebaseStore.class.getSimpleName();

    private static final String PATH_SEPARATOR = "/";

    private static final String ADMIN_PASSWORD = "adminpassword";
    private static final String CONFIG_KEYS = "configkeys";
    private static final String DIAGNOSIS = "diagnosis";
    private static final String SCREEENS = "screens";
    private static final String SCRIPTS = "scripts";

    private static final String ORDER_BY_FIELD = "position";

    public static final GenericTypeIndicator TYPE_ADMIN_PASSWORD = new GenericTypeIndicator<AdminPassword>() {};
    public static final GenericTypeIndicator TYPE_CONFIG_KEY = new GenericTypeIndicator<ConfigKey>() {};
    public static final GenericTypeIndicator TYPE_CONFIG_KEYS_LIST = new GenericTypeIndicator<Map<String, ConfigKey>>() {};
    public static final GenericTypeIndicator TYPE_DIAGNOSIS = new GenericTypeIndicator<Diagnosis>() {};
    public static final GenericTypeIndicator TYPE_DIAGNOSIS_LIST = new GenericTypeIndicator<Map<String, Diagnosis>>() {};
    public static final GenericTypeIndicator TYPE_SCREEN = new GenericTypeIndicator<Screen>() {};
    public static final GenericTypeIndicator TYPE_SCREENS_LIST = new GenericTypeIndicator<Map<String, Screen>>() {};
    public static final GenericTypeIndicator TYPE_SCRIPT = new GenericTypeIndicator<Script>() {};
    public static final GenericTypeIndicator TYPE_SCRIPTS_LIST = new GenericTypeIndicator<Map<String, Script>>() {};

    private static FirebaseStore sInstance;

    public static FirebaseStore get() {
        if (sInstance == null) {
            sInstance = new FirebaseStore();
        }
        return sInstance;
    }

    private FirebaseDatabase mDatabase;
    private CompositeSubscription mSubscription;
    private HashMap<String, Subscription> mManagedSubscriptions = new HashMap<>();

    private List<ConfigKey> mConfigKeys;
    private List<Script> mScripts;
    private Map<String, List<Screen>> mScreens;

    private Comparator<Screen> mScreenComparator = (left, right) -> {
        if (left.position == null) {
            return -1;
        } else if (right.position == null) {
            return 1;
        } else {
            return left.position.compareTo(right.position);
        }
    };

    private FirebaseStore() {

    }

    public void initialize() {
        if (mDatabase != null) {
            throw new RuntimeException("FirebaseStore already initialized");
        }
        mDatabase = FirebaseDatabase.getInstance();
        subscribe();
    }

    public boolean isInitialized() {
        return (mDatabase != null);
    }

    private void checkInitialized() {
        if (mDatabase == null) {
            throw new RuntimeException("FirebaseStore already initialized");
        }
    }

//    public Observable<List<ConfigKey>> loadConfigKeys() {
//        //noinspection unchecked
//        return RxFirebase.observeOnce(queryConfigKeys(), TYPE_CONFIG_KEYS_LIST);
//    }
//
//    public Observable<ConfigKey> observeConfigKeys() {
//        //noinspection unchecked
//        return RxFirebase.observe(queryConfigKeys(), TYPE_CONFIG_KEY);
//    }

    public Observable<List<Script>> loadScripts() {
        //noinspection unchecked
        return RxFirebase.observeOnce(queryScripts(), TYPE_SCRIPTS_LIST)
                .map(result -> convertMapToList((Map<String, Script>) result));
    }

    public List<Script> loadScriptsSync() {
        //noinspection unchecked
        return (List<Script>) RxFirebase.observeOnce(queryScripts(), TYPE_SCRIPTS_LIST)
                .map(result -> convertMapToList((Map<String, Script>) result))
                .toBlocking()
                .single();

    }

    public Observable<AdminPassword> observeAdminPassword() {
        //noinspection unchecked
        return RxFirebase.observe(query(ADMIN_PASSWORD), TYPE_ADMIN_PASSWORD);
    }

    public Observable<RxFirebase.FirebaseChildEvent<Script>> observeScripts() {
        //noinspection unchecked
        return RxFirebase.observeChildren(queryScripts(), TYPE_SCRIPT);
    }

//    public Observable<List<Screen>> loadScreens() {
//        //noinspection unchecked
//        return RxFirebase.observeOnce(queryScreens(), TYPE_SCREENS_LIST);
//    }
//
//    public Observable<Screen> observeScreens() {
//        //noinspection unchecked
//        return RxFirebase.observe(queryScreens(), TYPE_SCREEN);
//    }
//
    public Observable<List<Screen>> loadScreens(String scriptId) {
        //noinspection unchecked
        return RxFirebase.observeOnce(queryScreens(scriptId), TYPE_SCREENS_LIST)
                .map(result -> convertMapToList((Map<String, Screen>) result, mScreenComparator));
    }

    public Observable<RxFirebase.FirebaseChildEvent<Screen>> observeScreens(String scriptId) {
        //noinspection unchecked
        return RxFirebase.observeChildren(queryScreens(scriptId), TYPE_SCREEN);
    }

    public Observable<List<Diagnosis>> loadDiagnosis(String scriptId) {
        //noinspection unchecked
        return RxFirebase.observeOnce(queryDiagnosis(scriptId), TYPE_DIAGNOSIS_LIST)
                .map(result -> convertMapToList((Map<String, Screen>) result));
    }

    public Observable<RxFirebase.FirebaseChildEvent<Diagnosis>> observeDiagnosis(String scriptId) {
        //noinspection unchecked
        return RxFirebase.observeChildren(queryDiagnosis(scriptId), TYPE_DIAGNOSIS);
    }

    public Query queryConfigKeys() {
        return query(CONFIG_KEYS);
    }

    public Query queryScripts() {
        return query(SCRIPTS);
    }

    public Query queryScreens() {
        return query(SCREEENS);
    }

    public Query queryScreens(String scriptId) {
        return query(SCREEENS, scriptId).orderByChild(ORDER_BY_FIELD);
    }

    public Query queryScreen(String scriptId, String screenId) {
        return query(SCREEENS, scriptId, screenId);
    }

    public Query queryDiagnosis() {
        return query(DIAGNOSIS);
    }

    public Query queryDiagnosis(String scriptId) {
        return query(DIAGNOSIS, scriptId).orderByChild(ORDER_BY_FIELD);
    }

    public Query queryDiagnosis(String scriptId, String diagnosisId) {
        return query(DIAGNOSIS, scriptId, diagnosisId);
    }

    private Query query(String collection, String... params) {
        return mDatabase.getReference(path(collection, params));
    }

    public void subscribe() {
        Log.d(TAG, "Subscribing to data changes");
        addSubscription(observeScripts().subscribe(event -> {
            switch (event.eventType) {
                case RxFirebase.FirebaseChildEvent.TYPE_ADD:
                    Log.d(TAG, String.format("Script event [type=ADD, key=%s]", event.key));
                    subscribeScriptScreens(event.key);
                    subscribeScriptDiagnosis(event.key);
                    break;
                case RxFirebase.FirebaseChildEvent.TYPE_CHANGE:
                    Log.d(TAG, String.format("Script event [type=CHANGE, key=%s]", event.key));
                    break;
                case RxFirebase.FirebaseChildEvent.TYPE_MOVE:
                    Log.d(TAG, String.format("Script event [type=MOVE, key=%s]", event.key));
                    break;
                case RxFirebase.FirebaseChildEvent.TYPE_REMOVE:
                    Log.d(TAG, String.format("Script event [type=REMOVE, key=%s]", event.key));
                    unsubscribeScriptScreens(event.key);
                    unsubscribeScriptDiagnosis(event.key);
                    break;
            }
        }));

        addSubscription(observeAdminPassword().subscribe(value -> {
            Log.d(TAG, "Received admin password");

            RealmStore.storeAdminPassword(new org.neotree.model.realm.AdminPassword(value));
        }));
    }

    public void subscribeScriptScreens(String scriptId) {
        Log.d(TAG, String.format("Subscribing to screen data changes [scriptId=%s]", scriptId));
        Subscription subscription = observeScreens(scriptId).subscribe(event -> {
            switch (event.eventType) {
                case RxFirebase.FirebaseChildEvent.TYPE_ADD:
                    Log.d(TAG, String.format("Screen event [type=ADD, scriptId=%s, key=%s]", scriptId, event.key));
                    break;
                case RxFirebase.FirebaseChildEvent.TYPE_CHANGE:
                    Log.d(TAG, String.format("Screen event [type=CHANGE, scriptId=%s, key=%s]", scriptId, event.key));
                    break;
                case RxFirebase.FirebaseChildEvent.TYPE_MOVE:
                    Log.d(TAG, String.format("Screen event [type=MOVE, scriptId=%s, key=%s]", scriptId, event.key));
                    break;
                case RxFirebase.FirebaseChildEvent.TYPE_REMOVE:
                    Log.d(TAG, String.format("Screen event [type=REMOVE, scriptId=%s, key=%s]", scriptId, event.key));
                    break;
            }
        });
        addManagedSubscription(String.format("screens/%s", scriptId), subscription);
    }

    public void unsubscribeScriptScreens(String scriptId) {
        Log.d(TAG, String.format("Unsubscribing from screen data changes [scriptId=%s]", scriptId));
        removeManagedSubscription(String.format("screens/%s", scriptId));
    }

    public void subscribeScriptDiagnosis(String scriptId) {
        Log.d(TAG, String.format("Subscribing to diagnosis data changes [scriptId=%s]", scriptId));
        Subscription subscription = observeDiagnosis(scriptId).subscribe(event -> {
            switch (event.eventType) {
                case RxFirebase.FirebaseChildEvent.TYPE_ADD:
                    Log.d(TAG, String.format("Diagnosis event [type=ADD, scriptId=%s, key=%s]", scriptId, event.key));
                    break;
                case RxFirebase.FirebaseChildEvent.TYPE_CHANGE:
                    Log.d(TAG, String.format("Diagnosis event [type=CHANGE, scriptId=%s, key=%s]", scriptId, event.key));
                    break;
                case RxFirebase.FirebaseChildEvent.TYPE_MOVE:
                    Log.d(TAG, String.format("Diagnosis event [type=MOVE, scriptId=%s, key=%s]", scriptId, event.key));
                    break;
                case RxFirebase.FirebaseChildEvent.TYPE_REMOVE:
                    Log.d(TAG, String.format("Diagnosis event [type=REMOVE, scriptId=%s, key=%s]", scriptId, event.key));
                    break;
            }
        });
        addManagedSubscription(String.format("diagnosis/%s", scriptId), subscription);
    }

    public void unsubscribeScriptDiagnosis(String scriptId) {
        Log.d(TAG, String.format("Unsubscribing from diagnosis data changes [scriptId=%s]", scriptId));
        removeManagedSubscription(String.format("diagnosis/%s", scriptId));
    }
    public void destroy() {
        Log.d(TAG, "Unsubscribing from all data changes");
        if (mSubscription != null && mSubscription.hasSubscriptions() && !mSubscription.isUnsubscribed()) {
            mSubscription.unsubscribe();
        }
        mSubscription = null;
        mDatabase = null;
        sInstance = null;
    }

    private String path(String collection, String... params) {
        StringBuilder path = new StringBuilder(collection);
        if (params != null) {
            for (String param : params) {
                path.append(PATH_SEPARATOR);
                path.append(param);
            }
        }
        return path.toString();
    }

    private <K, V> List<V> convertMapToList(Map<K, V> map) {
        return convertMapToList(map, null);
    }

    private <K, V> List<V> convertMapToList(Map<K, V> map, Comparator<V> comparator) {
        final ArrayList<V> list = new ArrayList<>();
        if (map != null) {
            for (K key : map.keySet()) {
                list.add(map.get(key));
            }
        }

        if (comparator != null) {
            Collections.sort(list, comparator);
        }
        return list;
    }

    private void addSubscription(Subscription subscription) {
        if (mSubscription == null) {
            mSubscription = new CompositeSubscription();
        }
        mSubscription.add(subscription);
    }

    private void removeSubscription(Subscription subscription) {
        mSubscription.remove(subscription);
    }

    private void addManagedSubscription(String key, Subscription subscription) {
        if (mManagedSubscriptions == null) {
            mManagedSubscriptions = new HashMap<>();
        }
        mManagedSubscriptions.put(key, subscription);
        addSubscription(subscription);
    }

    private void removeManagedSubscription(String key) {
        if (mManagedSubscriptions == null) {
            return;
        }

        Subscription subscription = mManagedSubscriptions.get(key);
        if (subscription != null) {
            removeSubscription(subscription);
        }
    }

}
