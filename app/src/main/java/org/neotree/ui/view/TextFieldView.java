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
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jakewharton.rxbinding.widget.RxTextView;

import org.neotree.R;
import org.neotree.model.firebase.Field;
import org.neotree.player.type.DefaultValueType;
import org.neotree.support.SessionHelper;

import butterknife.BindView;

import rx.Observable;
import rx.subjects.BehaviorSubject;

/**
 * Created by matteo on 17/07/2016.
 */
public class TextFieldView extends FieldView<CharSequence> {
    public CharSequence value;

    @BindView(R.id.field_text_input)
    EditText mValueInput;

    private final BehaviorSubject<Boolean> mValidSubject = BehaviorSubject.create();

    public TextFieldView(Context context) {
        this(context, null);
    }

    public TextFieldView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextFieldView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mValueInput.addTextChangedListener(new TextWatcher() {
            Boolean editing = false;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                String value = editable.toString();
                value = parseNUID(value);
                if (editing) return;
                editing = true;
                editable.clear();
                editable.insert(0, value);
                editing = false;
            }
        });
    }

    public String parseNUID(String s) {
        if (!(getField().key.equals("NUID_S") || getField().key.equals("NUID_NS"))) return s;

        s = s.replaceAll("[^a-fA-F0-9]", "");
        String letters = "";
        String digits = "";

        for(int i = 0; i < s.length(); i++) {
            if (i < 8) {
                String letter = Character.toString(s.charAt((i)));
                if (i < 4) {
                    letters = letters + letter;
                } else {
                    digits = digits + letter;
                    digits = digits.replaceAll("\\D", "");
                }
            }
        }
        s = letters + (digits.length() > 0 ? "-" : "") + digits;
        s = s.toUpperCase();
        return s;
    }

    @Override
    protected int getViewLayoutId() {
        return R.layout.view_field_text;
    }

    @Override
    protected void onRegisterSubscribers(Field field) {
        String errorMessage = "NeoTree ID must be 9 characters, eg AB2C-9756";

        // Hide/show error message
        Observable<Boolean> validRangeObservable = valueObservable().map((value) -> {
            if (!TextUtils.isEmpty(value) &&
                    (field.key.equals("NUID_S") || field.key.equals("NUID_NS"))) {
                String v = (String) value;
                if (v.length() < 9) return false;
            }
            return true;
        });

        setErrorText(errorMessage);

        addSubscription(validRangeObservable.subscribe((valid) -> {
            showError(!valid);
            mValidSubject.onNext(valid);
        }));

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
        value = super.restoreValue();
        if (value == null && getField().defaultValue != null) {
            DefaultValueType type = DefaultValueType.fromString(getField().defaultValue);
            switch (type) {
                case UID:
                    value = SessionHelper.uid(getContext(), getField().key);





            }
        }
        return value;
    }

}
