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

package org.neotree.model.realm;

import android.content.Context;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.neotree.player.type.DataType;
import org.neotree.support.datastore.RealmStore;

import java.util.ArrayList;
import java.util.HashSet;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;

/**
 * Created by matteo on 15/07/2016.
 */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
public class SessionEntry extends RealmObject {

    @PrimaryKey private String entryId;
    @Index private String scriptId;
    @Index private String sessionId;
    private int position;
    private boolean confidential;
    private String dataType;
    private String key;
    private String label;
    private RealmList<SessionValue> values;
    private String sectionTitle;

    public SessionEntry() {

    }

    public SessionEntry(String scriptId, String sessionId, String sectionTitle, int position, String dataType, String key, String label, SessionValue value) {
        this(scriptId, sessionId, sectionTitle, position, dataType, key, label);
        setValueInternal(value);
    }

    public SessionEntry(String scriptId, String sessionId, String sectionTitle, int position, String dataType, String key, String label, ArrayList<SessionValue> values) {
        this(scriptId, sessionId, sectionTitle, position, dataType, key, label);
        setValueInternal(values);
    }

    private SessionEntry(String scriptId, String sessionId, String sectionTitle, int position, String dataType, String key, String label) {
        setEntryId(RealmStore.buildEntryId(scriptId, sessionId, position, dataType, key));
        setScriptId(scriptId);
        setSessionId(sessionId);
        setDataType(dataType);
        setKey(key);
        setLabel(label);
        setPosition(position);
        setSectionTitle(sectionTitle);
    }

    public DataType getDataTypeAsObject() {
        return DataType.fromString(dataType);
    }

    private void setValueInternal(SessionValue value) {
        RealmList<SessionValue> realmValues = null;
        if (value != null) {
            realmValues = new RealmList<>();
            realmValues.add(value);
        }
        setValues(realmValues);
        setConfidential(value.isConfidential());
    }

    private void setValueInternal(ArrayList<SessionValue> values) {
        RealmList<SessionValue> realmValues = null;
        if (values != null) {
            realmValues = new RealmList<>();
            realmValues.addAll(values);

            // Set confidentiality from first item (they are always the same)
            if (values != null && values.size() > 0) {
                setConfidential(values.get(0).isConfidential());
            } else {
                setConfidential(false);
            }
        }
        setValues(realmValues);
    }

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public String getScriptId() {
        return scriptId;
    }

    public void setScriptId(String scriptId) {
        this.scriptId = scriptId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isConfidential() {
        return confidential;
    }

    public void setConfidential(boolean confidential) {
        this.confidential = confidential;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public RealmList<SessionValue> getValues() {
        return values;
    }

    public void setValues(RealmList<SessionValue> values) {
        this.values = values;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }

    @JsonIgnore
    public boolean isMultipleValues() {
        return (DataType.fromString(getDataType()) == DataType.SET_ID);
    }

    @JsonIgnore
    public SessionValue getSingleValue() {
        return (values != null && values.size() > 0) ? values.get(0) : null;
    }

    @JsonIgnore
    public String getValueAsString(Context context) {
        final StringBuilder sb = new StringBuilder();
        final RealmList<SessionValue> values = getValues();
        for (int i = 0; values != null && i < values.size(); i++) {
            if (i > 0) {
                sb.append("\n");
            }
            sb.append(values.get(i).getValueAsFormattedString(context));
        }
        return sb.toString();
    }

    @JsonIgnore
    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        if (values == null || values.size() == 0) {
            return null;
        }

        if (isMultipleValues()) {
            HashSet<String> nativeValues = new HashSet<>();
            for (SessionValue v : values) {
                nativeValues.add(v.getValue());
            }
            return (T) nativeValues;
        } else {
            return getSingleValue().getValue();
        }
    }

}
