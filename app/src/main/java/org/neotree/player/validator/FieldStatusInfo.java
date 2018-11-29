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

package org.neotree.player.validator;

/**
 * Created by matteo on 23/08/2016.
 */
public class FieldStatusInfo {

    private int mIndex;
    private boolean mEnabled;

    public FieldStatusInfo(int index, boolean enabled) {
        mIndex = index;
        mEnabled = enabled;
    }

    public int getIndex() {
        return mIndex;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FieldStatusInfo{");
        sb.append("mIndex=").append(mIndex);
        sb.append(", mEnabled=").append(mEnabled);
        sb.append('}');
        return sb.toString();
    }

}
