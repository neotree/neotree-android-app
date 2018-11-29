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
import android.widget.TextView;

import org.neotree.NeoTree;
import org.neotree.R;
import org.neotree.model.firebase.Item;
import org.neotree.model.firebase.Metadata;
import org.neotree.model.firebase.Screen;
import org.neotree.support.android.BottomOffsetDecoration;
import org.neotree.support.android.SimpleDividerItemDecoration;
import org.neotree.ui.core.ButterknifeViewHolder;

import butterknife.BindView;

/**
 * Created by matteo on 14/07/2016.
 */
public class SimpleListScreenFragment extends AbstractScreenFragment {

    public static SimpleListScreenFragment newInstance(Screen screen) {
        Bundle args = new Bundle();
        args.putParcelable(NeoTree.EXTRA_SCREEN, screen);

        SimpleListScreenFragment fragment = new SimpleListScreenFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @BindView(R.id.screen_recycler_view) RecyclerView mRecyclerView;

    private SimpleListViewAdapter mListViewAdapter;

    @Override
    protected int getFragmentViewId() {
        return R.layout.fragment_screen_simplelist;
    }

    @Override
    public void onFragmentViewCreated(View view, Bundle savedInstanceState) {
        super.onFragmentViewCreated(view, savedInstanceState);

        mListViewAdapter = new SimpleListViewAdapter(getScreenMetadata());
        mRecyclerView.setAdapter(mListViewAdapter);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));
        mRecyclerView.addItemDecoration(new BottomOffsetDecoration(getActivity(), R.dimen.recycler_view_bottom_spacing));

        setActionNextEnabled(true);
    }

    static class SimpleListViewAdapter extends RecyclerView.Adapter<SimpleListItemViewHolder> {

        private final Metadata mMetadata;

        public SimpleListViewAdapter(Metadata metadata) {
            mMetadata = metadata;
        }

        @Override
        public SimpleListItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new SimpleListItemViewHolder(
                    inflater.inflate(R.layout.view_screen_simplelist_item, parent, false)
            );
        }

        @Override
        public void onBindViewHolder(SimpleListItemViewHolder holder, int position) {
            Item item = getItem(position);
            holder.title.setText(item.label);
            if (!TextUtils.isEmpty(item.summary)) {
                holder.summary.setText(item.summary);
                holder.summary.setVisibility(View.VISIBLE);
            } else {
                holder.summary.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return (mMetadata != null && mMetadata.items != null) ? mMetadata.items.size() : 0;
        }

        public Item getItem(int position) {
            return mMetadata.items.get(position);
        }

    }

    static class SimpleListItemViewHolder extends ButterknifeViewHolder {
        @BindView(R.id.simplelist_item_title) public TextView title;
        @BindView(R.id.simplelist_item_summary) public TextView summary;

        public SimpleListItemViewHolder(View view) {
            super(view);
        }
    }

}
