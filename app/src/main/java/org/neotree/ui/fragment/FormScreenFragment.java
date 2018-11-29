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
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.neotree.NeoTree;
import org.neotree.R;
import org.neotree.model.firebase.Field;
import org.neotree.model.firebase.Metadata;
import org.neotree.model.firebase.Screen;
import org.neotree.player.ScriptPlayer;
import org.neotree.player.type.FieldType;
import org.neotree.player.validator.FormManager;
import org.neotree.support.android.BottomOffsetDecoration;
import org.neotree.support.android.VerticalSpacingItemDecoration;
import org.neotree.support.rx.RxHelper;
import org.neotree.ui.core.ButterknifeViewHolder;
import org.neotree.ui.view.DateTimeFieldView;
import org.neotree.ui.view.DropdownFieldView;
import org.neotree.ui.view.NumberFieldView;
import org.neotree.ui.view.PeriodFieldView;
import org.neotree.ui.view.TextFieldView;

import butterknife.BindView;

/**
 * Created by matteo on 14/07/2016.
 */
public class FormScreenFragment extends AbstractScreenFragment {

    public static FormScreenFragment newInstance(Screen screen) {
        Bundle args = new Bundle();
        args.putParcelable(NeoTree.EXTRA_SCREEN, screen);

        FormScreenFragment fragment = new FormScreenFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @BindView(R.id.screen_recycler_view) RecyclerView mRecyclerView;

    private FormManager mFormManager;

    @Override
    protected int getFragmentViewId() {
        return R.layout.fragment_screen_form;
    }

    @Override
    public void onFragmentViewCreated(View view, Bundle savedInstanceState) {
        super.onFragmentViewCreated(view, savedInstanceState);

        mFormManager = new FormManager(getScreen(), getScriptPlayer());
        mFormManager.subscribe();

        final FormListViewAdapter adapter = new FormListViewAdapter(getActivity(), mFormManager, getScriptPlayer(), getScreen());
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new VerticalSpacingItemDecoration(getActivity(), !getScreen().hasContentText(), true));
        mRecyclerView.addItemDecoration(new BottomOffsetDecoration(getActivity(), R.dimen.recycler_view_bottom_spacing));

        addSubscription(mFormManager.validFormObservable()
                .compose(RxHelper.applySchedulers())
                .subscribe(this::setActionNextEnabled)
        );

        addSubscription(mFormManager.fieldStatusObservable()
                .compose(RxHelper.applySchedulers())
                .subscribe(statusInfo -> adapter.notifyFieldStatusChanged(statusInfo.getIndex()))
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mFormManager.destroy();
    }

    static class FormListViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final String TAG = FormListViewAdapter.class.getSimpleName();

        private static final int VIEW_TYPE_CONTENT  = 100;
        private static final int VIEW_TYPE_DATE     = 101;
        private static final int VIEW_TYPE_DATETIME = 102;
        private static final int VIEW_TYPE_DROPDOWN = 103;
        private static final int VIEW_TYPE_NUMBER   = 104;
        private static final int VIEW_TYPE_PERIOD   = 105;
        private static final int VIEW_TYPE_TEXT     = 106;
        private static final int VIEW_TYPE_TIME     = 107;

        private final Context mContext;
        private final FormManager mFormManager;
        private final Metadata mMetadata;
        private final ScriptPlayer mScriptPlayer;
        private final Screen mScreen;

        public FormListViewAdapter(Context context, FormManager formManager, ScriptPlayer scriptPlayer, Screen screen) {
            mContext = context;
            mFormManager = formManager;
            mMetadata = screen.metadata;
            mScriptPlayer = scriptPlayer;
            mScreen = screen;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            switch (viewType) {
                case VIEW_TYPE_CONTENT:
                    return new ContentTextItemViewHolder(
                            inflater.inflate(R.layout.view_field_content_text, parent, false)
                    );

                case VIEW_TYPE_DATE:
                    DateTimeFieldView dateView = new DateTimeFieldView(mContext);
                    dateView.setDisplayMode(DateTimeFieldView.DateTimeDisplayMode.DATE);
                    return new DateTimeFieldItemViewHolder(dateView);

                case VIEW_TYPE_DATETIME:
                    DateTimeFieldView dateTimeView = new DateTimeFieldView(mContext);
                    dateTimeView.setDisplayMode(DateTimeFieldView.DateTimeDisplayMode.DATETIME);
                    return new DateTimeFieldItemViewHolder(dateTimeView);

                case VIEW_TYPE_DROPDOWN:
                    DropdownFieldView dropdownView = new DropdownFieldView(mContext);
                    return new DropdownFieldItemViewHolder(dropdownView);

                case VIEW_TYPE_NUMBER:
                    NumberFieldView numberView = new NumberFieldView(mContext);
                    return new NumberFieldItemViewHolder(numberView);

                case VIEW_TYPE_PERIOD:
                    PeriodFieldView periodView = new PeriodFieldView(mContext);
                    return new PeriodFieldItemViewHolder(periodView);

                case VIEW_TYPE_TEXT:
                    TextFieldView textView = new TextFieldView(mContext);
                    return new TextFieldItemViewHolder(textView);

                case VIEW_TYPE_TIME:
                    DateTimeFieldView timeView = new DateTimeFieldView(mContext);
                    timeView.setDisplayMode(DateTimeFieldView.DateTimeDisplayMode.TIME);
                    return new DateTimeFieldItemViewHolder(timeView);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ContentTextItemViewHolder) {
                ContentTextItemViewHolder vh = (ContentTextItemViewHolder) holder;
                vh.contentText.setText(mScreen.contentText);
            } else {
                try {
                    Field field = getField(position);
                    boolean enabled = mFormManager.isFieldEnabled(getFieldIndex(position));
                    if (holder instanceof DateTimeFieldItemViewHolder) {
                        DateTimeFieldItemViewHolder vh = (DateTimeFieldItemViewHolder) holder;
                        vh.setScriptPlayer(mScriptPlayer);
                        vh.field.setField(field);
                        vh.field.setEnabled(enabled);
                    } else if (holder instanceof DropdownFieldItemViewHolder) {
                        DropdownFieldItemViewHolder vh = (DropdownFieldItemViewHolder) holder;
                        vh.setScriptPlayer(mScriptPlayer);
                        vh.field.setField(field);
                        vh.field.setValues(field.values);
                        vh.field.setEnabled(enabled);
                    } else if (holder instanceof NumberFieldItemViewHolder) {
                        NumberFieldItemViewHolder vh = (NumberFieldItemViewHolder) holder;
                        vh.setScriptPlayer(mScriptPlayer);
                        vh.field.setField(field);
                        vh.field.setEnabled(enabled);
                    } else if (holder instanceof PeriodFieldItemViewHolder) {
                        PeriodFieldItemViewHolder vh = (PeriodFieldItemViewHolder) holder;
                        vh.setScriptPlayer(mScriptPlayer);
                        vh.field.setField(field);
                        vh.field.setEnabled(enabled);
                    } else if (holder instanceof TextFieldItemViewHolder) {
                        TextFieldItemViewHolder vh = (TextFieldItemViewHolder) holder;
                        vh.setScriptPlayer(mScriptPlayer);
                        vh.field.setField(field);
                        vh.field.setEnabled(enabled);
                    }
                } catch (Exception e) {
                    mScriptPlayer.notifyScriptError(e.getMessage(), e);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (TextUtils.isEmpty(mScreen.contentText)) {
                return getViewTypeByFieldPosition(position);
            } else {
                return (position == 0) ? VIEW_TYPE_CONTENT : getViewTypeByFieldPosition(position);
            }
        }

        @Override
        public int getItemCount() {
            final int fieldsCount = (mMetadata.fields != null) ? mMetadata.fields.size() : 0;
            return fieldsCount + (TextUtils.isEmpty(mScreen.contentText) ? 0 : 1);
        }

        public Field getField(int position) {
            final int fieldPosition = getFieldIndex(position);
            return mMetadata.fields.get(fieldPosition);
        }

        public int getFieldIndex(int position) {
            return position + ((TextUtils.isEmpty(mScreen.contentText)) ? 0 : -1);
        }

        private int getViewTypeByFieldPosition(int position) {
            Field field = getField(position);
            FieldType fieldType = FieldType.fromString(field.type);
            switch (fieldType) {
                case DATE:
                    return VIEW_TYPE_DATE;
                case DATETIME:
                    return VIEW_TYPE_DATETIME;
                case DROPDOWN:
                    return VIEW_TYPE_DROPDOWN;
                case NUMBER:
                    return VIEW_TYPE_NUMBER;
                case PERIOD:
                    return VIEW_TYPE_PERIOD;
                case TEXT:
                    return VIEW_TYPE_TEXT;
                case TIME:
                    return VIEW_TYPE_TIME;
                default:
                    return -1;
            }
        }

        public void notifyFieldStatusChanged(int position) {
            int index = position + ((TextUtils.isEmpty(mScreen.contentText)) ? 0 : 1);
            notifyItemChanged(index);
        }
    }

    static class ContentTextItemViewHolder extends ButterknifeViewHolder {
        @BindView(R.id.screen_content_text) public TextView contentText;

        public ContentTextItemViewHolder(View view) {
            super(view);
        }
    }

    static class DateTimeFieldItemViewHolder extends RecyclerView.ViewHolder {
        public DateTimeFieldView field;

        public DateTimeFieldItemViewHolder(DateTimeFieldView view) {
            super(view);
            this.field = view;
        }

        public void setScriptPlayer(ScriptPlayer scriptPlayer) {
            field.setScriptPlayer(scriptPlayer);
        }
    }

    static class DropdownFieldItemViewHolder extends RecyclerView.ViewHolder {
        public DropdownFieldView field;

        public DropdownFieldItemViewHolder(DropdownFieldView view) {
            super(view);
            this.field = view;
        }

        public void setScriptPlayer(ScriptPlayer scriptPlayer) {
            field.setScriptPlayer(scriptPlayer);
        }
    }

    static class NumberFieldItemViewHolder extends RecyclerView.ViewHolder {
        public NumberFieldView field;

        public NumberFieldItemViewHolder(NumberFieldView view) {
            super(view);
            this.field = view;
        }

        public void setScriptPlayer(ScriptPlayer scriptPlayer) {
            field.setScriptPlayer(scriptPlayer);
        }
    }

    static class PeriodFieldItemViewHolder extends RecyclerView.ViewHolder {
        public PeriodFieldView field;

        public PeriodFieldItemViewHolder(PeriodFieldView view) {
            super(view);
            this.field = view;
        }

        public void setScriptPlayer(ScriptPlayer scriptPlayer) {
            field.setScriptPlayer(scriptPlayer);
        }
    }

    static class TextFieldItemViewHolder extends RecyclerView.ViewHolder {
        public TextFieldView field;

        public TextFieldItemViewHolder(TextFieldView view) {
            super(view);
            this.field = view;
        }

        public void setScriptPlayer(ScriptPlayer scriptPlayer) {
            field.setScriptPlayer(scriptPlayer);
        }
    }

}
