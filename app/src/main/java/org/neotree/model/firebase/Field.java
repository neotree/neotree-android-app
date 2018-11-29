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

import org.neotree.player.type.DataType;
import org.neotree.player.type.FieldType;

/**
 * Created by matteo on 31/07/2016.
 */
@IgnoreExtraProperties
public class Field implements Parcelable {

    private static final String TAG = Field.class.getSimpleName();

    public static Field numeric(String key, String label, String minValue, String maxValue) {
        Field field = new Field();
        field.type = FieldType.NUMBER.toString();
        field.dataType = DataType.NUMBER.toString();
        field.key = key;
        field.label = label;
        field.minValue = minValue;
        field.maxValue = maxValue;
        return field;
    }

    public String calculation;
    public String condition;
    public boolean confidential;
    public String dataType;
    public String defaultValue;
    public String format;
    public String initialValue;
    public String key;
    public String label;
    public String maxValue;
    public String minValue;
    public Boolean optional;
    public Integer position;
    public String type;
    public String values;

    public Field() {

    }

    public boolean isOptional() {
        return (optional != null && optional);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.calculation);
        dest.writeString(this.condition);
        dest.writeByte(this.confidential ? (byte) 1 : (byte) 0);
        dest.writeString(this.dataType);
        dest.writeString(this.defaultValue);
        dest.writeString(this.format);
        dest.writeString(this.initialValue);
        dest.writeString(this.key);
        dest.writeString(this.label);
        dest.writeString(this.maxValue);
        dest.writeString(this.minValue);
        dest.writeValue(this.optional);
        dest.writeValue(this.position);
        dest.writeString(this.type);
        dest.writeString(this.values);
    }

    protected Field(Parcel in) {
        this.calculation = in.readString();
        this.condition = in.readString();
        this.confidential = in.readByte() != 0;
        this.dataType = in.readString();
        this.defaultValue = in.readString();
        this.format = in.readString();
        this.initialValue = in.readString();
        this.key = in.readString();
        this.label = in.readString();
        this.maxValue = in.readString();
        this.minValue = in.readString();
        this.optional = (Boolean) in.readValue(Boolean.class.getClassLoader());
        this.position = (Integer) in.readValue(Integer.class.getClassLoader());
        this.type = in.readString();
        this.values = in.readString();
    }

    public static final Creator<Field> CREATOR = new Creator<Field>() {
        @Override
        public Field createFromParcel(Parcel source) {
            return new Field(source);
        }

        @Override
        public Field[] newArray(int size) {
            return new Field[size];
        }
    };
}
