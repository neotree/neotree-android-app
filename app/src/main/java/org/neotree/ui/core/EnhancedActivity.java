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

package org.neotree.ui.core;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;

import org.neotree.NeoTree;
import org.neotree.R;
import org.neotree.inject.ApplicationComponent;
import org.neotree.support.realm.EncryptionKeyStore;
import org.neotree.support.rx.RxBus;
import org.neotree.support.rx.RxHelper;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.realm.Realm;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by matteo on 14/07/2016.
 */
public abstract class EnhancedActivity<C> extends AppCompatActivity {

    private static AtomicInteger sAtomicInteger = new AtomicInteger();

    private String mViewIdentity = String.format("activity-%d", sAtomicInteger.incrementAndGet());

    @BindView(R.id.activity_coordinator_layout) @Nullable
    CoordinatorLayout mCoordinatorLayout;

    @Inject
    RxBus mRxBus;

    private C mActivityComponent;

    private CompositeSubscription mSubscription;
    private Realm mRealm;

    protected abstract int getActivityViewId();
    protected abstract C setupActivityComponent();

    @SuppressWarnings("unchecked")
    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initializeRealm();

        // Set activity content view
        int viewId = getActivityViewId();
        if (viewId != 0) {
            // Initialize the injector
            Log.v(logTag(), "Setting up activity component");
            mActivityComponent = setupActivityComponent();

            // Inject the activity
            if (mActivityComponent != null) {
                Log.v(logTag(), "Injecting dependencies");
                injectActivity();
            }

            Log.v(logTag(), "Triggering onCreateBeforeSetContentView()");
            onCreateBeforeSetContentView(savedInstanceState);

            Log.v(logTag(), "Setting activity content view");
            setContentView(viewId);

            Log.v(logTag(), "Binding activity content view");
            ButterKnife.bind(this);

            Log.v(logTag(), "Triggering onCreateAfterSetContentView()");
            onCreateAfterSetContentView(savedInstanceState);

        } else {
            Log.e(logTag(), "Invalid content view for activity");
            throw new RuntimeException("Invalid content view for activity");
        }
    }

    private void injectActivity() {
        Object injector = getActivityComponent();
        try {
            Method injectMethod = injector.getClass().getMethod("inject", getClass());
            injectMethod.invoke(injector, this);
        } catch (Exception e) {
            Log.e(logTag(), "Error injecting activity dependencies", e);
        }
    }

    public void onCreateBeforeSetContentView(Bundle savedInstanceState) {
        // Do nothing by default
    }

    public void onCreateAfterSetContentView(Bundle savedInstanceState) {
        // Do nothing by default
    }

    @Override
    protected void onResume() {
        Log.v(logTag(), "onResume()");
        super.onResume();

        Log.v(logTag(), "Subscribing presenter to the event bus");
        addSubscription(getEventBus().observable()
                .compose(RxHelper.applySchedulers())
                .subscribe(this::onEventReceived)
        );
    }

    @Override
    protected void onPause() {
        Log.v(logTag(), "onPause()");
        super.onPause();

        Log.v(logTag(), "Removing all subscriptions");
        if (mSubscription != null && mSubscription.hasSubscriptions()
                && !mSubscription.isUnsubscribed()) {
            mSubscription.unsubscribe();
        }
        mSubscription = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.v(logTag(), "Close realm instance");
        if (mRealm != null && !mRealm.isClosed()) {
            mRealm.close();
        }
    }

    protected void addSubscription(Subscription subscription) {
        if (mSubscription == null) {
            mSubscription = new CompositeSubscription();
        }
        mSubscription.add(subscription);
    }

    protected void removeSubscription(Subscription subscription) {
        mSubscription.remove(subscription);
    }

    protected void onEventReceived(Object event) {
        // Do nothing by default
    }

    protected void applyMenuItemTint(Menu menu) {
//        // FIXME: Find a way to fix this properly...
//        int iconColor = ResourcesCompat.getColor(getResources(), R.color.colorMainToolbarForeground, getTheme());
//        for (int i = 0; i < menu.size(); i++) {
//            MenuItem item = menu.getItem(i);
//            Drawable icon = item.getIcon();
//            if (icon != null) {
//                Drawable menuIcon = icon.mutate();
//                menuIcon.setColorFilter(new PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN));
//                item.setIcon(menuIcon);
//            }
//        }
    }

    public final ApplicationComponent getApplicationComponent() {
        return ((NeoTree) getApplication()).getComponent();
    }

    public void initializeRealm() {
        Log.d(logTag(), "Creating realm instance");
        try {
            mRealm = Realm.getDefaultInstance();
        } catch (Exception e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle(R.string.label_realm_error_title)
                    .setMessage(R.string.label_realm_error_message)
                    .setPositiveButton(R.string.label_realm_error_action, (d, which) -> {
                      //  EncryptionKeyStore.reset(EnhancedActivity.this);
                        Realm.deleteRealm(((NeoTree) getApplication()).getRealmConfiguration());
                        finish();
                    });
            builder.create().show();
        }
    }

    @Nullable
    public CoordinatorLayout getCoordinatorLayout() {
        return mCoordinatorLayout;
    }

    public final C getActivityComponent() {
        return mActivityComponent;
    }

    public RxBus getEventBus() {
        return mRxBus;
    }

    public synchronized Realm getRealm() {
        return mRealm;
    }

    public boolean isLiveReloadEnabled() {
        return getResources().getBoolean(R.bool.enable_live_editing);
    }

    public String logTag() {
        return String.format("%s [%s]", getClass().getSimpleName(), mViewIdentity);
    }

}
