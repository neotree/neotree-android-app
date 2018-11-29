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
import android.text.TextUtils;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by matteo on 25/07/2016.
 */
@IgnoreExtraProperties
public class Screen implements Parcelable {

    public boolean skippable;
    public String actionText;
    public String condition;
    public String contentText;
    public String epicId;
    public String infoText;
    public Integer position;
    public String refId;
    public String screenId;
    public String sectionTitle;
    public String step;
    public String storyId;
    public String title;
    public String triggers;
    public String type;
    public Long createdAt;
    public Long updatedAt;
    public Metadata metadata;

    public Screen() {

    }

    public boolean hasContentText() {
        return !TextUtils.isEmpty(contentText);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.skippable ? (byte) 1 : (byte) 0);
        dest.writeString(this.actionText);
        dest.writeString(this.condition);
        dest.writeString(this.contentText);
        dest.writeString(this.epicId);
        dest.writeString(this.infoText);
        dest.writeValue(this.position);
        dest.writeString(this.refId);
        dest.writeString(this.screenId);
        dest.writeString(this.sectionTitle);
        dest.writeString(this.step);
        dest.writeString(this.storyId);
        dest.writeString(this.title);
        dest.writeString(this.triggers);
        dest.writeString(this.type);
        dest.writeValue(this.createdAt);
        dest.writeValue(this.updatedAt);
        dest.writeParcelable(this.metadata, flags);
    }

    protected Screen(Parcel in) {
        this.skippable = in.readByte() != 0;
        this.actionText = in.readString();
        this.condition = in.readString();
        this.contentText = in.readString();
        this.epicId = in.readString();
        this.infoText = in.readString();
        this.position = (Integer) in.readValue(Integer.class.getClassLoader());
        this.refId = in.readString();
        this.screenId = in.readString();
        this.sectionTitle = in.readString();
        this.step = in.readString();
        this.storyId = in.readString();
        this.title = in.readString();
        this.triggers = in.readString();
        this.type = in.readString();
        this.createdAt = (Long) in.readValue(Long.class.getClassLoader());
        this.updatedAt = (Long) in.readValue(Long.class.getClassLoader());
        this.metadata = in.readParcelable(Metadata.class.getClassLoader());
    }

    public static final Creator<Screen> CREATOR = new Creator<Screen>() {
        @Override
        public Screen createFromParcel(Parcel source) {
            return new Screen(source);
        }

        @Override
        public Screen[] newArray(int size) {
            return new Screen[size];
        }
    };
}
