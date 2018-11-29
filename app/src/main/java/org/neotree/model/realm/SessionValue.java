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
import android.text.TextUtils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.ISODateTimeFormat;
import org.joda.time.format.ISOPeriodFormat;
import org.neotree.NeoTree;
import org.neotree.R;
import org.neotree.player.type.DataType;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;

/**
 * Created by matteo on 09/09/2016.
 */
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.NONE,
        getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
public class SessionValue extends RealmObject {

    @Ignore private boolean confidential;
    private String key;
    @Ignore private String label;
    private String dataType;
    private String format;
    private String valueLabel;
    private String stringValue;
    private Double doubleValue;
    private Boolean booleanValue;

    public SessionValue() {

    }

    public SessionValue(String dataType, String key, String label, String format, String valueLabel, Object value, boolean confidential) {
        setDataType(dataType);
        setKey(key);
        setLabel(label);
        setFormat(format);
        setValueLabel(valueLabel);
        setValue(value);
        setConfidential(confidential);
    }

    public DataType getDataTypeAsObject() {
        return DataType.fromString(dataType);
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        switch (getDataTypeAsObject()) {
            case BOOLEAN:
                return (T) getBooleanValue();
            case DATETIME:
            case DATE:
            case TIME:
                return (T) ((getStringValue() != null) ? DateTime.parse(getStringValue(), ISODateTimeFormat.dateTimeNoMillis()) : null);
            case ID:
                return (T) getStringValue();
            case NUMBER:
                return (T) getDoubleValue();
            case PERIOD:
                return (T) ((getStringValue() != null) ? Period.parse(getStringValue(), ISOPeriodFormat.standard()) : null);
            case SET_ID:
                return (T) getStringValue();
            case STRING:
                return (T) getStringValue();
            case VOID:
            default:
                return null;
        }
    }

    @JsonIgnore
    @SuppressWarnings("unchecked")
    public String getValueAsFormattedString(Context context) {
        switch (getDataTypeAsObject()) {
            case BOOLEAN:
                return (getBooleanValue() != null && getBooleanValue()) ? "Yes" : "No";
            case DATETIME:
                return (getStringValue() == null) ? context.getString(R.string.label_not_set)
                        : DateTime.parse(getStringValue(), ISODateTimeFormat.dateTimeNoMillis())
                            .toString(NeoTree.NeoTreeFormat.DATETIME);
            case DATE:
                return (getStringValue() == null) ? context.getString(R.string.label_not_set)
                        : DateTime.parse(getStringValue(), ISODateTimeFormat.dateTimeNoMillis())
                            .toString(NeoTree.NeoTreeFormat.DATE);
            case SET_ID:
            case ID:
                //return String.format("%s - %s", getStringValue(), getValueLabel());
                return getValueLabel();
            case NUMBER:
                final int decimalDigits = (TextUtils.isEmpty(getFormat())) ? 0 : getFormat().length();
                return (getDoubleValue() == null) ? context.getString(R.string.label_not_set)
                        : String.format("%." + decimalDigits + "f", getDoubleValue());
            case PERIOD:
                return (getStringValue() == null) ? context.getString(R.string.label_not_set)
                        : Period.parse(getStringValue(), ISOPeriodFormat.standard())
                            .toString(NeoTree.NeoTreeFormat.PERIOD);
            case STRING:
                return (getStringValue() == null) ? context.getString(R.string.label_not_set)
                        : getStringValue();
            case TIME:
                return (getStringValue() == null) ? context.getString(R.string.label_not_set)
                        : DateTime.parse(getStringValue(), ISODateTimeFormat.dateTimeNoMillis())
                            .toString(NeoTree.NeoTreeFormat.TIME);
            case VOID:
            default:
                break;
        }
        return "";
    }

    @JsonIgnore
    @SuppressWarnings("unchecked")
    public String getValueAsExportString(Context context) {
        switch (getDataTypeAsObject()) {
            case BOOLEAN:
                return (getBooleanValue() != null && getBooleanValue()) ? "Yes" : "No";
            case DATETIME:
                return (getStringValue() == null) ? context.getString(R.string.label_not_set)
                        : DateTime.parse(getStringValue(), ISODateTimeFormat.dateTimeNoMillis())
                        .toString(NeoTree.NeoTreeFormat.DATETIME_EXPORT);
            case DATE:
                return (getStringValue() == null) ? context.getString(R.string.label_not_set)
                        : DateTime.parse(getStringValue(), ISODateTimeFormat.dateTimeNoMillis())
                        .toString(NeoTree.NeoTreeFormat.DATE_EXPORT);
            case ID:
            case STRING:
                return getStringValue();
            case NUMBER:
                final int decimalDigits = (TextUtils.isEmpty(getFormat())) ? 0 : getFormat().length();
                return (getDoubleValue() == null) ? context.getString(R.string.label_not_set)
                        : String.format("%." + decimalDigits + "f", getDoubleValue());
            case PERIOD:
                return (getStringValue() == null) ? context.getString(R.string.label_not_set)
                        : Period.parse(getStringValue(), ISOPeriodFormat.standard())
                        .toString(NeoTree.NeoTreeFormat.PERIOD);
            case TIME:
                return (getStringValue() == null) ? context.getString(R.string.label_not_set)
                        : DateTime.parse(getStringValue(), ISODateTimeFormat.dateTimeNoMillis())
                        .toString(NeoTree.NeoTreeFormat.TIME);
            case SET_ID:
            case VOID:
            default:
                break;
        }
        return null;
    }

    public void setValue(Object value) {
        if (value == null) {
            setBooleanValue(null);
            setDoubleValue(null);
            setStringValue(null);
        } else if (value instanceof Boolean) {
            setBooleanValue((Boolean) value);
        } else if (value instanceof Double) {
            setDoubleValue((Double) value);
        } else if (value instanceof String) {
            setStringValue((String) value);
        } else if (value instanceof DateTime) {
            setStringValue(((DateTime) value).toString(ISODateTimeFormat.dateTimeNoMillis()));
        } else if (value instanceof Period) {
            setStringValue(((Period) value).toString(ISOPeriodFormat.standard()));
        }
    }

    public boolean isConfidential() {
        return confidential;
    }

    public void setConfidential(boolean confidential) {
        this.confidential = confidential;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValueLabel() {
        return valueLabel;
    }

    public void setValueLabel(String valueLabel) {
        this.valueLabel = valueLabel;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }
}
