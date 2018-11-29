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
import android.widget.RadioButton;
import android.widget.TextView;

import org.neotree.NeoTree;
import org.neotree.R;
import org.neotree.model.firebase.Item;
import org.neotree.model.firebase.Metadata;
import org.neotree.model.firebase.Screen;
import org.neotree.player.ScriptPlayer;
import org.neotree.support.android.BottomOffsetDecoration;
import org.neotree.support.android.VerticalSpacingItemDecoration;
import org.neotree.support.datastore.RealmStore;
import org.neotree.ui.core.ButterknifeViewHolder;

import java.util.concurrent.atomic.AtomicInteger;

import butterknife.BindView;
import rx.Observable;
import rx.subjects.BehaviorSubject;

/**
 * Created by matteo on 14/07/2016.
 */
public class ChecklistScreenFragment extends AbstractScreenFragment {

    public static ChecklistScreenFragment newInstance(Screen screen) {
        Bundle args = new Bundle();
        args.putParcelable(NeoTree.EXTRA_SCREEN, screen);

        ChecklistScreenFragment fragment = new ChecklistScreenFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @BindView(R.id.screen_recycler_view) RecyclerView mRecyclerView;

    private CheckListViewAdapter mListViewAdapter;

    @Override
    protected int getFragmentViewId() {
        return R.layout.fragment_screen_checklist;
    }

    @Override
    public void onFragmentViewCreated(View view, Bundle savedInstanceState) {
        super.onFragmentViewCreated(view, savedInstanceState);

        mListViewAdapter = new CheckListViewAdapter(getScriptPlayer(), getScreen());
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

    static class CheckListViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
            implements View.OnClickListener {

        private static final int VIEW_TYPE_CONTENT = 100;
        private static final int VIEW_TYPE_ITEM = 101;

        private final BehaviorSubject<Integer> mCheckedCounterObservable = BehaviorSubject.create();
        private final AtomicInteger mCheckedCounter = new AtomicInteger(0);

        private final ScriptPlayer mScriptPlayer;
        private final Screen mScreen;
        private final Metadata mMetadata;

        private String mExclusiveKey;

        public CheckListViewAdapter(ScriptPlayer scriptPlayer, Screen screen) {
            mScriptPlayer = scriptPlayer;
            mScreen = screen;
            mMetadata = screen.metadata;

            for (Item item : mMetadata.items) {
                boolean value = mScriptPlayer.getValue(item.key, false);
                if (value) {
                    if (item.exclusive) {
                        setCheckedStateForItem(item, true);
                        break;
                    } else {
                        mCheckedCounter.incrementAndGet();
                    }
                }
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
                case VIEW_TYPE_ITEM:
                    return new CheckListItemViewHolder(
                            inflater.inflate(R.layout.view_screen_checklist_radio_item, parent, false)
                    );
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ContentTextItemViewHolder) {
                ContentTextItemViewHolder vh = (ContentTextItemViewHolder) holder;
                vh.contentText.setText(mScreen.contentText);
            } else if (holder instanceof CheckListItemViewHolder) {
                CheckListItemViewHolder vh = (CheckListItemViewHolder) holder;

                Item item = getItem(position);
                vh.title.setText(item.label);
                if (!TextUtils.isEmpty(item.summary)) {
                    vh.summary.setText(item.summary);
                    vh.summary.setVisibility(View.VISIBLE);
                } else {
                    vh.summary.setVisibility(View.GONE);
                }
                vh.setTag(position);

                boolean isChecked = mScriptPlayer.getValue(item.key, false);
                vh.positive.setOnClickListener(null);
                vh.negative.setOnClickListener(null);
                vh.setChecked(isChecked);
                vh.positive.setOnClickListener(this);
                vh.negative.setOnClickListener(this);

                boolean isEnabled = mExclusiveKey == null || mExclusiveKey.equals(item.key);
                vh.title.setEnabled(isEnabled);
                vh.summary.setEnabled(isEnabled);
                vh.positive.setEnabled(isEnabled);
                vh.negative.setEnabled(isEnabled);
            }
        }

        @Override
        public void onClick(View v) {
            int position = (int) v.getTag();
            setCheckedStateForItem(getItem(position), (v.getId() == R.id.checklist_item_radio_positive));
        }

        private void setCheckedStateForItem(Item currentItem, boolean checked) {
            if (currentItem.exclusive) {
                // Reset all item values but the one selected
                for (Item item : mMetadata.items) {
                    if (item.key.equals(currentItem.key)) {
                        // Current item
                        mScriptPlayer.setValue(item.key, checked);
                        mScriptPlayer.storeValue(mScreen.sectionTitle, RealmStore.getSessionValue(item, checked));
                        mCheckedCounter.set((checked) ? 1 : 0);
                    } else {
                        // If current item is checked set all other to false, otherwise leave their values untouched
                        boolean otherItemValue = (checked) ? false : mScriptPlayer.getValue(item.key, false);
                        mScriptPlayer.setValue(item.key, otherItemValue);
                        mScriptPlayer.storeValue(mScreen.sectionTitle, RealmStore.getSessionValue(item, otherItemValue));
                    }
                }
                mExclusiveKey = (checked) ? currentItem.key : null;
            } else {
                mScriptPlayer.setValue(currentItem.key, checked);
                mScriptPlayer.storeValue(mScreen.sectionTitle, RealmStore.getSessionValue(currentItem, checked));
                if (checked) {
                    mCheckedCounter.incrementAndGet();
                } else {
                    mCheckedCounter.decrementAndGet();
                }
            }
            mCheckedCounterObservable.onNext(mCheckedCounter.get());
            notifyDataSetChanged();
        }

        public Observable<Integer> getCheckedCounterObservable() {
            return mCheckedCounterObservable.asObservable();
        }

        @Override
        public int getItemCount() {
            final int itemsCount = (mMetadata.items != null) ? mMetadata.items.size() : 0;
            return itemsCount + (TextUtils.isEmpty(mScreen.contentText) ? 0 : 1);
        }

        public Item getItem(int position) {
            final int itemPosition = getItemOffsetPosition(position);
            return mMetadata.items.get(itemPosition);
        }

        @Override
        public int getItemViewType(int position) {
            if (TextUtils.isEmpty(mScreen.contentText)) {
                return VIEW_TYPE_ITEM;
            } else {
                return (position == 0) ? VIEW_TYPE_CONTENT : VIEW_TYPE_ITEM;
            }
        }

        public int getItemOffsetPosition(int position) {
            return position + ((TextUtils.isEmpty(mScreen.contentText)) ? 0 : -1);
        }

    }

    static class ContentTextItemViewHolder extends ButterknifeViewHolder {
        @BindView(R.id.screen_content_text) public TextView contentText;

        public ContentTextItemViewHolder(View view) {
            super(view);
        }
    }

    static class CheckListItemViewHolder extends ButterknifeViewHolder {
        @BindView(R.id.checklist_item_title) public TextView title;
        @BindView(R.id.checklist_item_summary) public TextView summary;
        @BindView(R.id.checklist_item_radio_positive) public RadioButton positive;
        @BindView(R.id.checklist_item_radio_negative) public RadioButton negative;

        public CheckListItemViewHolder(View view) {
            super(view);
        }

        public void setChecked(boolean checked) {
            if (checked) {
                positive.toggle();
            } else {
                negative.toggle();
            }
        }

        public void setTag(Object o) {
            positive.setTag(o);
            negative.setTag(o);
        }
    }

}