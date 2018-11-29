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

package org.neotree.support.realm;

import android.util.Log;

import org.neotree.model.realm.SessionEntry;

import io.realm.DynamicRealm;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

/**
 * Created by matteo on 24/09/2016.
 */

public class NeoTreeRealmMigration implements RealmMigration {

    private static final String TAG = NeoTreeRealmMigration.class.getSimpleName();

    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {

        Log.i(TAG, String.format("Migrating Realm schema version [from=%d, to=%d]", oldVersion, newVersion));
        RealmSchema schema = realm.getSchema();

        long migrateVersion = oldVersion;

        // Migrate to version 2: Add section title to SessionEntry.
        if (oldVersion == 1) {
            schema.get(SessionEntry.class.getSimpleName())
                    .addField("sectionTitle", String.class);
            migrateVersion++;
        }

//        // Migrate to version 2: Add a primary key + object references
//        // Example:
//        // public Person extends RealmObject {
//        //     private String name;
//        //     @PrimaryKey
//        //     private int age;
//        //     private Dog favoriteDog;
//        //     private RealmList<Dog> dogs;
//        //     // getters and setters left out for brevity
//        // }
//        if (oldVersion == 1) {
//            schema.get("Person")
//                    .addField("id", long.class, FieldAttribute.PRIMARY_KEY)
//                    .addRealmObjectField("favoriteDog", schema.get("Dog"))
//                    .addRealmListField("dogs", schema.get("Dog"));
//            oldVersion++;
//        }
    }

}
