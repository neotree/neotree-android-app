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
import android.util.AttributeSet;
import android.widget.EditText;

import com.jakewharton.rxbinding.widget.RxTextView;

import org.neotree.R;
import org.neotree.model.firebase.Field;
import org.neotree.player.type.DefaultValueType;
import org.neotree.support.SessionHelper;

import butterknife.BindView;

/**
 * Created by matteo on 17/07/2016.
 */
public class TextFieldView extends FieldView<CharSequence> {

    @BindView(R.id.field_text_input)
    EditText mValueInput;

    public TextFieldView(Context context) {
        this(context, null);
    }

    public TextFieldView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextFieldView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int getViewLayoutId() {
        return R.layout.view_field_text;
    }

    @Override
    protected void onRegisterSubscribers(Field field) {
        // Publish value
        addSubscription(RxTextView.textChanges(mValueInput)
                .map((input) -> (input.length() > 0) ? input.toString() : null)
                .subscribe(this::publishValue)
        );
    }

    @Override
    protected void onUpdateFieldView(CharSequence value) {
        mValueInput.setText(value);
    }

    @Override
    protected void onEnabledStateChanged(boolean enabled) {
        mValueInput.setEnabled(enabled);
    }

    @Override
    protected CharSequence restoreValue() {
        CharSequence value = super.restoreValue();
        if (value == null && getField().defaultValue != null) {
            DefaultValueType type = DefaultValueType.fromString(getField().defaultValue);
            switch (type) {
                case UID:
                    value = SessionHelper.uid(getContext(), getField().key);
                    break;
            }
        }
        return value;
    }

}
