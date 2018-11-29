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

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.neotree.NeoTree;
import org.neotree.R;
import org.neotree.model.firebase.Metadata;
import org.neotree.model.firebase.Screen;
import org.neotree.player.ScriptPlayer;
import org.neotree.player.type.ScreenType;
import org.neotree.support.android.VerticalSpacingItemDecoration;
import org.neotree.support.datastore.RealmStore;
import org.neotree.ui.core.ButterknifeViewHolder;

import butterknife.BindView;
import rx.subjects.BehaviorSubject;

/**
 * Created by matteo on 14/07/2016.
 */
public class YesNoScreenFragment extends AbstractScreenFragment {

    public static YesNoScreenFragment newInstance(Screen screen) {
        Bundle args = new Bundle();
        args.putParcelable(NeoTree.EXTRA_SCREEN, screen);

        YesNoScreenFragment fragment = new YesNoScreenFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @BindView(R.id.screen_recycler_view) RecyclerView mRecyclerView;

    private final BehaviorSubject<Boolean> mActionEnabledPublisher = BehaviorSubject.create();

    @Override
    protected int getFragmentViewId() {
        return R.layout.fragment_screen_yesno;
    }

    @Override
    public void onFragmentViewCreated(View view, Bundle savedInstanceState) {
        super.onFragmentViewCreated(view, savedInstanceState);

        BooleanListViewAdapter listViewAdapter = new BooleanListViewAdapter(getScriptPlayer(), getScreen(), mActionEnabledPublisher);
        mRecyclerView.setAdapter(listViewAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new VerticalSpacingItemDecoration(getActivity(), !getScreen().hasContentText(), true));

        mActionEnabledPublisher.onNext(
                getScriptPlayer().getValue(getScreenMetadata().key, null) != null);

        addSubscription(
                mActionEnabledPublisher
                        .distinctUntilChanged()
                        .subscribe((enableNext) -> setActionNextEnabled(getScreen().skippable || enableNext))
        );
    }

    static class BooleanListViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
            implements CompoundButton.OnCheckedChangeListener {

        private static final int VIEW_TYPE_CONTENT = 100;
        private static final int VIEW_TYPE_POSITIVE_ANSWER = 101;
        private static final int VIEW_TYPE_NEGATIVE_ANSWER = 102;

        private final ScriptPlayer mScriptPlayer;
        private final Screen mScreen;
        private final ScreenType mScreenType;
        private final Metadata mMetadata;
        private final BehaviorSubject<Boolean> mEnableActionPublisher;

        public BooleanListViewAdapter(ScriptPlayer scriptPlayer, Screen screen, BehaviorSubject<Boolean> enableActionPublisher) {
            mScriptPlayer = scriptPlayer;
            mScreen = screen;
            mScreenType = ScreenType.fromString(screen.type);
            mMetadata = screen.metadata;
            mEnableActionPublisher = enableActionPublisher;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            switch (viewType) {
                case VIEW_TYPE_CONTENT:
                    return new ContentTextItemViewHolder(
                            inflater.inflate(R.layout.view_screen_content_text, parent, false));
                case VIEW_TYPE_POSITIVE_ANSWER:
                    return new CheckableAnswerItemViewHolder(
                            inflater.inflate(R.layout.view_screen_checkable_answer, parent, false), true);
                case VIEW_TYPE_NEGATIVE_ANSWER:
                    return new CheckableAnswerItemViewHolder(
                            inflater.inflate(R.layout.view_screen_checkable_answer, parent, false), false);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ContentTextItemViewHolder) {
                ContentTextItemViewHolder vh = (ContentTextItemViewHolder) holder;
                vh.contentText.setText(mScreen.contentText);
            } else if (holder instanceof CheckableAnswerItemViewHolder) {
                CheckableAnswerItemViewHolder vh = (CheckableAnswerItemViewHolder) holder;
                vh.toggleItem.setOnCheckedChangeListener(null);

                Boolean checked = mScriptPlayer.getValue(mMetadata.key, null);
                int viewType = getItemViewType(position);
                switch (viewType) {
                    case VIEW_TYPE_POSITIVE_ANSWER:
                        vh.toggleItem.setTextOn(mMetadata.positiveLabel);
                        vh.toggleItem.setTextOff(mMetadata.positiveLabel);
                        vh.toggleItem.setChecked((checked != null && checked));
                        break;
                    case VIEW_TYPE_NEGATIVE_ANSWER:
                        checked = mScriptPlayer.getValue(mMetadata.key, null);
                        vh.toggleItem.setTextOn(mMetadata.negativeLabel);
                        vh.toggleItem.setTextOff(mMetadata.negativeLabel);
                        vh.toggleItem.setChecked((checked != null && !checked));
                        break;
                }
                vh.toggleItem.setOnCheckedChangeListener(this);
            }
        }

        @Override
        public void onCheckedChanged(CompoundButton v, boolean checked) {
            Boolean value = (Boolean) v.getTag();
            mScriptPlayer.setValue(mMetadata.key, value);
            mScriptPlayer.storeValue(mScreen.sectionTitle, RealmStore.getSessionValue(mScreenType, mMetadata, value));
            mEnableActionPublisher.onNext(true);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            if (TextUtils.isEmpty(mScreen.contentText)) {
                return (position == 0) ? VIEW_TYPE_POSITIVE_ANSWER : VIEW_TYPE_NEGATIVE_ANSWER;
            } else {
                switch (position) {
                    case 0:
                        return VIEW_TYPE_CONTENT;
                    case 1:
                        return VIEW_TYPE_POSITIVE_ANSWER;
                    case 2:
                        return VIEW_TYPE_NEGATIVE_ANSWER;
                }
            }
            return -1;
        }

        @Override
        public int getItemCount() {
            // Yes/No + <content>
            return 2 + (TextUtils.isEmpty(mScreen.contentText) ? 0 : 1);
        }

    }

    static class ContentTextItemViewHolder extends ButterknifeViewHolder {
        @BindView(R.id.screen_content_text) public TextView contentText;

        public ContentTextItemViewHolder(View view) {
            super(view);
        }
    }

    static class CheckableAnswerItemViewHolder extends ButterknifeViewHolder {
        @BindView(R.id.toggle) public ToggleButton toggleItem;

        public CheckableAnswerItemViewHolder(View view, boolean value) {
            super(view);
            view.setOnClickListener(v -> toggleItem.setChecked(!toggleItem.isChecked()));
            toggleItem.setTag(value);
        }
    }

}
