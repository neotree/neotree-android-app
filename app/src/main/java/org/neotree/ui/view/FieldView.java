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
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.neotree.R;
import org.neotree.model.firebase.Field;
import org.neotree.player.ScriptPlayer;
import org.neotree.support.datastore.RealmStore;

import java.util.concurrent.atomic.AtomicInteger;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscription;
import rx.subjects.BehaviorSubject;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by matteo on 02/08/2016.
 */
public abstract class FieldView<V> extends FrameLayout {

    public interface ValueAdapter<T> {
        T onReadValue(T value);
        T onWriteValue(T value);
    }

    public static class DefaultValueAdapter<T> implements ValueAdapter<T> {
        @Override
        public T onReadValue(T value) {
            return value;
        }

        @Override
        public T onWriteValue(T value) {
            return value;
        }
    }

    private static AtomicInteger sAtomicInteger = new AtomicInteger();
    private String mViewIdentity = String.format("field-%d", sAtomicInteger.incrementAndGet());

    private final BehaviorSubject<V> mValueSubject = BehaviorSubject.create();

    @BindView(R.id.field_label)
    TextView mLabelText;
    @BindView(R.id.field_error)
    TextView mErrorText;

    private ValueAdapter<V> mValueAdapter = new DefaultValueAdapter<>();
    private V mValue;

    private CompositeSubscription mSubscription;
    private ScriptPlayer mScriptPlayer;

    private Field mField;

    public FieldView(Context context) {
        this(context, null);
    }

    public FieldView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FieldView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // Inflate layout
        inflate(getContext(), getViewLayoutId(), this);

        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));


        Log.v(logTag(), "Binding field view");
        ButterKnife.bind(this, this);
    }

    protected abstract int getViewLayoutId();
    protected abstract void onRegisterSubscribers(Field field);
    protected abstract void onUpdateFieldView(V value);
    protected abstract void onEnabledStateChanged(boolean enabled);

    public Observable<V> valueObservable() {
        return mValueSubject;
    }

    public Observable<Boolean> validationObservable() {
        return Observable.just(true);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (isInEditMode()) {
            return;
        }

        V value = null;
        if (mScriptPlayer != null) {
            Log.v(logTag(), String.format("Restoring field value [key=%s]", mField.key));
            value = restoreValue();
            Log.v(logTag(), String.format("Restored value [key=%s, value=%s]", mField.key, value));
        }
        setValue(mValueAdapter.onReadValue(value));

        Log.v(logTag(), String.format("Registering subscriptions [key=%s]", mField.key));
        onRegisterSubscribers(getField());
    }

    @Override
    protected void onDetachedFromWindow() {
        Log.v(logTag(), String.format("onDetachedFromWindow() [key=%s]", mField.key));
        super.onDetachedFromWindow();

        Log.v(logTag(), String.format("Removing all subscriptions [key=%s]", mField.key));
        if (mSubscription != null && mSubscription.hasSubscriptions()
                && !mSubscription.isUnsubscribed()) {
            mSubscription.unsubscribe();
        }
        mSubscription = null;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Log.v(logTag(), String.format("onSaveInstanceState() [key=%s]", mField.key));
        return super.onSaveInstanceState();
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Log.v(logTag(), String.format("onRestoreInstanceState() [key=%s]", mField.key));
        super.onRestoreInstanceState(state);
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

    public String logTag() {
        return String.format("%s [%s]", getClass().getSimpleName(), mViewIdentity);
    }

    public Field getField() {
        return mField;
    }

    public void setField(Field field) {
        mField = field;

        if (mField != null) {
            mLabelText.setText(mField.label);
        }
    }

    protected V restoreValue() {
        return mScriptPlayer.getValue(mField.key);
    }

    protected void publishValue(V value) {
        if (TextUtils.isEmpty(mField.key)) {
            throw new IllegalStateException("A key MUST be set for each field");
        }

        V storeValue = mValueAdapter.onWriteValue(value);

        mValue = storeValue;
        mValueSubject.onNext(storeValue);

        if (mScriptPlayer != null) {
            Log.v(logTag(), String.format("Publishing value [key=%s]", mField.key));
            mScriptPlayer.setValue(mField.key, storeValue);
            mScriptPlayer.storeValue(getScriptPlayer().currentScreen().sectionTitle, RealmStore.getSessionValue(mField, storeValue));
        }
    }

    protected ScriptPlayer getScriptPlayer() {
        return mScriptPlayer;
    }

    public void setScriptPlayer(ScriptPlayer scriptPlayer) {
        mScriptPlayer = scriptPlayer;
    }

    public void setValueAdapter(ValueAdapter<V> valueAdapter) {
        mValueAdapter = (valueAdapter != null) ? valueAdapter : new DefaultValueAdapter<>();
    }

    public void setErrorText(String errorText) {
        mErrorText.setText(errorText);
    }

    public void showError(boolean show) {
        mErrorText.setVisibility((show) ? VISIBLE : GONE);
    }

    public String getLabel() {
        return mLabelText.getText().toString();
    }

    public void setLabel(String label) {
        mLabelText.setText(label);
    }

    public V getValue() {
        return mValue;
    }

    public void setValue(V value) {
        mValue = value;
        mValueSubject.onNext(value);
        onUpdateFieldView(value);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mLabelText.setEnabled(enabled);
        onEnabledStateChanged(enabled);
    }

}
