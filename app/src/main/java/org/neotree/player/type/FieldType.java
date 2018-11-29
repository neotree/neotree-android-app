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

package org.neotree.player.type;

/**
 * Created by matteo on 31/07/2016.
 */
public enum FieldType {

    DATE(FieldType.VALUE_DATE),
    DATETIME(FieldType.VALUE_DATETIME),
    DROPDOWN(FieldType.VALUE_DROPDOWN),
    NUMBER(FieldType.VALUE_NUMBER),
    PERIOD(FieldType.VALUE_PERIOD),
    TEXT(FieldType.VALUE_TEXT),
    TIME(FieldType.VALUE_TIME);

    private static final String VALUE_DATE = "date";
    private static final String VALUE_DATETIME = "datetime";
    private static final String VALUE_DROPDOWN = "dropdown";
    private static final String VALUE_NUMBER = "number";
    private static final String VALUE_PERIOD = "period";
    private static final String VALUE_TEXT = "text";
    private static final String VALUE_TIME = "time";

    private final String value;

    FieldType(String value) {
        this.value = value;
    }

    public String toString() {
        return value;
    }

    public static FieldType fromString(String value) {
        if (VALUE_DATE.equals(value)) {
            return DATE;
        } else if (VALUE_DATETIME.equals(value)) {
            return DATETIME;
        } else if (VALUE_DROPDOWN.equals(value)) {
            return DROPDOWN;
        } else if (VALUE_NUMBER.equals(value)) {
            return NUMBER;
        } else if (VALUE_PERIOD.equals(value)) {
            return PERIOD;
        } else if (VALUE_TEXT.equals(value)) {
            return TEXT;
        } else if (VALUE_TIME.equals(value)) {
            return TIME;
        }
        return null;
    }

}
