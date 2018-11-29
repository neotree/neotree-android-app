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
public enum DefaultValueType {

    COMPUTE(DefaultValueType.VALUE_COMPUTE),
    DATE_MIDNIGHT(DefaultValueType.VALUE_DATE_MIDNIGHT),
    DATE_NOON(DefaultValueType.VALUE_DATE_NOON),
    DATE_NOW(DefaultValueType.VALUE_DATE_NOW),
    EMPTY(DefaultValueType.VALUE_EMPTY),
    UID(DefaultValueType.VALUE_UID);

    private static final String VALUE_COMPUTE = "compute";
    private static final String VALUE_DATE_MIDNIGHT = "date_noon";
    private static final String VALUE_DATE_NOW = "date_now";
    private static final String VALUE_DATE_NOON = "date_noon";
    private static final String VALUE_EMPTY = "";
    private static final String VALUE_UID = "uid";

    private final String value;

    DefaultValueType(String value) {
        this.value = value;
    }

    public String toString() {
        return value;
    }

    public static DefaultValueType fromString(String value) {
        if (VALUE_COMPUTE.equals(value)) {
            return COMPUTE;
        } else if (VALUE_DATE_MIDNIGHT.equals(value)) {
            return DATE_MIDNIGHT;
        } else if (VALUE_DATE_NOON.equals(value)) {
            return DATE_NOON;
        } else if (VALUE_DATE_NOW.equals(value)) {
            return DATE_NOW;
        } else if (VALUE_EMPTY.equals(value)) {
            return EMPTY;
        } else if (VALUE_UID.equals(value)) {
            return UID;
        }
        return null;
    }

}
