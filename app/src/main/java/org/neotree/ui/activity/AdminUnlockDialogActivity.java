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

package org.neotree.ui.activity;

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.mindrot.jbcrypt.BCrypt;
import org.neotree.R;
import org.neotree.inject.ActivityComponent;
import org.neotree.inject.module.ActivityModule;
import org.neotree.model.realm.AdminPassword;
import org.neotree.support.datastore.RealmStore;
import org.neotree.ui.core.EnhancedActivity;

import butterknife.BindView;
import butterknife.OnClick;

public class AdminUnlockDialogActivity extends EnhancedActivity<ActivityComponent> {

    @BindView(R.id.admin_unlock_password_field)
    EditText mPasswordField;
    @BindView(R.id.admin_unlock_error)
    TextView mErrorText;
    @BindView(R.id.admin_unlock_cancel_button)
    Button mCancelButton;
    @BindView(R.id.admin_unlock_unlock_button)
    Button mUnlockButton;

    @Override
    protected int getActivityViewId() {
        return R.layout.activity_admin_unlock_dialog;
    }

    @Override
    protected ActivityComponent setupActivityComponent() {
        return getApplicationComponent().plus(new ActivityModule(this));
    }

    @OnClick(R.id.admin_unlock_cancel_button)
    void onCancelButtonClick() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @OnClick(R.id.admin_unlock_unlock_button)
    void onUnlockButtonClick() {
        String password = mPasswordField.getText().toString();
        if (!TextUtils.isEmpty(password)) {
            AdminPassword stored = RealmStore.loadAdminPassword(getRealm());
            if (BCrypt.checkpw(password, stored.getPassword())) {
                setResult(RESULT_OK);
                finish();
            } else {
                mErrorText.setVisibility(View.VISIBLE);
            }
        }
    }

}

