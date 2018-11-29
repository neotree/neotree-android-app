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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.neotree.NeoTree;
import org.neotree.R;
import org.neotree.model.firebase.FileInfo;
import org.neotree.model.firebase.Metadata;
import org.neotree.model.firebase.Screen;
import org.neotree.model.firebase.Section;
import org.neotree.support.android.BottomOffsetDecoration;
import org.neotree.support.android.VerticalSpacingItemDecoration;
import org.neotree.ui.core.ButterknifeViewHolder;

import java.util.ArrayList;

import butterknife.BindView;

/**
 * Created by matteo on 14/07/2016.
 */
public class ManagementScreenFragment extends AbstractScreenFragment {

    public static ManagementScreenFragment newInstance(Screen screen) {
        Bundle args = new Bundle();
        args.putParcelable(NeoTree.EXTRA_SCREEN, screen);

        ManagementScreenFragment fragment = new ManagementScreenFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @BindView(R.id.screen_recycler_view) RecyclerView mRecyclerView;

    private ManagementViewAdapter mListViewAdapter;

    @Override
    protected int getFragmentViewId() {
        return R.layout.fragment_screen_management;
    }

    @Override
    public void onFragmentViewCreated(View view, Bundle savedInstanceState) {
        super.onFragmentViewCreated(view, savedInstanceState);

        mListViewAdapter = new ManagementViewAdapter(getScreen());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new VerticalSpacingItemDecoration(getActivity(), !getScreen().hasContentText(), true));
        mRecyclerView.addItemDecoration(new BottomOffsetDecoration(getActivity(), R.dimen.recycler_view_bottom_spacing));
        mRecyclerView.setAdapter(mListViewAdapter);

        setActionNextEnabled(true);
    }

    static class ManagementViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final String TAG = ManagementViewAdapter.class.getSimpleName();

        private static final int VIEW_TYPE_CONTENT = 100;
        private static final int VIEW_TYPE_ITEM = 101;

        private final Screen mScreen;
        private final Metadata mMetadata;

        public ManagementViewAdapter(Screen screen) {
            mScreen = screen;
            mMetadata = screen.metadata;

            // TODO: Temporary implementation to fake list of sections in management screen
            ArrayList<Section> sections = new ArrayList<>();
            if (mMetadata != null) {
                if (!TextUtils.isEmpty(mMetadata.title1) || !TextUtils.isEmpty(mMetadata.text1) || mMetadata.image1 != null) {
                    sections.add(new Section(mMetadata.title1, mMetadata.text1, mMetadata.image1));
                }

                if (!TextUtils.isEmpty(mMetadata.title2) || !TextUtils.isEmpty(mMetadata.text2) || mMetadata.image2 != null) {
                    sections.add(new Section(mMetadata.title2, mMetadata.text2, mMetadata.image2));
                }

                if (!TextUtils.isEmpty(mMetadata.title3) || !TextUtils.isEmpty(mMetadata.text3) || mMetadata.image3 != null) {
                    sections.add(new Section(mMetadata.title3, mMetadata.text3, mMetadata.image3));
                }
                mMetadata.sections = sections;
            }
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
                    return new ManagementItemViewHolder(
                            inflater.inflate(R.layout.view_screen_management_item, parent, false)
                    );
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ContentTextItemViewHolder) {
                ContentTextItemViewHolder vh = (ContentTextItemViewHolder) holder;
                vh.contentText.setText(mScreen.contentText);
            } else if (holder instanceof ManagementItemViewHolder) {
                ManagementItemViewHolder vh = (ManagementItemViewHolder) holder;

                Section section = getItem(position);
                if (!TextUtils.isEmpty(section.title)) {
                    vh.title.setText(section.title);
                    vh.title.setVisibility(View.VISIBLE);
                } else {
                    vh.title.setVisibility(View.GONE);
                }

                if (!TextUtils.isEmpty(section.body)) {
                    vh.body.setText(section.body);
                    //NeoTreeHelper.processMarkdown(vh.body, section.body);
                    vh.body.setVisibility(View.VISIBLE);
                } else {
                    vh.body.setVisibility(View.GONE);
                }

                if (section.image != null) {
                    vh.image.setVisibility(View.GONE);

                    try {
                        FileInfo info = section.image;
                        if (TextUtils.isEmpty(info.data)) {
                            return;
                        }

                        String base64Data = info.data.substring(info.data.indexOf(',') + 1);
                        if (TextUtils.isEmpty(info.data)) {
                            return;
                        }

                        byte[] decodedString = Base64.decode(base64Data, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        vh.image.setImageBitmap(bitmap);
                        vh.image.setVisibility(View.VISIBLE);
                    } catch (Exception e) {
                        Log.e(TAG, "Error decoding image", e);
                    }
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (TextUtils.isEmpty(mScreen.contentText)) {
                return VIEW_TYPE_ITEM;
            } else {
                return (position == 0) ? VIEW_TYPE_CONTENT : VIEW_TYPE_ITEM;
            }
        }

        @Override
        public int getItemCount() {
            final int itemsCount = (mMetadata.sections != null) ? mMetadata.sections.size() : 0;
            return itemsCount + (TextUtils.isEmpty(mScreen.contentText) ? 0 : 1);
        }

        public Section getItem(int position) {
            final int itemPosition = getItemOffsetPosition(position);
            return mMetadata.sections.get(itemPosition);
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

    static class ManagementItemViewHolder extends ButterknifeViewHolder {
        @BindView(R.id.management_item_title) public TextView title;
        @BindView(R.id.management_item_body) public TextView body;
        @BindView(R.id.management_item_image) public ImageView image;

        public ManagementItemViewHolder(View view) {
            super(view);
        }
    }

}
