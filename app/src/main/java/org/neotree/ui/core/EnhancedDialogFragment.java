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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import org.neotree.support.rx.RxBus;
import org.neotree.support.rx.RxHelper;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.realm.Realm;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by Matteo Giaccone on 08/10/15.
 */
public abstract class EnhancedDialogFragment extends DialogFragment {

    private static AtomicInteger sAtomicInteger = new AtomicInteger();

    private String mViewIdentity = String.format("fragment-%d", sAtomicInteger.incrementAndGet());

    private CompositeSubscription mSubscription;

    protected abstract int getFragmentViewId();

    private Unbinder mUnbinder;

    @SuppressWarnings("unchecked")
    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v(logTag(), "onCreateView()");

        int viewId = getFragmentViewId();
        if (viewId != 0) {
            // Inject fragment
            Log.v(logTag(), "Injecting dependencies");
            injectFragment();

            Log.v(logTag(), "Inflating fragment layout");
            View view = inflater.inflate(viewId, container, false);

            Log.v(logTag(), "Binding fragment view");
            mUnbinder = ButterKnife.bind(this, view);

            Log.v(logTag(), "Subscribing to the event bus");
            addSubscription(getEventBus().observable()
                    .compose(RxHelper.applySchedulers())
                    .subscribe(this::onEventReceived)
            );

            Log.v(logTag(), "Calling onFragmentViewCreated()");
            onFragmentViewCreated(view, savedInstanceState);

            return view;
        } else {
            Log.e(logTag(), "Invalid view for fragment");
            throw new RuntimeException("Invalid content view for activity");
        }
    }

    private void injectFragment() {
        Object injector = ((EnhancedActivity<?>) getActivity()).getActivityComponent();
        try {
            Method injectMethod = injector.getClass().getMethod("inject", getClass());
            injectMethod.invoke(injector, this);
        } catch (Exception e) {
            Log.e(logTag(), "Error injecting fragment dependencies", e);
        }
    }

    public void onFragmentViewCreated(View view, Bundle savedInstanceState) {
        // Do nothing by default
    }

    @TargetApi(23)
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onAttachToContext(context);
    }

    /*
     * Deprecated on API 23
     * Use onAttachToContext instead
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (Build.VERSION.SDK_INT < 23) {
            onAttachToContext(activity);
        }
    }

    /*
     * This method will be called from one of the two previous method
     */
    protected void onAttachToContext(Context context) {

    }

    @Override
    public void onDestroyView() {
        Log.v(logTag(), "onDestroyView()");
        mUnbinder.unbind();

        Log.v(logTag(), "Removing all subscriptions");
        if (mSubscription != null && mSubscription.hasSubscriptions()
                && !mSubscription.isUnsubscribed()) {
            mSubscription.unsubscribe();
        }
        mSubscription = null;

        super.onDestroyView();
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

    protected EnhancedActivity<?> getEnhancedActivity() {
        return (EnhancedActivity<?>) getActivity();
    }

    protected void applyMenuItemTint(Menu menu) {
        getEnhancedActivity().applyMenuItemTint(menu);
    }

    public boolean isLiveReloadEnabled() {
        return getEnhancedActivity().isLiveReloadEnabled();
    }

    public RxBus getEventBus() {
        return getEnhancedActivity().getEventBus();
    }

    public Realm getRealm() {
        return getEnhancedActivity().getRealm();
    }

    public String logTag() {
        return String.format("%s [%s]", getClass().getSimpleName(), mViewIdentity);
    }

}
