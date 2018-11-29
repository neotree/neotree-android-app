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

package org.neotree.support.android;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.neotree.R;

/**
 * Created by matteo on 16/07/2016.
 */
public class VerticalSpacingItemDecoration extends RecyclerView.ItemDecoration {

    private Context mContext;
    private int mVerticalSpace;
    private int mFirstItemTopSpace;
    private int mLastItemBottomSpace;

    public VerticalSpacingItemDecoration(@NonNull Context context) {
        this(context, R.dimen.card_vertical_spacing, true, true);
    }

    public VerticalSpacingItemDecoration(@NonNull Context context, boolean showTopSpacing, boolean showBottomSpacing) {
        this(context, R.dimen.card_vertical_spacing, showTopSpacing, showBottomSpacing);
    }

    public VerticalSpacingItemDecoration(@NonNull Context context, @DimenRes int resId, boolean showTopSpacing, boolean showBottomSpacing) {
        mContext = context;

        int defaultSpace = getDimension(mContext, resId);

        mFirstItemTopSpace = (showTopSpacing) ? defaultSpace : 0;
        mLastItemBottomSpace = (showBottomSpacing) ? defaultSpace : 0;
        mVerticalSpace = defaultSpace;
    }

    public void setFirstItemTopSpace(@DimenRes int resId) {
        mFirstItemTopSpace = getDimension(mContext, resId);
    }

    public void setLastItemTopSpace(@DimenRes int resId) {
        mLastItemBottomSpace = getDimension(mContext, resId);
    }

    public void setVerticalSpace(@DimenRes int resId) {
        mLastItemBottomSpace = getDimension(mContext, resId);
    }

    private int getDimension(Context context, @DimenRes int resId) {
        //return (int) (context.getResources().getDimensionPixelOffset(resId) / context.getResources().getDisplayMetrics().density);
        return context.getResources().getDimensionPixelOffset(resId);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int viewPosition = parent.getChildAdapterPosition(view);
        int lastItemPosition = parent.getAdapter().getItemCount() - 1;

        if (viewPosition == 0) {
            outRect.top = mFirstItemTopSpace;
            outRect.bottom = mVerticalSpace;
        } else if (viewPosition == lastItemPosition) {
            outRect.bottom = mLastItemBottomSpace;
        } else {
            outRect.bottom = mVerticalSpace;
        }
    }

}