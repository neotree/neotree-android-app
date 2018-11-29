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
public enum DataType {

    BOOLEAN(DataType.VALUE_BOOLEAN),
    DATETIME(DataType.VALUE_DATETIME),
    DATE(DataType.VALUE_DATE),
    ID(DataType.VALUE_ID),
    NUMBER(DataType.VALUE_NUMBER),
    PERIOD(DataType.VALUE_PERIOD),
    SET_ID(DataType.VALUE_SET_ID),
    STRING(DataType.VALUE_STRING),
    TIME(DataType.VALUE_TIME),
    VOID(DataType.VALUE_VOID);

    private static final String VALUE_BOOLEAN = "boolean";
    private static final String VALUE_DATETIME = "datetime";
    private static final String VALUE_DATE = "date";
    private static final String VALUE_ID = "id";
    private static final String VALUE_NUMBER = "number";
    private static final String VALUE_PERIOD = "period";
    private static final String VALUE_SET_ID = "set<id>";
    private static final String VALUE_STRING = "string";
    private static final String VALUE_TIME = "time";
    private static final String VALUE_VOID = "void";

    private final String value;

    DataType(String value) {
        this.value = value;
    }

    public String toString() {
        return value;
    }

    public static DataType fromString(String value) {
        if (VALUE_BOOLEAN.equals(value)) {
            return BOOLEAN;
        } else if (VALUE_DATETIME.equals(value)) {
            return DATETIME;
        } else if (VALUE_DATE.equals(value)) {
            return DATE;
        } else if (VALUE_ID.equals(value)) {
            return ID;
        } else if (VALUE_NUMBER.equals(value)) {
            return NUMBER;
        } else if (VALUE_PERIOD.equals(value)) {
            return PERIOD;
        } else if (VALUE_SET_ID.equals(value)) {
            return SET_ID;
        } else if (VALUE_STRING.equals(value)) {
            return STRING;
        } else if (VALUE_TIME.equals(value)) {
            return TIME;
        } else if (VALUE_VOID.equals(value)) {
            return VOID;
        }
        return null;
    }

}
