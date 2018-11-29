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

import android.text.TextUtils;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.neotree.model.firebase.Field;
import org.neotree.model.firebase.Item;
import org.neotree.model.firebase.Metadata;
import org.neotree.model.realm.AdminPassword;
import org.neotree.model.realm.Session;
import org.neotree.model.realm.SessionEntry;
import org.neotree.model.realm.SessionValue;
import org.neotree.player.type.DataType;
import org.neotree.player.type.FieldType;
import org.neotree.player.type.ScreenType;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by matteo on 13/09/2016.
 */

public class RealmStore {

    private static final String TAG = RealmStore.class.getSimpleName();

    private static MessageDigest sMessageDigest;

    public interface OnRealmTransactionListener {
        void onTransactionDone();
    }

    public interface OnRealmDetachedResultListener<T> {
        void onRealmQueryDetachedResult(List<T> results);
    }

    static {
        try {
            sMessageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error creating MD5 message digest", e);
        }
    }

    public static Session createSession(Realm realm, String sessionId, String scriptId) {
        final Session session = new Session(sessionId, scriptId);
        realm.executeTransactionAsync(bgRealm -> {
            bgRealm.copyToRealmOrUpdate(session);
        }, () -> {
            // Transaction was a success.
            Log.v(TAG, "createSession(): success");
        }, error -> {
            // Transaction failed and was automatically canceled.
            Log.e(TAG, "createSession(): failure", error);
        });
        return session;
    }

    public static void finishSession(Realm realm, Session session) {
        realm.executeTransactionAsync(bgRealm -> {
            session.setCompletedAt(DateTime.now().toString(ISODateTimeFormat.dateTimeNoMillis()));
            bgRealm.copyToRealmOrUpdate(session);
        }, () -> {
            // Transaction was a success.
            Log.v(TAG, "finishSession(): success");
        }, error -> {
            // Transaction failed and was automatically canceled.
            Log.e(TAG, "finishSession(): failure", error);
        });
    }

    public static void storeValue(Realm realm, String scriptId, String sessionId, String sectionTitle, int position, SessionValue value) {
        realm.executeTransactionAsync(bgRealm -> {
            String entryId = buildEntryId(scriptId, sessionId, position, value.getDataType(), value.getKey());
            RealmResults<SessionEntry> results = bgRealm.where(SessionEntry.class)
                    .equalTo("entryId", entryId)
                    .findAll();

            if (results.size() > 0) {
                results.first().getValues().deleteAllFromRealm();
            }

            SessionEntry entry = new SessionEntry(scriptId, sessionId, sectionTitle, position, value.getDataType(), value.getKey(), value.getLabel(), value);
            bgRealm.copyToRealmOrUpdate(entry);
        }, () -> {
            // Transaction was a success.
            Log.v(TAG, "storeValue(single): success");
        }, error -> {
            // Transaction failed and was automatically canceled.
            Log.e(TAG, "storeValue(single): failure", error);
        });
    }

    public static void storeValue(Realm realm, String scriptId, String sessionId, String sectionTitle, int position, String key, String label, ArrayList<SessionValue> values) {
        realm.executeTransactionAsync(bgRealm -> {
            String entryId = buildEntryId(scriptId, sessionId, position, DataType.SET_ID.toString(), key);
            RealmResults<SessionEntry> results = bgRealm.where(SessionEntry.class)
                    .equalTo("entryId", entryId)
                    .findAll();

            if (results.size() > 0) {
                results.first().getValues().deleteAllFromRealm();
            }

            SessionEntry entry = new SessionEntry(scriptId, sessionId, sectionTitle, position, DataType.SET_ID.toString(), key, label, values);
            bgRealm.copyToRealmOrUpdate(entry);
        }, () -> {
            // Transaction was a success.
            Log.v(TAG, "storeValue(multiple) : success");
        }, error -> {
            // Transaction failed and was automatically canceled.
            Log.e(TAG, "storeValue(multiple): failure", error);
        });
    }

    public static void loadSessions(Realm realm, RealmChangeListener<RealmResults<Session>> callback) {
        RealmResults<Session> result = realm.where(Session.class)
                .findAllAsync()
                .sort("createdAt", Sort.DESCENDING);
        result.addChangeListener(callback);
    }

    public static Session loadSession(Realm realm, String sessionId) {
        /*Session result = realm
                .where(Session.class)
                .equalTo("scriptTitle", sessionId)
                .findFirst();*/
        Session result = realm
                .where(Session.class)
                .equalTo("sessionId", sessionId)
                .findFirst();
        return realm.copyFromRealm(result);
    }

    public static void loadEntriesForSession(Realm realm, String sessionId, boolean confidential, RealmChangeListener<RealmResults<SessionEntry>> callback) {
       RealmQuery<SessionEntry> query = realm
               .where(SessionEntry.class)
               .equalTo("sessionId", sessionId);

       // If confidential mode, make sure confidential records are not retrieved
       if (confidential) {
           query.equalTo("confidential", false);
       }


       RealmResults<SessionEntry> results = query.sort("position", Sort.ASCENDING).findAllAsync();

       results.addChangeListener(callback);
   }

    public static List<SessionEntry> loadEntriesForSession(Realm realm, String sessionId, boolean confidential) {
       /* RealmQuery<SessionEntry> query = realm
                .where(SessionEntry.class)
                .equalTo("scriptTitle", sessionId);*/

        RealmQuery<SessionEntry> query = realm
                .where(SessionEntry.class)
                .equalTo("sessionId", sessionId);

        // If confidential mode, make sure confidential records are not retrieved
        if (confidential) {
            query.equalTo("confidential", false);
        }

        RealmResults<SessionEntry> results = query
                .findAll()
                .sort("position", Sort.ASCENDING);
        return realm.copyFromRealm(results);
    }

    public static List<SessionEntry> loadEntriesForScript(Realm realm, String scriptId, boolean confidential) {
        RealmQuery<SessionEntry> query = realm
                .where(SessionEntry.class)
                .equalTo("scriptId", scriptId);

        // If confidential mode, make sure confidential records are not retrieved
        if (confidential) {
            query.equalTo("confidential", false);
        }

        RealmResults<SessionEntry> results = query
                .findAll()
              //  .sort(new String[] {"scriptTitle", "position"}, new Sort[] { Sort.ASCENDING, Sort.ASCENDING });
                .sort(new String[] {"sessionId", "position"}, new Sort[] { Sort.ASCENDING, Sort.ASCENDING });
        return realm.copyFromRealm(results);
    }

    public static void storeAdminPassword(AdminPassword value) {
        Realm realm = null;

        try {
            realm = Realm.getDefaultInstance();
            realm.executeTransaction(bgRealm -> bgRealm.copyToRealmOrUpdate(value));
        } catch (Exception e) {
            Log.e(TAG, "Error storing admin password", e);
        } finally {
            if (realm != null && !realm.isClosed()) {
                realm.close();
            }
        }
    }

    public static AdminPassword loadAdminPassword(Realm realm) {
        return realm.where(AdminPassword.class).findFirst();
    }

    public static void deleteAllSessions(Realm realm, OnRealmTransactionListener listener) {
        // Delete all in a transaction
        realm.executeTransactionAsync(bgRealm -> {
            RealmQuery<Session> query = bgRealm
                    .where(Session.class);
            deleteSessions(bgRealm, query);
        }, () -> {
            // Transaction was a success.
            Log.v(TAG, "deleteAllSessions() : success");
            listener.onTransactionDone();
        }, error -> {
            // Transaction failed and was automatically canceled.
            Log.e(TAG, "deleteAllSessions(): failure", error);
        });
    }

    public static void deleteIncompleteSessions(Realm realm, OnRealmTransactionListener listener) {
        // Delete all in a transaction
        realm.executeTransactionAsync(bgRealm -> {
            RealmQuery<Session> query = bgRealm
                    .where(Session.class)
                    .isNull("completedAt");
            deleteSessions(bgRealm, query);
        }, () -> {
            // Transaction was a success.
            Log.v(TAG, "deleteIncompleteSessions() : success");
            listener.onTransactionDone();
        }, error -> {
            // Transaction failed and was automatically canceled.
            Log.e(TAG, "deleteIncompleteSessions(): failure", error);
        });
    }

    public static void deleteSingleSession(String sessionId, Realm realm, OnRealmTransactionListener listener) {
        // Delete all in a transaction
        realm.executeTransactionAsync(bgRealm -> {
            RealmQuery<Session> query = bgRealm
                    .where(Session.class)
                   // .equalTo("scriptTitle", sessionId);
                    .equalTo("sessionId", sessionId);
            deleteSessions(bgRealm, query);
        }, () -> {
            // Transaction was a success.
            Log.v(TAG, "deleteSingleSession() : success");
            listener.onTransactionDone();
        }, error -> {
            // Transaction failed and was automatically canceled.
            Log.e(TAG, "deleteSingleSession(): failure", error);
        });
    }

    private static void deleteSessions(Realm realm, RealmQuery<Session> sessionsQuery) {
        RealmResults<Session> sessions = sessionsQuery.findAll();
        if (sessions.size() > 0) {
            String[] sessionIDs = new String[sessions.size()];

            final ArrayList<String> sessionList = new ArrayList<>();
            for (Session session : sessions) {
                sessionList.add(session.getSessionId());
            }
            sessionList.toArray(sessionIDs);

            RealmResults<SessionEntry> sessionEntries = realm
                    .where(SessionEntry.class)
                    //.in("scriptTitle", sessionIDs)
                    .in("sessionId", sessionIDs)
                    .findAll();

            for (SessionEntry sessionEntry : sessionEntries) {
                sessionEntry.getValues().deleteAllFromRealm();
            }
            sessionEntries.deleteAllFromRealm();
            sessions.deleteAllFromRealm();
        }
    }

    public static String buildEntryId(String scriptId, String sessionId, int position, String dataType, String key) {
        String payload = String.format(Locale.getDefault(), "%s:%s:%d:%s:%s", scriptId, sessionId, position, dataType, key);
        sMessageDigest.reset();
        sMessageDigest.update(payload.getBytes(), 0, payload.length());
        return new BigInteger(1, sMessageDigest.digest()).toString(16);
    }

    public static SessionValue getSessionValue(Item item, Object value) {
        return new SessionValue(item.dataType, item.key.trim(), item.label, null, item.label, value, item.confidential);
    }

    public static SessionValue getSessionValue(Field field, Object value) {
        String valueLabel = null;
        if (FieldType.fromString(field.type) == FieldType.DROPDOWN) {
            valueLabel = getLabelForDropDownValue((String) value, field.values);
        }
        return new SessionValue(field.dataType, field.key.trim(), field.label, field.format, valueLabel, value, field.confidential);
    }

    public static SessionValue getSessionValue(ScreenType screenType, Metadata metadata, Object value) {
        String valueLabel = null;
        if (value != null) {
            switch (screenType) {
                case SINGLE_SELECT:
                    valueLabel = getItemLabelForValue(metadata, (String) value);
                    break;
                case YESNO:
                    valueLabel = ((Boolean) value) ? metadata.positiveLabel : metadata.negativeLabel;
                    break;
            }
        }
        return new SessionValue(metadata.dataType, metadata.key.trim(), metadata.label, null, valueLabel, value, metadata.confidential);
    }

    public static ArrayList<SessionValue> getSessionValue(Metadata metadata, Set<String> values) {
        ArrayList<SessionValue> sessionValues = null;
        if (values != null && values.size() > 0) {
            sessionValues = new ArrayList<>();
            for (String value : values) {
                String valueLabel = getItemLabelForValue(metadata, value);
                sessionValues.add(new SessionValue(metadata.dataType, metadata.key.trim(), metadata.label, null, valueLabel, value, metadata.confidential));
            }
        }
        return sessionValues;
    }

    private static String getItemLabelForValue(Metadata metadata, String value) {
        if (metadata.items != null) {
            for (Item item : metadata.items) {
                if (value.equals(item.id)) {
                    return item.label;
                }
            }
        }
        return null;
    }

    private static String getLabelForDropDownValue(String id, String values) {
        if (TextUtils.isEmpty(values) || TextUtils.isEmpty(id)) {
            return null;
        }

        try {
            BufferedReader reader = new BufferedReader(new StringReader(values));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] item = line.split(",");
                String valueId = item[0].trim();
                if (id.equals(valueId)) {
                    return item[1].trim();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return null;
    }
}
