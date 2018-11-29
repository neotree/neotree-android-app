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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.neotree.NeoTree;
import org.neotree.R;
import org.neotree.event.PrintSessionSummaryEvent;
import org.neotree.model.realm.SessionEntry;
import org.neotree.support.NeoTreeHelper;
import org.neotree.support.android.BottomOffsetDecoration;
import org.neotree.support.android.SimpleDividerItemDecoration;
import org.neotree.support.datastore.RealmStore;
import org.neotree.ui.activity.ScriptPlayerActivity;
import org.neotree.ui.activity.SessionHistoryActivity;
import org.neotree.ui.core.ButterknifeViewHolder;
import org.neotree.ui.core.EnhancedFragment;

import butterknife.BindView;
import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;

/**
 * Created by matteo on 20/09/2016.
 */

public class SessionSummaryFragment extends EnhancedFragment {

    private static final String EXTRA_ADD_BOTTOM_PADDING = NeoTree.EXTRA_PREFIX + "add_bottom_padding";

    public static SessionSummaryFragment newInstance(String sessionId, boolean addBottomPadding) {
        Bundle args = new Bundle();
        args.putString(NeoTree.EXTRA_SESSION_ID, sessionId);
        args.putBoolean(EXTRA_ADD_BOTTOM_PADDING, addBottomPadding);

        SessionSummaryFragment fragment = new SessionSummaryFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @BindView(R.id.session_summary_recycler_view) RecyclerView mRecyclerView;

    private String mSessionId;
    private boolean mConfidential;

    @Override
    protected int getFragmentViewId() {
        return R.layout.fragment_session_summary;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onFragmentViewCreated(View view, Bundle savedInstanceState) {
        super.onFragmentViewCreated(view, savedInstanceState);

        mSessionId = getArguments().getString(NeoTree.EXTRA_SESSION_ID);

        loadSessionEntries();
    }

    @Override
    protected void onAttachToContext(Context context) {
        if (getActivity() instanceof ScriptPlayerActivity) {
            mConfidential = false;
        } else if (getActivity() instanceof SessionHistoryActivity) {
            // Coming from the session history it will not show confidential data
            mConfidential = true;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_session_detail, menu);
        applyMenuItemTint(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_print:
                getEventBus().send(new PrintSessionSummaryEvent(mSessionId, mConfidential));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == NeoTreeHelper.REQUEST_CODE_ADMIN_UNLOCK) {
            if (resultCode == Activity.RESULT_OK) {
                mConfidential = false;
                loadSessionEntries();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void loadSessionEntries() {
        try {
            RealmStore.loadEntriesForSession(getRealm(), mSessionId, mConfidential, result -> {
                Log.e("Result iss","Result iss"+result);
                if (result.isLoaded()) {
                    final SessionEntryAdapter adapter = new SessionEntryAdapter(getActivity(), result, mConfidential);
                    mRecyclerView.setAdapter(adapter);
                    mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
                    mRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(getActivity()));

                    boolean addBottomPadding = getArguments().getBoolean(EXTRA_ADD_BOTTOM_PADDING);
                    if (addBottomPadding) {
                        mRecyclerView.addItemDecoration(new BottomOffsetDecoration(getActivity(), R.dimen.recycler_view_bottom_spacing));
                    }
                    result.removeAllChangeListeners();
                }
            });
        }catch(Exception e){
            Toast.makeText(getActivity(),"Error"+e,Toast.LENGTH_LONG).show();
            Log.e("Relam exception iss","Relam exception iss"+e);}
    }

    private void showAdminUnlockDialog() {
        NeoTreeHelper.showAdminUnlockDialog(this);
    }

    private class SessionEntryAdapter extends RealmRecyclerViewAdapter<SessionEntry, RecyclerView.ViewHolder>
            implements View.OnClickListener {

        private static final int VIEW_TYPE_NOTICE = 100;
        private static final int VIEW_TYPE_ENTRY = 101;

        private boolean mConfidential;

        public SessionEntryAdapter(@NonNull Context context, @Nullable OrderedRealmCollection<SessionEntry> data, boolean confidential) {
            super(data, false);
            mConfidential = confidential;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            switch (viewType) {
                case VIEW_TYPE_NOTICE:
                    return new ConfidentialNoticeViewHolder(
                            inflater.inflate(R.layout.view_session_summary_notice, parent, false));
                case VIEW_TYPE_ENTRY:
                    return new SessionEntryViewHolder(
                            inflater.inflate(R.layout.view_session_summary_item, parent, false));
                default:
                    return null;
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int viewType = getItemViewType(position);
            if (viewType == VIEW_TYPE_NOTICE) {
                ConfidentialNoticeViewHolder vh = (ConfidentialNoticeViewHolder) holder;
                vh.unlockButton.setOnClickListener(this);
            } else if (viewType == VIEW_TYPE_ENTRY) {
                SessionEntry item = getItem(position);
                if (item != null) {
                    SessionEntryViewHolder vh = (SessionEntryViewHolder) holder;
                    vh.label.setText(item.getLabel());
                    vh.detail.setText(item.getValueAsString(getActivity()));
                }
            }
        }

        @Nullable
        @Override
        public SessionEntry getItem(int index) {
            return super.getItem(index + ((mConfidential) ? -1 : 0));
        }

        @Override
        public int getItemCount() {
            return super.getItemCount() + ((mConfidential) ? 1 : 0);
        }

        @Override
        public int getItemViewType(int position) {
            return (mConfidential && position == 0) ? VIEW_TYPE_NOTICE : VIEW_TYPE_ENTRY;
        }

        @Override
        public void onClick(View v) {
            showAdminUnlockDialog();
        }
    }

    static class SessionEntryViewHolder extends ButterknifeViewHolder {
        @BindView(R.id.session_summary_label) public TextView label;
        @BindView(R.id.session_summary_detail) public TextView detail;

        public SessionEntryViewHolder(View view) {
            super(view);
        }
    }

    static class ConfidentialNoticeViewHolder extends ButterknifeViewHolder {
        @BindView(R.id.session_summary_action_show) public Button unlockButton;

        public ConfidentialNoticeViewHolder(View view) {
            super(view);
        }
    }

}
