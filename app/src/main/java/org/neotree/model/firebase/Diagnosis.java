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
public class Diagnosis implements Parcelable {

    public String diagnosisId;
    public String name;
    public String description;
    public String expression;
    public String text1;
    public String text2;
    public String text3;
    public FileInfo image1;
    public FileInfo image2;
    public FileInfo image3;
    public Long createdAt;
    public Long updatedAt;
    public ArrayList<Symptom> symptoms;

    public Diagnosis() {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.diagnosisId);
        dest.writeString(this.name);
        dest.writeString(this.description);
        dest.writeString(this.expression);
        dest.writeString(this.text1);
        dest.writeString(this.text2);
        dest.writeString(this.text3);
        dest.writeParcelable(this.image1, flags);
        dest.writeParcelable(this.image2, flags);
        dest.writeParcelable(this.image3, flags);
        dest.writeValue(this.createdAt);
        dest.writeValue(this.updatedAt);
        dest.writeTypedList(this.symptoms);
    }

    protected Diagnosis(Parcel in) {
        this.diagnosisId = in.readString();
        this.name = in.readString();
        this.description = in.readString();
        this.expression = in.readString();
        this.text1 = in.readString();
        this.text2 = in.readString();
        this.text3 = in.readString();
        this.image1 = in.readParcelable(FileInfo.class.getClassLoader());
        this.image2 = in.readParcelable(FileInfo.class.getClassLoader());
        this.image3 = in.readParcelable(FileInfo.class.getClassLoader());
        this.createdAt = (Long) in.readValue(Long.class.getClassLoader());
        this.updatedAt = (Long) in.readValue(Long.class.getClassLoader());
        this.symptoms = in.createTypedArrayList(Symptom.CREATOR);
    }

    public static final Creator<Diagnosis> CREATOR = new Creator<Diagnosis>() {
        @Override
        public Diagnosis createFromParcel(Parcel source) {
            return new Diagnosis(source);
        }

        @Override
        public Diagnosis[] newArray(int size) {
            return new Diagnosis[size];
        }
    };
}
