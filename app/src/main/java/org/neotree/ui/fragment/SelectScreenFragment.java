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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.neotree.NeoTree;
import org.neotree.R;
import org.neotree.model.firebase.Item;
import org.neotree.model.firebase.Metadata;
import org.neotree.model.firebase.Screen;
import org.neotree.player.ScriptPlayer;
import org.neotree.player.type.ScreenType;
import org.neotree.support.android.BottomOffsetDecoration;
import org.neotree.support.android.VerticalSpacingItemDecoration;
import org.neotree.support.datastore.RealmStore;
import org.neotree.ui.core.ButterknifeViewHolder;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import butterknife.BindView;
import rx.Observable;
import rx.subjects.BehaviorSubject;

/**
 * Created by matteo on 14/07/2016.
 */
public class SelectScreenFragment extends AbstractScreenFragment {

    public static SelectScreenFragment newInstance(Screen screen) {
        Bundle args = new Bundle();
        args.putParcelable(NeoTree.EXTRA_SCREEN, screen);

        SelectScreenFragment fragment = new SelectScreenFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @BindView(R.id.screen_recycler_view) RecyclerView mRecyclerView;

    private SelectListViewAdapter mListViewAdapter;

    @Override
    protected int getFragmentViewId() {
        return R.layout.fragment_screen_select;
    }

    @Override
    public void onFragmentViewCreated(View view, Bundle savedInstanceState) {
        super.onFragmentViewCreated(view, savedInstanceState);

        mListViewAdapter = new SelectListViewAdapter(getScriptPlayer(), getScreen());
        mRecyclerView.setAdapter(mListViewAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new VerticalSpacingItemDecoration(getActivity(), !getScreen().hasContentText(), true));
        mRecyclerView.addItemDecoration(new BottomOffsetDecoration(getActivity(), R.dimen.recycler_view_bottom_spacing));

        addSubscription(mListViewAdapter.getCheckedCounterObservable()
                .subscribe((checkedCount) -> {
                    Log.d(logTag(), String.format("Checked items [count=%d]", checkedCount));
                    setActionNextEnabled(getScreen().skippable || checkedCount > 0);
                })
        );
    }

    static class SelectListViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
            implements CompoundButton.OnCheckedChangeListener {

        private static final String TAG = SelectListViewAdapter.class.getSimpleName();

        private static final int VIEW_TYPE_CONTENT = 100;
        private static final int VIEW_TYPE_ANSWER = 101;

        private final BehaviorSubject<Integer> mCheckedCounterObservable = BehaviorSubject.create();
        private final AtomicInteger mCheckedCounter = new AtomicInteger(0);

        private final ScriptPlayer mScriptPlayer;
        private final Screen mScreen;
        private final ScreenType mScreenType;
        private final Metadata mMetadata;

        private String mExclusiveId;

        public SelectListViewAdapter(ScriptPlayer scriptPlayer, Screen screen) {
            mScriptPlayer = scriptPlayer;
            mScreen = screen;
            mScreenType = ScreenType.fromString(screen.type);
            mMetadata = screen.metadata;

            if (mScreenType == ScreenType.SINGLE_SELECT) {
                String value = mScriptPlayer.getValue(mMetadata.key, null);
                mCheckedCounter.set((value != null) ? 1 : 0);
            } else if (mScreenType == ScreenType.MULTI_SELECT) {
                Set<String> values = mScriptPlayer.getValue(mMetadata.key, new HashSet<>());
                int selectedCount = values.size();
                if (selectedCount == 1) {
                    final String value = values.iterator().next();
                    final Item item = findItemByValue(value);
                    setCheckedStateForItem(item, true);
                }
                mCheckedCounter.set(selectedCount);
            } else {
                throw new IllegalStateException("Invalid screen type: " + mScreenType);
            }
            mCheckedCounterObservable.onNext(mCheckedCounter.get());
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            switch (viewType) {
                case VIEW_TYPE_CONTENT:
                    return new ContentTextItemViewHolder(
                            inflater.inflate(R.layout.view_screen_content_text, parent, false)
                    );
                case VIEW_TYPE_ANSWER:
                    return new CheckableAnswerItemViewHolder(
                            inflater.inflate(R.layout.view_screen_checkable_answer, parent, false)
                    );
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ContentTextItemViewHolder) {
                ContentTextItemViewHolder vh = (ContentTextItemViewHolder) holder;
                vh.contentText.setText(mScreen.contentText);
            } else if (holder instanceof CheckableAnswerItemViewHolder) {
                Item item = getItem(position);

                CheckableAnswerItemViewHolder vh = (CheckableAnswerItemViewHolder) holder;
                vh.toggle.setTextOn(item.label);
                vh.toggle.setTextOff(item.label);

                boolean isChecked = getCheckedStateForItem(item);
                vh.toggle.setOnCheckedChangeListener(null);
                vh.toggle.setChecked(isChecked);
                vh.toggle.setOnCheckedChangeListener(this);
                vh.toggle.setEnabled(mExclusiveId == null || mExclusiveId.equals(item.id));

                vh.setTag(position);
            }
        }

        @Override
        public void onCheckedChanged(CompoundButton v, boolean checked) {
            final int position = (int) v.getTag();
            setCheckedStateForItem(getItem(position), checked);
        }

        @Override
        public int getItemViewType(int position) {
            if (TextUtils.isEmpty(mScreen.contentText)) {
                return VIEW_TYPE_ANSWER;
            } else {
                return (position == 0) ? VIEW_TYPE_CONTENT : VIEW_TYPE_ANSWER;
            }
        }

        @Override
        public int getItemCount() {
            final int itemsCount = (mMetadata.items != null) ? mMetadata.items.size() : 0;
            return itemsCount + (TextUtils.isEmpty(mScreen.contentText) ? 0 : 1);
        }

        public Item getItem(int position) {
            final int itemPosition = position + ((TextUtils.isEmpty(mScreen.contentText)) ? 0 : -1);
            return mMetadata.items.get(itemPosition);
        }

        private Item findItemByValue(String value) {
            if (TextUtils.isEmpty(value)) {
                return null;
            }

            for (Item item : mMetadata.items) {
                if (value.equals(item.id)) {
                    return item;
                }
            }
            return null;
        }

        private boolean getCheckedStateForItem(Item item) {
            if (mScreenType == ScreenType.SINGLE_SELECT) {
                String value = mScriptPlayer.getValue(mMetadata.key, "<unset>");
                return value.equals(item.id);
            } else if (mScreenType == ScreenType.MULTI_SELECT) {
                Set<String> values = mScriptPlayer.getValue(mMetadata.key, new HashSet<>());
                return values.contains(item.id);
            } else {
                throw new IllegalStateException("Invalid screen type: " + mScreenType);
            }
        }

        private void setCheckedStateForItem(Item item, boolean checked) {
            if (mScreenType == ScreenType.SINGLE_SELECT) {
                if (checked) {
                    mCheckedCounter.set(1);
                } else {
                    mCheckedCounter.set(0);
                }
                String value = (checked) ? item.id : null;
                mScriptPlayer.setValue(mMetadata.key, value);
                mScriptPlayer.storeValue(mScreen.sectionTitle, RealmStore.getSessionValue(mScreenType, mMetadata, value));
            } else if (mScreenType == ScreenType.MULTI_SELECT) {
                Set<String> values = mScriptPlayer.getValue(mMetadata.key, new HashSet<>());
                if (checked) {
                    if (item.exclusive) {
                        values.clear();
                        mExclusiveId = item.id;
                    }
                    values.add(item.id);
                } else {
                    if (mExclusiveId != null && mExclusiveId.equals(item.id)) {
                        mExclusiveId = null;
                    }
                    values.remove(item.id);
                }
                mCheckedCounter.set(values.size());
                mScriptPlayer.setValue(mMetadata.key, values);
                // TODO: Fix metadata label storage
                mScriptPlayer.storeValue(mMetadata.key, mMetadata.label, mScreen.sectionTitle, RealmStore.getSessionValue(mMetadata, values));
            } else {
                throw new IllegalStateException("Invalid screen type: " + mScreenType);
            }
            mCheckedCounterObservable.onNext(mCheckedCounter.get());
            notifyDataSetChanged();
        }

        public Observable<Integer> getCheckedCounterObservable() {
            return mCheckedCounterObservable.asObservable();
        }
    }

    static class ContentTextItemViewHolder extends ButterknifeViewHolder {
        @BindView(R.id.screen_content_text) public TextView contentText;

        public ContentTextItemViewHolder(View view) {
            super(view);
        }
    }

    static class CheckableAnswerItemViewHolder extends ButterknifeViewHolder {
        @BindView(R.id.toggle) public ToggleButton toggle;

        public CheckableAnswerItemViewHolder(View view) {
            super(view);
            view.setOnClickListener(v -> toggle.setChecked(!toggle.isChecked()));
        }

        public void setTag(Object o) {
            toggle.setTag(o);
        }
    }

}
