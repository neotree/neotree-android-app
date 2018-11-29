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
 * Created by matteo on 31/07/2016.
 */
@IgnoreExtraProperties
public class Item implements Parcelable {

    public boolean checked;
    public boolean confidential;
    public String dataType;
    public boolean exclusive;
    public String id;
    public String key;
    public String label;
    public Integer position;
    public String summary;

    public Item() {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.checked ? (byte) 1 : (byte) 0);
        dest.writeByte(this.confidential ? (byte) 1 : (byte) 0);
        dest.writeString(this.dataType);
        dest.writeByte(this.exclusive ? (byte) 1 : (byte) 0);
        dest.writeString(this.id);
        dest.writeString(this.key);
        dest.writeString(this.label);
        dest.writeValue(this.position);
        dest.writeString(this.summary);
    }

    protected Item(Parcel in) {
        this.checked = in.readByte() != 0;
        this.confidential = in.readByte() != 0;
        this.dataType = in.readString();
        this.exclusive = in.readByte() != 0;
        this.id = in.readString();
        this.key = in.readString();
        this.label = in.readString();
        this.position = (Integer) in.readValue(Integer.class.getClassLoader());
        this.summary = in.readString();
    }

    public static final Creator<Item> CREATOR = new Creator<Item>() {
        @Override
        public Item createFromParcel(Parcel source) {
            return new Item(source);
        }

        @Override
        public Item[] newArray(int size) {
            return new Item[size];
        }
    };
}
