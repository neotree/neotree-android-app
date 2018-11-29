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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.neotree.NeoTree;
import org.neotree.R;
import org.neotree.event.RefreshSessionListEvent;
import org.neotree.event.ShowSessionDetailEvent;
import org.neotree.model.firebase.Script;
import org.neotree.model.realm.Session;
import org.neotree.support.android.VerticalSpacingItemDecoration;
import org.neotree.support.datastore.FirebaseStore;
import org.neotree.support.datastore.RealmStore;
import org.neotree.ui.core.ButterknifeViewHolder;
import org.neotree.ui.core.EnhancedFragment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;

/**
 * Created by matteo on 20/09/2016.
 */

public class SessionHistoryListFragment extends EnhancedFragment
        implements View.OnClickListener, View.OnLongClickListener {

    public static SessionHistoryListFragment newInstance() {
        Bundle args = new Bundle();
        SessionHistoryListFragment fragment = new SessionHistoryListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getFragmentViewId() {
        return R.layout.fragment_session_history_list;
    }

    @BindView(R.id.session_history_list_recycler_view) RecyclerView mRecyclerView;

    private SessionListAdapter mListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_session_history, menu);
        applyMenuItemTint(menu);
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.menu_action_export:
//                //SummaryExportManager.print(getActivity(), getRealm(), mSessionId, false);
//                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }

    @Override
    public void onFragmentViewCreated(View view, Bundle savedInstanceState) {
        super.onFragmentViewCreated(view, savedInstanceState);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.addItemDecoration(new VerticalSpacingItemDecoration(getActivity()));

        reloadSessions();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.title_activity_session_history);
    }

    @Override
    public void onClick(View v) {
        String sessionId = (String) v.getTag();
        getEventBus().send(new ShowSessionDetailEvent(sessionId));
    }

    @Override
    public boolean onLongClick(View v) {
        final String sessionId = (String) v.getTag();
        showDeleteConfirmation(getActivity(), sessionId);
        return true;
    }

    @Override
    protected void onEventReceived(Object event) {
        if (event instanceof RefreshSessionListEvent) {
            reloadSessions();
        }
    }

    private void reloadSessions() {
        RealmStore.loadSessions(getRealm(), result -> {
            if (result.isLoaded()) {
                List<Script> scripts = FirebaseStore.get().loadScriptsSync();
                mListAdapter = new SessionListAdapter(getActivity(), result, scripts);
                mRecyclerView.setAdapter(mListAdapter);
                result.removeAllChangeListeners();
            }
        });
    }

    private void showDeleteConfirmation(Context context, final String sessionId) {
        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.label_history_delete_title)
                .setMessage(R.string.message_session_delete_single)
                .setNegativeButton(R.string.label_action_cancel, (d, i) -> d.dismiss())
                .setPositiveButton(R.string.label_action_ok, (d, i) -> {
                    final RealmStore.OnRealmTransactionListener listener = () -> {
                        getEventBus().send(new RefreshSessionListEvent());
                    };

                    RealmStore.deleteSingleSession(sessionId, getRealm(), listener);
                })
                .create();
        dialog.show();
    }

    class SessionListAdapter extends RealmRecyclerViewAdapter<Session, SessionViewHolder> {

        private Map<String, String> mScriptTitleMap = new HashMap<>();
        private Context mContext;

        public SessionListAdapter(@NonNull Context context, @Nullable OrderedRealmCollection<Session> data, List<Script> scripts) {
            super(data, false);
            mContext = context;

            if (scripts != null) {
                for (Script script : scripts) {
                    mScriptTitleMap.put(script.scriptId, script.title);
                }
            }
        }

        @Override
        public SessionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new SessionViewHolder(
                    inflater.inflate(R.layout.view_session_item, parent, false),
                    SessionHistoryListFragment.this,
                    SessionHistoryListFragment.this);
        }

        @Override
        public void onBindViewHolder(SessionViewHolder holder, int position) {
            final Session item = getData().get(position);

            final String startDate = item.getCreatedAt();
            holder.startDate.setText((TextUtils.isEmpty(startDate)
                    ? mContext.getString(R.string.label_not_set)
                    : DateTime.parse(startDate).toString(NeoTree.NeoTreeFormat.DATETIME)
            ));

            final String endDate = item.getCompletedAt();
            holder.endDate.setText((TextUtils.isEmpty(endDate)
                    ? mContext.getString(R.string.label_not_set)
                    : DateTime.parse(endDate).toString(NeoTree.NeoTreeFormat.DATETIME)
            ));

            String scriptTitle = mScriptTitleMap.get(item.getScriptId());
            if (scriptTitle == null) {
                scriptTitle = "Unknown";
            }
            holder.scriptTitle.setText(scriptTitle);

            final String sessionId = item.getSessionId();
            holder.setTag(sessionId);
        }
    }

    class SessionViewHolder extends ButterknifeViewHolder {
        @BindView(R.id.session_item_wrapper) public LinearLayout wrapper;
        @BindView(R.id.session_item_script_title) public TextView scriptTitle;
        @BindView(R.id.session_item_start_date) public TextView startDate;
        @BindView(R.id.session_item_end_date) public TextView endDate;

        public SessionViewHolder(View view, View.OnClickListener clickListener, View.OnLongClickListener longClickListener) {
            super(view);
            wrapper.setOnClickListener(clickListener);
            wrapper.setOnLongClickListener(longClickListener);
        }

        public void setTag(Object tag) {
            wrapper.setTag(tag);
        }
    }

}
