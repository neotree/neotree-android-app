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

import java.util.ArrayList;

/**
 * Created by matteo on 25/07/2016.
 */
@IgnoreExtraProperties
public class Metadata implements Parcelable {

    public String dataType;
    public boolean confidential;
    public String key;
    public String label;
    public String maxValue;
    public String minValue;
    public String multiplier;
    public String negativeLabel;
    public String positiveLabel;
    public String text1;
    public String text2;
    public String text3;
    public String timerValue;
    public String title1;
    public String title2;
    public String title3;
    public FileInfo image1;
    public FileInfo image2;
    public FileInfo image3;
    public ArrayList<Field> fields;
    public ArrayList<Item> items;
    public ArrayList<Section> sections;

    public Metadata() {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.dataType);
        dest.writeByte(this.confidential ? (byte) 1 : (byte) 0);
        dest.writeString(this.key);
        dest.writeString(this.label);
        dest.writeString(this.maxValue);
        dest.writeString(this.minValue);
        dest.writeString(this.multiplier);
        dest.writeString(this.negativeLabel);
        dest.writeString(this.positiveLabel);
        dest.writeString(this.text1);
        dest.writeString(this.text2);
        dest.writeString(this.text3);
        dest.writeString(this.timerValue);
        dest.writeString(this.title1);
        dest.writeString(this.title2);
        dest.writeString(this.title3);
        dest.writeParcelable(this.image1, flags);
        dest.writeParcelable(this.image2, flags);
        dest.writeParcelable(this.image3, flags);
        dest.writeTypedList(this.fields);
        dest.writeTypedList(this.items);
        dest.writeTypedList(this.sections);
    }

    protected Metadata(Parcel in) {
        this.dataType = in.readString();
        this.confidential = in.readByte() != 0;
        this.key = in.readString();
        this.label = in.readString();
        this.maxValue = in.readString();
        this.minValue = in.readString();
        this.multiplier = in.readString();
        this.negativeLabel = in.readString();
        this.positiveLabel = in.readString();
        this.text1 = in.readString();
        this.text2 = in.readString();
        this.text3 = in.readString();
        this.timerValue = in.readString();
        this.title1 = in.readString();
        this.title2 = in.readString();
        this.title3 = in.readString();
        this.image1 = in.readParcelable(FileInfo.class.getClassLoader());
        this.image2 = in.readParcelable(FileInfo.class.getClassLoader());
        this.image3 = in.readParcelable(FileInfo.class.getClassLoader());
        this.fields = in.createTypedArrayList(Field.CREATOR);
        this.items = in.createTypedArrayList(Item.CREATOR);
        this.sections = in.createTypedArrayList(Section.CREATOR);
    }

    public static final Creator<Metadata> CREATOR = new Creator<Metadata>() {
        @Override
        public Metadata createFromParcel(Parcel source) {
            return new Metadata(source);
        }

        @Override
        public Metadata[] newArray(int size) {
            return new Metadata[size];
        }
    };
}
