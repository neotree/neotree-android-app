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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.database.Query;

import org.neotree.NeoTree;
import org.neotree.R;
import org.neotree.inject.ActivityComponent;
import org.neotree.inject.module.ActivityModule;
import org.neotree.model.firebase.ConfigKey;
import org.neotree.support.android.SimpleDividerItemDecoration;
import org.neotree.support.datastore.FirebaseStore;
import org.neotree.support.firebase.FirebaseRecyclerAdapter;
import org.neotree.ui.core.ButterknifeViewHolder;
import org.neotree.ui.core.EnhancedActivity;

import butterknife.BindView;

public class ConfigurationActivity extends EnhancedActivity<ActivityComponent> {

    @BindView(R.id.configuration_toolbar)
    Toolbar mToolbar;
    @BindView(R.id.configuration_recyclerview) RecyclerView mRecyclerView;

    private ConfigurationListViewAdapter mListAdapter;

    @Override
    protected int getActivityViewId() {
        return R.layout.activity_configuration;
    }

    @Override
    protected ActivityComponent setupActivityComponent() {
        return getApplicationComponent().plus(new ActivityModule(this));
    }

    @Override
    public void onCreateAfterSetContentView(Bundle savedInstanceState) {
        super.onCreateAfterSetContentView(savedInstanceState);

        setSupportActionBar(mToolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        mListAdapter = new ConfigurationListViewAdapter(this, FirebaseStore.get().queryConfigKeys(), ConfigKey.class);

        mRecyclerView.setAdapter(mListAdapter);
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(this));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onEventReceived(Object event) {
//        if (event instanceof PreviousScreenEvent) {
//            Toast.makeText(this, "Show previous screen", Toast.LENGTH_SHORT).show();
//        } else if (event instanceof NextScreenEvent) {
//            showNextScreen();
//        }
    }

    private static class ConfigurationListViewAdapter extends FirebaseRecyclerAdapter<ConfigItemViewHolder, ConfigKey> {

        private SharedPreferences mConfigPrefs;

        public ConfigurationListViewAdapter(Context context, Query query, Class<ConfigKey> itemClass) {
            super(query, itemClass);
            mConfigPrefs = NeoTree.getConfigurationPreferences(context);
        }

        @Override
        public ConfigItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new ConfigItemViewHolder(
                    inflater.inflate(R.layout.view_screen_checklist_item, parent, false)
            );
        }

        @Override
        public void onBindViewHolder(ConfigItemViewHolder holder, int position) {
            final ConfigKey item = getItem(position);
            holder.setTag(position);

            holder.title.setText(item.label);
            if (!TextUtils.isEmpty(item.summary)) {
                holder.summary.setText(item.summary);
                holder.summary.setVisibility(View.VISIBLE);
            } else {
                holder.summary.setVisibility(View.GONE);
            }
            holder.setTag(position);

            boolean isChecked = mConfigPrefs.getBoolean(item.configKey, false);
            holder.toggle.setOnCheckedChangeListener(null);
            holder.toggle.setChecked(isChecked);
            holder.toggle.setOnCheckedChangeListener((compoundButton, checked) -> {
                mConfigPrefs.edit()
                        .putBoolean(item.configKey, checked)
                        .apply();
            });
        }

    }

    static class ConfigItemViewHolder extends ButterknifeViewHolder {
        @BindView(R.id.checklist_item_title) public TextView title;
        @BindView(R.id.checklist_item_summary) public TextView summary;
        @BindView(R.id.checklist_item_switch) public SwitchCompat toggle;

        public ConfigItemViewHolder(View view) {
            super(view);
            view.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
        }

        public void setTag(Object o) {
            toggle.setTag(o);
        }
    }
}

