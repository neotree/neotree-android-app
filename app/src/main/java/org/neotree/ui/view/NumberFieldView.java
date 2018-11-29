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
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;
import android.widget.EditText;

import com.jakewharton.rxbinding.widget.RxTextView;

import org.neotree.R;
import org.neotree.model.firebase.Field;

import butterknife.BindView;
import rx.Observable;
import rx.subjects.BehaviorSubject;

/**
 * Created by matteo on 17/07/2016.
 */
public class NumberFieldView extends FieldView<Double> {

    @BindView(R.id.field_number_input)
    EditText mValueInput;

    private final BehaviorSubject<Boolean> mValidSubject = BehaviorSubject.create();

    private String mFormat;

    public NumberFieldView(Context context) {
        this(context, null);
    }

    public NumberFieldView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NumberFieldView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int getViewLayoutId() {
        return R.layout.view_field_number;
    }

    @Override
    protected void onRegisterSubscribers(Field field) {
        final Double maxValue = (field.maxValue != null) ? Double.parseDouble(field.maxValue) : null;
        final Double minValue = (field.minValue != null) ? Double.parseDouble(field.minValue) : null;

        if (TextUtils.isEmpty(field.format)) {
            // Integer
            mValueInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
            mFormat = "%.0f";
        } else {
            final int decimalDigits = (TextUtils.isEmpty(field.format)) ? 0 : field.format.length();
            mValueInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            mValueInput.setFilters(new InputFilter[] { new DecimalInputFilter(decimalDigits) });
            mFormat = "%." + decimalDigits + "f";
        }

        // Set the formatted value
        mValueInput.setText((getValue() != null) ? String.format(mFormat, getValue()) : null);

        StringBuilder errorBuffer = new StringBuilder("The value must be ");
        if (field.minValue != null) {
            errorBuffer.append(String.format("greater than " + mFormat, minValue));
        }
        if (field.minValue != null && field.maxValue != null) {
            errorBuffer.append(" and ");
        }
        if (field.maxValue != null) {
            errorBuffer.append(String.format("lower than " + mFormat , maxValue));
        }

        final String errorMessage = errorBuffer.toString();
        setErrorText(errorMessage);

        // Hide/show error message
        Observable<Boolean> validRangeObservable = valueObservable().map((value) ->
                (value == null || ((minValue == null || minValue <= value )
                        && (maxValue == null || value <= maxValue )))
        );

        addSubscription(validRangeObservable.subscribe((valid) -> {
            showError(!valid);
            mValidSubject.onNext(valid);
        }));

        // Publish value
        addSubscription(RxTextView.textChanges(mValueInput)
                .map((input) -> {
                    if (input.length() > 0) {
                        try {
                            return Double.parseDouble(input.toString());
                        } catch (NumberFormatException e) {
                            // Trap and ignore
                        }
                    }
                    return null;
                })
                .subscribe(this::publishValue)
        );
    }

    @Override
    protected void onUpdateFieldView(Double value) {
        mValueInput.setText((value != null) ? value.toString() : null);
    }

    @Override
    protected void onEnabledStateChanged(boolean enabled) {
        mValueInput.setEnabled(enabled);
    }

    @Override
    public Observable<Boolean> validationObservable() {
        return mValidSubject;
    }

    public String getFormat() {
        return mFormat;
    }

    private class DecimalInputFilter extends DigitsKeyListener {

        private int mMaxDecimalDigits;

        public DecimalInputFilter(int maxDecimalDigits) {
            super(false, true);
            mMaxDecimalDigits = maxDecimalDigits;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            // if changed, replace the source
            CharSequence out = super.filter(source, start, end, dest, dstart, dend);
            if (out != null) {
                source = out;
                start = 0;
                end = out.length();
            }

            int len = end - start;

            // if deleting, source is empty and deleting can't break anything
            if (len == 0) {
                return source;
            }

            int dlen = dest.length();

            // Find the position of the decimal .
            for (int i = 0; i < dstart; i++) {
                if (dest.charAt(i) == '.') {
                    // being here means, that a number has been inserted after the dot check if the amount of digits is right
                    return (dlen-(i+1) + len > mMaxDecimalDigits) ? "" : null;
                }
            }

            for (int i = start; i < end; ++i) {
                if (source.charAt(i) == '.') {
                    // being here means, dot has been inserted check if the amount of digits is right
                    if ((dlen-dend) + (end-(i + 1)) > mMaxDecimalDigits) {
                        return "";
                    } else {
                        return null;
                    }
                }
            }
            return null;
        }
    }
}
