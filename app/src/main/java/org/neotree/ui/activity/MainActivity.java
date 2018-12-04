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

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.Query;
import com.squareup.seismic.ShakeDetector;

import org.neotree.NeoTree;
import org.neotree.R;
import org.neotree.event.PlayScriptEvent;
import org.neotree.inject.ActivityComponent;
import org.neotree.inject.module.ActivityModule;
import org.neotree.model.firebase.Script;
import org.neotree.support.android.AndroidHelper;
import org.neotree.support.android.VerticalSpacingItemDecoration;
import org.neotree.support.datastore.FirebaseStore;
import org.neotree.support.firebase.FirebaseRecyclerAdapter;
import org.neotree.support.rx.RxBus;
import org.neotree.support.rx.RxFirebase;
import org.neotree.support.rx.RxHelper;
import org.neotree.ui.core.ButterknifeViewHolder;
import org.neotree.ui.core.EnhancedActivity;

import java.util.Random;

import butterknife.BindView;

public class MainActivity extends EnhancedActivity<ActivityComponent>
        implements NavigationView.OnNavigationItemSelectedListener, ShakeDetector.Listener {

    private static final int PLAY_SERVICES_REQUEST = 9000;
    private static final int LOGIN_REQUEST = 9001;

    @BindView(R.id.activity_coordinator_layout)
    CoordinatorLayout mCoordinatorLayout;
    @BindView(R.id.main_toolbar)
    Toolbar mToolbar;
    @BindView(R.id.main_drawer_layout)
    DrawerLayout mDrawer;
    @BindView(R.id.main_nav_view) NavigationView mNavigationView;
    @BindView(R.id.main_recycler_view) RecyclerView mRecyclerView;

    //private FirebaseAuth mFirebaseAuth;

    private ScriptListViewAdapter mListAdapter;
    private SensorManager mSensorManager;
    private ShakeDetector mShakeDetector;
    private AlertDialog mInfoDialog;

    @Override
    protected int getActivityViewId() {
        return org.neotree.R.layout.activity_main;
    }

    @Override
    protected ActivityComponent setupActivityComponent() {
        return getApplicationComponent().plus(new ActivityModule(this));
    }

    @Override
    public void onCreateAfterSetContentView(Bundle savedInstanceState) {
        super.onCreateAfterSetContentView(savedInstanceState);

        setTitle(R.string.title_activity_main);


        setSupportActionBar(mToolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawer, mToolbar,
                org.neotree.R.string.navigation_drawer_open, org.neotree.R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();

        mNavigationView.setNavigationItemSelectedListener(this);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new VerticalSpacingItemDecoration(this));

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mShakeDetector = new ShakeDetector(this);
    }

    private void registerListAdapter() {
        if (mListAdapter == null) {
            mListAdapter = new ScriptListViewAdapter(getEventBus(), FirebaseStore.get().queryScripts(), Script.class);
            mRecyclerView.setAdapter(mListAdapter);
        }
    }

    private void unregisterListAdapter() {
        if (mListAdapter != null) {
            mListAdapter.destroy();
            mListAdapter = null;
        }
        mRecyclerView.setAdapter(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(logTag(), "Refreshing activity data and signed in status");
        initialize();
        mShakeDetector.start(mSensorManager);
    }

    @Override
    protected void onPause() {
        Log.d(logTag(), "Unregistering list adapter");
        unregisterListAdapter();
        mShakeDetector.stop();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebaseStore.get().destroy();
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.action_nav_configuration) {
            showConfigurationActivity();
        } else if (id == R.id.action_nav_session_history) {
            showSessionHistoryActivity();
        } else if (id == R.id.action_nav_logout) {
            FirebaseStore.get().destroy();
            addSubscription(
                RxFirebase.signOut(FirebaseAuth.getInstance())
                        .compose(RxHelper.applySchedulers())
                        .subscribe()
            );
        }
        mDrawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onEventReceived(Object event) {
        if (event instanceof PlayScriptEvent) {
            showScriptPlayerActivity(((PlayScriptEvent) event).script);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOGIN_REQUEST) {
            if (resultCode == RESULT_OK) {
                Log.d(logTag(), "Login success, showing main content");
                initialize();
            } else {
                Log.d(logTag(), "Login failed, finishing main activity");
                finish();
            }
        } else if (requestCode == PLAY_SERVICES_REQUEST) {
            if (resultCode == RESULT_OK) {
                Log.d(logTag(), "Google Play Services update success");
                // TODO: Handle this as Google apps with close app and notify
                initialize();
            } else {
                Log.d(logTag(), "Google Play Services update failed");
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void hearShake() {
        if (mInfoDialog == null) {
            try {
                PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                mInfoDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.label_main_appinfo_title)
                        .setMessage(getResources().getString(R.string.label_main_appinfo_message,
                                pInfo.packageName, pInfo.versionName, pInfo.versionCode))
                        .setPositiveButton(R.string.label_action_ok, (d, which) -> {
                            d.dismiss();
                            mInfoDialog = null;
                        })
                        .create();
                mInfoDialog.show();
            } catch (Exception e) {
                Log.w(logTag(), "Error getting app info");
            }
        }
    }

    private void initialize() {
        // Check play services are installed
        if (checkPlayServices()) {
            addSubscription(
                    RxFirebase.observeAuthChange(FirebaseAuth.getInstance())
                            .compose(RxHelper.applySchedulers())
                            .subscribe(authResult -> {
                                FirebaseUser user = authResult.getCurrentUser();
                                if (user == null) {
                                    // User is signed out
                                    Log.d(logTag(), "onAuthStateChanged:signed_out");
                                    showLoginActivity();
                                    return;
                                }
                                // User is signed in
                                // TODO: Disable for release ???
                              //  Crashlytics.setUserIdentifier(user.getUid());
                              //  Crashlytics.setUserEmail(user.getEmail());

                                if (!FirebaseStore.get().isInitialized()) {
                                    FirebaseStore.get().initialize();
                                }
                                Log.d(logTag(), "onAuthStateChanged:signed_in");
                                registerListAdapter();
                            })
            );
        }
    }

    private void showLoginActivity() {
        AndroidHelper.startActivityForResult(this, LoginActivity.class, LOGIN_REQUEST, false);
    }

    private void showConfigurationActivity() {
        AndroidHelper.startActivity(this, ConfigurationActivity.class, false);
    }

    private void showSessionHistoryActivity() {
        AndroidHelper.startActivity(this, SessionHistoryActivity.class, false);
    }

    private void showScriptPlayerActivity(Script script) {
        Bundle args = new Bundle();
        args.putParcelable(NeoTree.EXTRA_SCRIPT, script);

        AndroidHelper.startActivity(this, ScriptPlayerActivity.class, false, args);
    }

    private boolean checkPlayServices() {
        Log.d(logTag(), "Checking Google Play Services availability");
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                Dialog dialog = apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_REQUEST);
                dialog.setOnDismissListener(dialog1 -> finish());
                dialog.show();
            } else {
                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setMessage("This device is not supported")
                        .setPositiveButton(R.string.label_action_close, (d, i) -> {
                            finish();
                        })
                        .create();
                dialog.show();
            }
            return false;
        }
        return true;
    }

    static class ScriptListViewAdapter extends FirebaseRecyclerAdapter<ScriptViewHolder, Script> implements View.OnClickListener {

        private RxBus mEventBus;

        public ScriptListViewAdapter(RxBus eventBus, Query query, Class<Script> itemClass) {
            super(query, itemClass);
            mEventBus = eventBus;
        }

        @Override
        public ScriptViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new ScriptViewHolder(inflater.inflate(R.layout.view_script_item, parent, false), this);
        }

        @Override
        public void onBindViewHolder(ScriptViewHolder holder, int position) {
            holder.setTag(position);

            final Script script = getItem(position);
            holder.scriptTitle.setText(script.title);
            holder.scriptDescription.setText(script.description);
        }

        @Override
        public void onClick(View v) {
            final int scriptIndex = (int) v.getTag();
            final Script script = getItem(scriptIndex);
            mEventBus.send(new PlayScriptEvent(script));
        }

    }

    static class ScriptViewHolder extends ButterknifeViewHolder {
        @BindView(R.id.script_item_wrapper) public LinearLayout wrapper;
        @BindView(R.id.script_item_title) public TextView scriptTitle;
        @BindView(R.id.script_item_description) public TextView scriptDescription;

        public ScriptViewHolder(View view, View.OnClickListener clickListener) {
            super(view);
            wrapper.setOnClickListener(clickListener);
        }

        public void setTag(Object o) {
            wrapper.setTag(o);
        }
    }

}
