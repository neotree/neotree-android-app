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

package org.neotree.diagnosis.type;

/**
 * Created by matteo on 31/07/2016.
 */
public enum SymptomType {

    RISK(SymptomType.VALUE_RISK),
    SIGN(SymptomType.VALUE_SIGN);

    private static final String VALUE_RISK = "risk";
    private static final String VALUE_SIGN = "sign";

    private final String value;

    SymptomType(String value) {
        this.value = value;
    }

    public String toString() {
        return value;
    }

    public static SymptomType fromString(String value) {
        if (VALUE_RISK.equals(value)) {
            return RISK;
        } else if (VALUE_SIGN.equals(value)) {
            return SIGN;
        }
        return null;
    }

}
