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

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import org.neotree.NeoTree;
import org.neotree.R;
import org.neotree.ui.core.EnhancedFragment;

import butterknife.BindView;

/**
 * Created by matteo on 20/09/2016.
 */

public class SummaryFragment extends EnhancedFragment implements ViewPager.OnPageChangeListener {

    private static final String EXTRA_ADD_BOTTOM_PADDING = NeoTree.EXTRA_PREFIX + "add_bottom_padding";

    public static SummaryFragment newInstance(String sessionId, boolean addBottomPadding) {
        Bundle args = new Bundle();
        args.putString(NeoTree.EXTRA_SESSION_ID, sessionId);
        args.putBoolean(EXTRA_ADD_BOTTOM_PADDING, addBottomPadding);

        SummaryFragment fragment = new SummaryFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @BindView(R.id.summary_tab_layout) TabLayout mTabLayout;
    @BindView(R.id.summary_viewpager)
    ViewPager mViewPager;

    private SummaryPagerAdapter mPagerAdapter;
    private String mSessionId;
    private boolean mAddBottomPadding;

    @Override
    protected int getFragmentViewId() {
        return R.layout.fragment_summary;
    }

    @Override
    public void onFragmentViewCreated(View view, Bundle savedInstanceState) {
        super.onFragmentViewCreated(view, savedInstanceState);
        mSessionId = getArguments().getString(NeoTree.EXTRA_SESSION_ID);
        mAddBottomPadding = getArguments().getBoolean(EXTRA_ADD_BOTTOM_PADDING);

        mPagerAdapter = new SummaryPagerAdapter(getChildFragmentManager());
        mViewPager.setAdapter(mPagerAdapter);

        mViewPager.addOnPageChangeListener(this);
        mTabLayout.setupWithViewPager(mViewPager);

        getEnhancedActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        getEnhancedActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    class SummaryPagerAdapter extends FragmentStatePagerAdapter {

        public SummaryPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return SessionSummaryFragment.newInstance(mSessionId, mAddBottomPadding);
                case 1:
                    return SessionDiagnosisFragment.newInstance(mSessionId, mAddBottomPadding);
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.title_tab_summary);
                case 1:
                    return getString(R.string.title_tab_diagnosis);
            }
            return null;
        }
    }

}
