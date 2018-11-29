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

package org.neotree.ui.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.neotree.NeoTree;
import org.neotree.R;
import org.neotree.model.firebase.Field;

import java.util.HashSet;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by matteo on 17/07/2016.
 */
public class PeriodFieldView extends FieldView<Period> {

    @BindView(R.id.field_period_button)
    Button mPeriodValueButton;

    public PeriodFieldView(Context context) {
        this(context, null);
    }

    public PeriodFieldView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PeriodFieldView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int getViewLayoutId() {
        return R.layout.view_field_period;
    }

    @Override
    protected void onRegisterSubscribers(Field field) {
        if (TextUtils.isEmpty(field.calculation)) {
            return;
        }

        final HashSet<String> observeKeys = buildObserveKeySet(field);
        if (observeKeys != null) {
            addSubscription(getScriptPlayer().valueChangeObservable().subscribe((kv) -> {
                if (observeKeys.contains(kv.getKey())) {
                    if (kv.getValue() != null && kv.getValue() instanceof DateTime) {
                        setValue(recalculateValue());
                        return;
                    } else {
                        Log.w(logTag(), "Source value for calculation must be of type DateTime");
                    }
                    setValue(null);
                }
            }));
        }
    }

    @Override
    protected void onUpdateFieldView(Period value) {
        if (value == null) {
            mPeriodValueButton.setText("Select");
        } else {
            String text = NeoTree.NeoTreeFormat.PERIOD.print(value);
            if (TextUtils.isEmpty(text)) {
                mPeriodValueButton.setText(R.string.label_field_period_too_low);
            } else {
                mPeriodValueButton.setText(text);
            }
        }
        publishValue(value);
    }

    @Override
    protected void onEnabledStateChanged(boolean enabled) {
        mPeriodValueButton.setEnabled(enabled);
    }

    @Override
    protected Period restoreValue() {
        return recalculateValue();
    }

    private Period recalculateValue() {
        if (TextUtils.isEmpty(getField().calculation)) {
            return null;
        }

        // Always recalculate field value
        String cleanedExpression = getField().calculation.replaceAll("[\\s$]*", "");
        String[] keys = cleanedExpression.split("-");

        if (keys.length == 0) {
            Log.w(logTag(), String.format("Invalid expression for period field. Reference value [%s] for period field is null. Ignoring calculation", keys[0]));
            return null;
        } else if (keys.length == 1) {
            DateTime refValue = getScriptPlayer().getValue(keys[0]);
            if (refValue == null) {
                Log.w(logTag(), String.format("Reference value [%s] for period field is null. Ignoring calculation", keys[0]));
                return null;
            }
            return new Period(refValue, DateTime.now());
        } else if (keys.length == 2) {
            DateTime v0 = getScriptPlayer().getValue(keys[0]);
            DateTime v1 = getScriptPlayer().getValue(keys[1]);
            if (v0 == null || v1 == null) {
                Log.w(logTag(), String.format("Reference value [key1=%s, key2=%s]for period field is null. Ignoring calculation", keys[0], keys[1]));
                return null;
            }
            return new Period(v0, v1);
        }
        return null;
    }

    private HashSet<String> buildObserveKeySet(Field field) {
        if (TextUtils.isEmpty(getField().calculation)) {
            return null;
        }

        // Always recalculate field value
        String cleanedExpression = getField().calculation.replaceAll("[\\s$]*", "");
        String[] keys = cleanedExpression.split("-");

        if (keys.length == 0) {
            return null;
        }

        HashSet<String> keySet = new HashSet<>();
        for (int i = 0; i < keys.length; i++) {
            keySet.add(keys[i]);
        }
        return keySet;
    }

    @OnClick(R.id.field_period_button)
    public void onRefreshValue() {
        publishValue(restoreValue());
    }

}
