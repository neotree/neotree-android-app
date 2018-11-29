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

package org.neotree.ui.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import org.neotree.R;
import org.neotree.event.PrintSessionSummaryEvent;
import org.neotree.event.RefreshSessionListEvent;
import org.neotree.event.ShowSessionDetailEvent;
import org.neotree.export.SummaryExportManager;
import org.neotree.inject.ActivityComponent;
import org.neotree.inject.module.ActivityModule;
import org.neotree.model.firebase.Diagnosis;
import org.neotree.model.realm.Session;
import org.neotree.support.android.AndroidHelper;
import org.neotree.support.datastore.FirebaseStore;
import org.neotree.support.datastore.RealmStore;
import org.neotree.support.rx.RxHelper;
import org.neotree.ui.core.EnhancedActivity;
import org.neotree.ui.fragment.DataExportFragment;
import org.neotree.ui.fragment.SessionHistoryListFragment;
import org.neotree.ui.fragment.SummaryFragment;

import java.util.List;

import butterknife.BindView;
import rx.Observable;

public class SessionHistoryActivity extends EnhancedActivity<ActivityComponent> {

    @BindView(R.id.session_history_toolbar)
    Toolbar mToolbar;

    private List<Diagnosis> mDiagnosisList;
    private int mDeleteMode = -1;

    @Override
    protected int getActivityViewId() {
        return R.layout.activity_session_history;
    }

    @Override
    protected ActivityComponent setupActivityComponent() {
        return getApplicationComponent().plus(new ActivityModule(this));
    }

    @Override
    public void onCreateAfterSetContentView(Bundle savedInstanceState) {
        super.onCreateAfterSetContentView(savedInstanceState);

        setSupportActionBar(mToolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        if (savedInstanceState == null) {
            showSessionHistoryList();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                setTitle(R.string.title_activity_session_history);
                onBackPressed();
                break;
            case R.id.menu_action_export:
                setTitle(R.string.title_activity_export_settings);
                showGlobalExportSettings();
                break;
            case R.id.menu_action_delete_incomplete:
                showDeleteConfirmation(this);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onEventReceived(Object event) {
        if (event instanceof ShowSessionDetailEvent) {
            ShowSessionDetailEvent e = (ShowSessionDetailEvent) event;
            showSessionSummaryDetail(e.sessionId);
        } else if (event instanceof PrintSessionSummaryEvent) {
            PrintSessionSummaryEvent e = (PrintSessionSummaryEvent) event;
            Session session = RealmStore.loadSession(getRealm(), e.sessionId);
            addSubscription(FirebaseStore.get().loadDiagnosis(session.getScriptId())
                    .compose(RxHelper.applySchedulers())
                    .flatMap(diagnosisList -> {
                        mDiagnosisList = diagnosisList;
                        return Observable.just(RealmStore.loadEntriesForSession(getRealm(), e.sessionId, e.confidential));
                    })
                    .subscribe(result -> {
                        SummaryExportManager.print(this, getRealm(), e.sessionId, result, mDiagnosisList);
                    }, throwable -> {
                        Log.e(logTag(), "Error!", throwable);
                    })
            );
        }
    }

    private void showDeleteConfirmation(Context context) {
        CharSequence[] items = new CharSequence[] {
                context.getString(R.string.label_history_delete_incomplete),
                context.getString(R.string.label_history_delete_all),
        };

        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.label_history_delete_title)
                .setSingleChoiceItems(items, -1, (d, index) -> {
                    mDeleteMode = index;
                })
                .setNegativeButton(R.string.label_action_cancel, (d, i) -> d.dismiss())
                .setPositiveButton(R.string.label_action_ok, (d, i) -> {
                    final RealmStore.OnRealmTransactionListener listener = () -> {
                        getEventBus().send(new RefreshSessionListEvent());
                    };

                    switch (mDeleteMode) {
                        case 0:
                            RealmStore.deleteIncompleteSessions(getRealm(), listener);
                            break;
                        case 1:
                            RealmStore.deleteAllSessions(getRealm(), listener);
                            break;
                        default:
                            break;
                    }
                })
                .create();
        dialog.show();
    }

    private void showSessionHistoryList() {
        AndroidHelper.replaceFragment(getFragmentManager(),
                R.id.content_frame,
                SessionHistoryListFragment.newInstance(),
                false);
    }

    private void showSessionSummaryDetail(String sessionId) {
        setTitle(R.string.title_activity_session_detail);

        AndroidHelper.replaceFragment(getFragmentManager(),
                R.id.content_frame,
                SummaryFragment.newInstance(sessionId, false),
                true);
    }

    private void showGlobalExportSettings() {
        setTitle(R.string.title_activity_export_settings);

        AndroidHelper.replaceFragment(getFragmentManager(),
                R.id.content_frame,
                DataExportFragment.newInstance(),
                true);
    }

}


