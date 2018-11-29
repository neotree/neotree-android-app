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
import android.widget.ImageView;
import android.widget.TextView;

import org.neotree.NeoTree;
import org.neotree.R;
import org.neotree.model.firebase.Item;
import org.neotree.model.firebase.Metadata;
import org.neotree.model.firebase.Screen;
import org.neotree.support.android.BottomOffsetDecoration;
import org.neotree.ui.core.ButterknifeViewHolder;

import butterknife.BindView;

/**
 * Created by matteo on 14/07/2016.
 */
public class ProgressScreenFragment extends AbstractScreenFragment {

    public static ProgressScreenFragment newInstance(Screen screen) {
        Bundle args = new Bundle();
        args.putParcelable(NeoTree.EXTRA_SCREEN, screen);

        ProgressScreenFragment fragment = new ProgressScreenFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @BindView(R.id.screen_recycler_view) RecyclerView mRecyclerView;

    private ProgressViewAdapter mListViewAdapter;

    @Override
    protected int getFragmentViewId() {
        return R.layout.fragment_screen_checklist;
    }

    @Override
    public void onFragmentViewCreated(View view, Bundle savedInstanceState) {
        super.onFragmentViewCreated(view, savedInstanceState);

        mListViewAdapter = new ProgressViewAdapter(getScreen());
        mRecyclerView.setAdapter(mListViewAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new BottomOffsetDecoration(getActivity(), R.dimen.recycler_view_bottom_spacing));

        // Always allowed to skip
        setActionNextEnabled(true);
    }

    static class ProgressViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_CONTENT = 100;
        private static final int VIEW_TYPE_PROGRESS_ITEM = 101;

        private final Screen mScreen;
        private final Metadata mMetadata;

        public ProgressViewAdapter(Screen screen) {
            mScreen = screen;
            mMetadata = screen.metadata;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            switch (viewType) {
                case VIEW_TYPE_CONTENT:
                    return new ContentTextItemViewHolder(
                            inflater.inflate(R.layout.view_screen_content_text, parent, false)
                    );
                case VIEW_TYPE_PROGRESS_ITEM:
                    return new ProgressItemViewHolder(
                            inflater.inflate(R.layout.view_screen_progress_item, parent, false)
                    );
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ContentTextItemViewHolder) {
                ContentTextItemViewHolder vh = (ContentTextItemViewHolder) holder;
                vh.contentText.setText(mScreen.contentText);
            } else if (holder instanceof ProgressItemViewHolder) {
                Item item = getItem(position);

                ProgressItemViewHolder vh = (ProgressItemViewHolder) holder;
                vh.title.setText(item.label);
                if (!TextUtils.isEmpty(item.summary)) {
                    vh.summary.setText(item.summary);
                    vh.summary.setVisibility(View.VISIBLE);
                } else {
                    vh.summary.setVisibility(View.GONE);
                }

                if (item.checked) {
                    vh.icon.setImageResource(R.drawable.ic_progress_checked);
                } else {
                    vh.icon.setImageResource(R.drawable.ic_progress_unchecked);
                }

                final int offsetPosition = getItemOffsetPosition(position);
                vh.linkTop.setVisibility((offsetPosition == 0) ? View.INVISIBLE : View.VISIBLE);
                vh.linkBottom.setVisibility((offsetPosition < getItemCount() - 2) ? View.VISIBLE : View.INVISIBLE);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (TextUtils.isEmpty(mScreen.contentText)) {
                return VIEW_TYPE_PROGRESS_ITEM;
            } else {
                return (position == 0) ? VIEW_TYPE_CONTENT : VIEW_TYPE_PROGRESS_ITEM;
            }
        }

        @Override
        public int getItemCount() {
            final int itemsCount = (mMetadata != null && mMetadata.items != null) ? mMetadata.items.size() : 0;
            return itemsCount + (TextUtils.isEmpty(mScreen.contentText) ? 0 : 1);
        }

        public Item getItem(int position) {
            final int itemPosition = getItemOffsetPosition(position);
            return mMetadata.items.get(itemPosition);
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

    static class ProgressItemViewHolder extends ButterknifeViewHolder {
        @BindView(R.id.progress_item_link_top) public View linkTop;
        @BindView(R.id.progress_item_link_bottom) public View linkBottom;
        @BindView(R.id.progress_item_icon) public ImageView icon;
        @BindView(R.id.progress_item_title) public TextView title;
        @BindView(R.id.progress_item_summary) public TextView summary;

        public ProgressItemViewHolder(View view) {
            super(view);
        }
    }

}
