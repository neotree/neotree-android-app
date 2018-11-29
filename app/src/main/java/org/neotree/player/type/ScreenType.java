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
public enum ScreenType {

    CHECKLIST(ScreenType.VALUE_CHECKLIST),
    FORM(ScreenType.VALUE_FORM),
    LIST(ScreenType.VALUE_LIST),
    MANAGEMENT(ScreenType.VALUE_MANAGEMENT),
    MULTI_SELECT(ScreenType.VALUE_MULTI_SELECT),
    SINGLE_SELECT(ScreenType.VALUE_SINGLE_SELECT),
    PROGRESS(ScreenType.VALUE_PROGRESS),
    TIMER(ScreenType.VALUE_TIMER),
    YESNO(ScreenType.VALUE_YESNO);

    private static final String VALUE_CHECKLIST = "checklist";
    private static final String VALUE_FORM = "form";
    private static final String VALUE_MANAGEMENT = "management";
    private static final String VALUE_LIST = "list";
    private static final String VALUE_MULTI_SELECT = "multi_select";
    private static final String VALUE_SINGLE_SELECT = "single_select";
    private static final String VALUE_PROGRESS = "progress";
    private static final String VALUE_TIMER = "timer";
    private static final String VALUE_YESNO = "yesno";

    private final String value;

    ScreenType(String value) {
        this.value = value;
    }

    public String toString() {
        return value;
    }

    public static ScreenType fromString(String value) {
        if (VALUE_CHECKLIST.equals(value)) {
            return CHECKLIST;
        } else if (VALUE_FORM.equals(value)) {
            return FORM;
        } else if (VALUE_MANAGEMENT.equals(value)) {
            return MANAGEMENT;
        } else if (VALUE_LIST.equals(value)) {
            return LIST;
        } else if (VALUE_MULTI_SELECT.equals(value)) {
            return MULTI_SELECT;
        } else if (VALUE_SINGLE_SELECT.equals(value)) {
            return SINGLE_SELECT;
        } else if (VALUE_PROGRESS.equals(value)) {
            return PROGRESS;
        } else if (VALUE_TIMER.equals(value)) {
            return TIMER;
        } else if (VALUE_YESNO.equals(value)) {
            return YESNO;
        }
        return null;
    }

}
