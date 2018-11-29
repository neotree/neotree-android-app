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

package org.neotree.ui.fragment;

import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.ColorRes;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.neotree.NeoTree;
import org.neotree.R;
import org.neotree.model.firebase.Field;
import org.neotree.model.firebase.Metadata;
import org.neotree.model.firebase.Screen;
import org.neotree.support.rx.RxHelper;
import org.neotree.ui.view.FieldView;
import org.neotree.ui.view.NumberFieldView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import rx.Observable;
import rx.Subscription;

/**
 * Created by matteo on 14/07/2016.
 */
public class TimerScreenFragment extends AbstractScreenFragment {

    public static TimerScreenFragment newInstance(Screen screen) {
        Bundle args = new Bundle();
        args.putParcelable(NeoTree.EXTRA_SCREEN, screen);

        TimerScreenFragment fragment = new TimerScreenFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private static final long VIBRATOR_PATTERN[] = { 500, 250, 500, 250 };

    @BindView(R.id.screen_content_text)
    TextView mContentText;
    @BindView(R.id.screen_timer_layout)
    LinearLayout mTimerLayout;
    @BindView(R.id.screen_timer_text)
    TextView mTimerText;
    @BindView(R.id.screen_timer_message)
    TextView mTimerMessageText;
    @BindView(R.id.screen_timer_value_input) NumberFieldView mValueInput;
    @BindView(R.id.screen_timer_multiplier)
    TextView mMultiplierText;
    @BindView(R.id.screen_timer_result_text)
    TextView mResultText;

    private Subscription mTimerSubscription;
    private Subscription mAlarmSubscription;
    private Ringtone mRingtone;
    private Vibrator mVibratorService;
    private long mInitialTimerValue;
    private double mMultiplier;

    @Override
    protected int getFragmentViewId() {
        return R.layout.fragment_screen_timer;
    }

    @Override
    public void onFragmentViewCreated(View view, Bundle savedInstanceState) {
        super.onFragmentViewCreated(view, savedInstanceState);

        mVibratorService = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);

        final String contentText = getScreen().contentText;
        if (!TextUtils.isEmpty(contentText)) {
            mContentText.setVisibility(View.VISIBLE);
            mContentText.setText(contentText);
        } else {
            mContentText.setVisibility(View.GONE);
        }

        Metadata metadata = getScreenMetadata();

        mInitialTimerValue = Integer.parseInt(metadata.timerValue);
        setTimerValue(mInitialTimerValue);

        // Create fake field for input
        Field field = Field.numeric(metadata.key, metadata.label, metadata.minValue, metadata.maxValue);
        mValueInput.setField(field);
        mValueInput.setScriptPlayer(getScriptPlayer());
        mValueInput.setValueAdapter(new FieldView.ValueAdapter<Double>() {
            @Override
            public Double onReadValue(Double value) {
                return (value != null) ? (value / mMultiplier) : null;
            }

            @Override
            public Double onWriteValue(Double value) {
                return (value != null) ? value * mMultiplier : null;
            }
        });
        mValueInput.setValue(getScriptPlayer().getValue(metadata.key, null));

        // Multiplier
        String multiplierString = TextUtils.isEmpty(metadata.multiplier) ? "1" : metadata.multiplier;
        mMultiplier = Double.parseDouble(multiplierString);
        mMultiplierText.setText(multiplierString);

        // Next button
        addSubscription(Observable.combineLatest(mValueInput.valueObservable(), mValueInput.validationObservable(),
                (value, valid) -> (value != null && valid)) // Required field
                .distinctUntilChanged()
                .subscribe((enableNext) -> setActionNextEnabled(getScreen().skippable || enableNext)));

        addSubscription(mValueInput.valueObservable()
                .subscribe(value -> {
                    if (value == null) {
                        mResultText.setText(null);
                    } else {
                        mResultText.setText(String.format(Locale.getDefault(), "%.0f", value));
                    }
                })
        );
    }

    @Override
    public void onPause() {
        super.onPause();
        stopVibrator();
    }

    @OnClick(R.id.screen_timer_layout)
    public void onTimerClick() {
        if (mAlarmSubscription != null) {
            resetAlarm();
        } else if (mTimerSubscription == null) {
            // Start timer
            mTimerMessageText.setText(R.string.label_screen_stop_timer);
            mTimerSubscription = Observable.interval(0, 1, TimeUnit.SECONDS)
                    .compose(RxHelper.applySchedulers())
                    .map(v -> mInitialTimerValue - v)
                    .takeUntil(v -> v == 0)
                    .subscribe((v) -> {
                        setTimerValue(v);
                        if (v == 0) {
                            triggerTimerAlarm();
                        }
                    });
            addSubscription(mTimerSubscription);
        } else {
            resetTimer(true);
        }
    }

    // TODO: Add vibration?
    private void triggerTimerAlarm() {
        // Start alarm
        setTimerColor(R.color.colorScreenAttentionTextColor);
        setTimerValue(0);
        startAlarm();
        startVibrator();
        mAlarmSubscription = Observable.interval(0, 500, TimeUnit.MILLISECONDS)
                .compose(RxHelper.applySchedulers())
                .takeUntil(v -> v == 20)
                .subscribe((v) -> {
                    mTimerText.setVisibility((v % 2 == 0) ? View.VISIBLE : View.INVISIBLE);
                    if (v == 20) {
                        resetAlarm();
                    }
                });
        addSubscription(mAlarmSubscription);
    }

    private void resetTimer(boolean resetTime) {
        if (mTimerSubscription != null) {
            removeSubscription(mTimerSubscription);
            mTimerSubscription = null;
        }

        if (resetTime) {
            mTimerText.setVisibility(View.VISIBLE);
            mTimerMessageText.setText(R.string.label_screen_start_timer);
            setTimerColor(R.color.colorScreenContentTextColor);
            setTimerValue(mInitialTimerValue);
        }
    }

    private void resetAlarm() {
        if (mAlarmSubscription != null) {
            removeSubscription(mAlarmSubscription);
            mAlarmSubscription = null;
        }
        stopAlarm();
        stopVibrator();
        resetTimer(true);
    }

    private void setTimerColor(@ColorRes int colorRes) {
        int textColor;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            textColor = getResources().getColor(colorRes, getActivity().getTheme());
        } else {
            textColor = getResources().getColor(colorRes);
        }
        mTimerText.setTextColor(textColor);
        mTimerMessageText.setTextColor(textColor);
    }

    private void setTimerValue(long seconds) {
        long min = (long) Math.floor(seconds / 60);
        long sec = seconds - (min * 60);
        mTimerText.setText(String.format("%02d:%02d", min, sec));
    }

    private void startVibrator() {
        if (mVibratorService.hasVibrator()) {
            mVibratorService.vibrate(VIBRATOR_PATTERN, 0);
        }
    }

    private void stopVibrator() {
        if (mVibratorService.hasVibrator()) {
            mVibratorService.cancel();
        }
    }

    private void startAlarm() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            mRingtone = RingtoneManager.getRingtone(getActivity().getApplicationContext(), notification);
            mRingtone.play();
        } catch (Exception e) {
            Log.e(logTag(), "Error playing ringtone", e);
        }
    }

    private void stopAlarm() {
        if (mRingtone != null && mRingtone.isPlaying()) {
            mRingtone.stop();
        }
    }

}
