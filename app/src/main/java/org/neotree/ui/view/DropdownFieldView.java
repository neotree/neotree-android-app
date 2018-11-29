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
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.Button;

import org.neotree.R;
import org.neotree.model.firebase.Field;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by matteo on 17/07/2016.
 */
public class DropdownFieldView extends FieldView<String> {

    @BindView(R.id.field_dropdown_button)
    Button mValueButton;

    private ArrayList<String> mValueIds;
    private ArrayList<CharSequence> mValueLabels;

    public DropdownFieldView(Context context) {
        this(context, null);
    }

    public DropdownFieldView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DropdownFieldView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int getViewLayoutId() {
        return R.layout.view_field_dropdown;
    }

    @Override
    protected void onRegisterSubscribers(Field field) {

    }

    @Override
    protected void onUpdateFieldView(String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }

        int index = getCheckedItemIndex(value);
        if (index == -1) {
            throw new IllegalStateException(
                    String.format("Invalid value \"%s\" set for dropdown \"%s\"", value, getField().label));
        }
        setValueByIndex(index);
    }

    @Override
    protected void onEnabledStateChanged(boolean enabled) {
        mValueButton.setEnabled(enabled);
    }

    public void setValues(String values) {
        if (TextUtils.isEmpty(values)) {
            throw new IllegalStateException(
                    String.format("Values MUST be set for the dropdown field \"%s\"", getField().label));
        }

        mValueIds = new ArrayList<>();
        mValueLabels = new ArrayList<>();

        try {
            BufferedReader reader = new BufferedReader(new StringReader(values));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] item = line.split(",");
                mValueIds.add(item[0].trim());
                mValueLabels.add(item[1].trim());
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    String.format("The dropdown field \"%s\" contains invalid values", getField().label), e);
        }
    }

    public ArrayList<String> getValueIds() {
        return mValueIds;
    }

    public ArrayList<CharSequence> getValueLabels() {
        return mValueLabels;
    }

    @OnClick(R.id.field_dropdown_button)
    public void showDropdownMenu() {
        CharSequence[] items = new CharSequence[mValueLabels.size()];
        mValueLabels.toArray(items);

        int itemIndex = getCheckedItemIndex(getValue());

        final AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.label_field_dropdown_title)
                .setSingleChoiceItems(items, itemIndex, (d, index) -> {
                    setValueByIndex(index);
                    d.dismiss();
                })
                .setNegativeButton(R.string.label_action_cancel, (d, i) -> d.dismiss())
                .create();
        dialog.show();
    }

    private void setValueByIndex(int index) {
        String value = mValueIds.get(index);
        mValueButton.setText(mValueLabels.get(index));
        publishValue(value);
    }

    private int getCheckedItemIndex(String value) {
        int index = -1;
        if (!TextUtils.isEmpty(value)) {
            for (int i = 0; i < mValueIds.size(); i++) {
                if (mValueIds.get(i).equals(value)) {
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

}
