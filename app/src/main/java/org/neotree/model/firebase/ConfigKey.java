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

package org.neotree.model.firebase;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by matteo on 25/07/2016.
 */
@IgnoreExtraProperties
public class ConfigKey implements Parcelable {

    public String configKeyId;
    public String configKey;
    public String label;
    public String summary;
    public Long createdAt;
    public Long updatedAt;

    public ConfigKey() {

    }

    public ConfigKey(String configKeyId, String configKey, String label, String summary, Long createdAt, Long updatedAt) {
        this.configKey = configKey;
        this.label = label;
        this.summary = summary;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.configKeyId = configKeyId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.configKeyId);
        dest.writeString(this.configKey);
        dest.writeString(this.label);
        dest.writeString(this.summary);
        dest.writeValue(this.createdAt);
        dest.writeValue(this.updatedAt);
    }

    protected ConfigKey(Parcel in) {
        this.configKeyId = in.readString();
        this.configKey = in.readString();
        this.label = in.readString();
        this.summary = in.readString();
        this.createdAt = (Long) in.readValue(Long.class.getClassLoader());
        this.updatedAt = (Long) in.readValue(Long.class.getClassLoader());
    }

    public static final Creator<ConfigKey> CREATOR = new Creator<ConfigKey>() {
        @Override
        public ConfigKey createFromParcel(Parcel source) {
            return new ConfigKey(source);
        }

        @Override
        public ConfigKey[] newArray(int size) {
            return new ConfigKey[size];
        }
    };
}
