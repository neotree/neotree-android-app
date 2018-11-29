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

import android.app.Activity;
import android.content.Context;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.neotree.R;
import org.neotree.model.firebase.Field;
import org.neotree.player.type.DefaultValueType;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * Created by matteo on 17/07/2016.
 */
public class DateTimeFieldView extends FieldView<DateTime>
        implements DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    public enum DateTimeDisplayMode {
        DATETIME, DATE, TIME
    }

    @BindView(R.id.field_date_button)
    Button mDateValueButton;
    @BindView(R.id.field_time_button)
    Button mTimeValueButton;
    @BindView(R.id.field_spacer)
    View mFieldSpacer;

    private DateTimeDisplayMode mDisplayMode;

    public DateTimeFieldView(Context context) {
        this(context, null);
    }

    public DateTimeFieldView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DateTimeFieldView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int getViewLayoutId() {
        return R.layout.view_field_datetime;
    }

    @Override
    protected void onRegisterSubscribers(Field field) {

    }

    @Override
    protected void onUpdateFieldView(DateTime value) {
        if (value == null) {
            mDateValueButton.setText(null);
            mTimeValueButton.setText(null);
        } else {
            mDateValueButton.setText(value.toString(DateTimeFormat.forPattern("dd MMM YYYY")));
            mTimeValueButton.setText(value.toString(DateTimeFormat.forPattern("HH:mm")));
        }
        publishValue(value);
    }

    @Override
    protected void onEnabledStateChanged(boolean enabled) {
        mDateValueButton.setEnabled(enabled);
        mTimeValueButton.setEnabled(enabled);
    }

    public DateTimeDisplayMode getDisplayMode() {
        return mDisplayMode;
    }

    public void setDisplayMode(DateTimeDisplayMode displayMode) {
        mDisplayMode = displayMode;

        switch (displayMode) {
            case DATE:
                mTimeValueButton.setVisibility(GONE);
                mFieldSpacer.setVisibility(GONE);
                break;

            case TIME:
                mDateValueButton.setVisibility(GONE);
                mFieldSpacer.setVisibility(GONE);
                break;

            case DATETIME:
            default:
                mDateValueButton.setVisibility(VISIBLE);
                mTimeValueButton.setVisibility(VISIBLE);
                mFieldSpacer.setVisibility(VISIBLE);
                break;
        }
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        DateTime value = getValue();
        // TODO: Decide on what is the most sensible default
        //DateTime baseDate = (value != null) ? value : DateTime.now().withTimeAtStartOfDay();
        DateTime baseDate = (value != null) ? value : DateTime.now().withTime(12, 0, 0, 0);
        value = baseDate
                .withYear(year)
                .withMonthOfYear(monthOfYear + 1)
                .withDayOfMonth(dayOfMonth);
        setValue(value);
    }

    @Override
    public void onTimeSet(RadialPickerLayout view, int hourOfDay, int minute, int second) {
        DateTime value = getValue();
        DateTime baseDate = (value != null) ? value : DateTime.now();
        value = baseDate
                .withHourOfDay(hourOfDay)
                .withMinuteOfHour(minute)
                .withSecondOfMinute(0)
                .withMillisOfSecond(0);
        setValue(value);
    }

    @OnClick(R.id.field_date_button)
    public void onDateFieldClick() {
        DateTime pickerDate = (getValue() != null) ? getValue() : DateTime.now();
        DatePickerDialog picker = DatePickerDialog.newInstance(this,
                pickerDate.getYear(),
                pickerDate.getMonthOfYear() - 1, // Damn Java!
                pickerDate.getDayOfMonth()
        );
        picker.setMaxDate(Calendar.getInstance());
        picker.dismissOnPause(true);
        picker.vibrate(false);
        picker.show(((Activity) getContext()).getFragmentManager(), "NTDatePickerDialog");
    }

    @OnClick(R.id.field_time_button)
    public void onTimeFieldClick() {
        DateTime pickerTime = (getValue() != null) ? getValue() : DateTime.now();
        TimePickerDialog picker = TimePickerDialog.newInstance(this,
                pickerTime.getHourOfDay(),
                pickerTime.getMinuteOfHour(),
                DateFormat.is24HourFormat(getContext())
        );
        picker.dismissOnPause(true);
        picker.vibrate(false);
        picker.show(((Activity) getContext()).getFragmentManager(), "NTTimePickerDialog");
    }

    @Override
    protected DateTime restoreValue() {
        DateTime value = super.restoreValue();
        if (value == null && getField().defaultValue != null) {
            DefaultValueType type = DefaultValueType.fromString(getField().defaultValue);
            switch (type) {
                case DATE_NOW:
                    value = DateTime.now();
                    break;
                case DATE_NOON:
                    value = DateTime.now().withTime(12, 0, 0, 0);
                    break;
                case DATE_MIDNIGHT:
                    value = DateTime.now().withTimeAtStartOfDay();
                    break;
            }
        }
        return value;
    }
}
