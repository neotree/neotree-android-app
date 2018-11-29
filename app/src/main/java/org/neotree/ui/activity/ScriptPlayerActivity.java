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

import android.app.Fragment;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.neotree.NeoTree;
import org.neotree.R;
import org.neotree.event.NextActionEnableEvent;
import org.neotree.event.PrintSessionSummaryEvent;
import org.neotree.export.SummaryExportManager;
import org.neotree.inject.ActivityComponent;
import org.neotree.inject.module.ActivityModule;
import org.neotree.model.firebase.Diagnosis;
import org.neotree.model.firebase.Screen;
import org.neotree.model.firebase.Script;
import org.neotree.player.ScriptPlayer;
import org.neotree.player.type.ScreenType;
import org.neotree.support.android.AndroidHelper;
import org.neotree.support.datastore.FirebaseStore;
import org.neotree.support.datastore.RealmStore;
import org.neotree.support.rx.RxHelper;
import org.neotree.ui.core.EnhancedActivity;
import org.neotree.ui.fragment.ChecklistScreenFragment;
import org.neotree.ui.fragment.FormScreenFragment;
import org.neotree.ui.fragment.ManagementScreenFragment;
import org.neotree.ui.fragment.ProgressScreenFragment;
import org.neotree.ui.fragment.SelectScreenFragment;
import org.neotree.ui.fragment.SimpleListScreenFragment;
import org.neotree.ui.fragment.SummaryFragment;
import org.neotree.ui.fragment.TimerScreenFragment;
import org.neotree.ui.fragment.YesNoScreenFragment;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import rx.Observable;

public class ScriptPlayerActivity extends EnhancedActivity<ActivityComponent>
        implements ScriptPlayer.ScriptPlayerListener {

    @BindView(R.id.script_player_toolbar)
    Toolbar mToolbar;
    @BindView(R.id.script_player_action_text)
    TextView mActionText;
    @BindView(R.id.script_player_step_text)
    TextView mStepText;
    @BindView(R.id.script_player_next_button) FloatingActionButton mNextButton;
    @BindView(R.id.script_player_finish_button) FloatingActionButton mFinishButton;

    // TODO: Save/Restore ScriptPlayer context
    private ScriptPlayer mScriptPlayer;
    private Script mScript;
    private List<Diagnosis> mDiagnosisList;
    private boolean mIsSummaryScreen;

    @Override
    protected int getActivityViewId() {
        return R.layout.activity_script_player;
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

        mScript = getIntent().getParcelableExtra(NeoTree.EXTRA_SCRIPT);
        if (mScript == null) {
            throw new IllegalStateException("Script was null");
        }

        Log.d(logTag(), String.format("Starting script \"%s\"", mScript.title));
        setTitle(mScript.title);

        addSubscription(FirebaseStore.get().loadScreens(mScript.scriptId)
                .compose(RxHelper.applySchedulers())
                .subscribe((screens -> {
                    mScriptPlayer = new ScriptPlayer(this, getRealm(), this);
                    mScriptPlayer.setPlayerData(mScript, screens);
                }))
        );
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.menu_action_script_cancel:
                showCancelScriptWarning();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mIsSummaryScreen) {
            super.onBackPressed();
            mFinishButton.hide();
            mIsSummaryScreen = false;
        } else if (!mScriptPlayer.isFirstScreen()) {
            showPreviousScreen();
        } else {
            showCancelScriptWarning();
        }
    }

    @OnClick(R.id.script_player_next_button)
    public void onActionNextClick() {
        showNextScreen();
    }

    @OnClick(R.id.script_player_finish_button)
    public void onActionFinishClick() {
        finish();
    }

    @Override
    public void onScriptReady() {
        // Get started
        showNextScreen();
    }

    @Override
    public void onScriptEmpty() {
        Log.d(logTag(), "onScriptEmpty()");
    }

    @Override
    public void onScriptError(String message, Throwable throwable) {
        Log.e(logTag(), "Script error", throwable);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setMessage(message)
                .setNeutralButton(R.string.label_action_close, (d, i) -> {
                    d.dismiss();
                })
                .create();
        dialog.show();
    }

    @Override
    public void onCurrentScreenUpdated(Screen screen) {
        Log.d(logTag(), String.format("onCurrentScreenUpdated() - [%s]", screen.screenId));
        showScreen(screen);
    }

    @Override
    protected void onEventReceived(Object event) {
        if (event instanceof NextActionEnableEvent) {
            if (mNextButton != null) {
                if (((NextActionEnableEvent) event).enabled) {
                    mNextButton.show();
                } else {
                    mNextButton.hide();
                }
            }
        } else if (event instanceof PrintSessionSummaryEvent) {
            PrintSessionSummaryEvent e = (PrintSessionSummaryEvent) event;
            addSubscription(FirebaseStore.get().loadDiagnosis(getScript().scriptId)
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

    private void showCancelScriptWarning() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.label_script_cancel_title)
                .setMessage(R.string.label_script_cancel_message)
                .setPositiveButton(R.string.label_action_ok, (d, which) -> finish())
                .setNegativeButton(R.string.label_action_cancel, (d, which) -> d.dismiss());
        builder.create().show();
    }

    private void showNextScreen() {
        final Screen screen = mScriptPlayer.nextScreen();
        if (screen != null) {
            showScreen(screen);
        } else {
            showSummaryScreen();
        }
    }

    private void showPreviousScreen() {
        final Screen screen = mScriptPlayer.previousScreen();
        if (screen != null) {
            showScreen(screen);
        } else {
            Snackbar.make(getCoordinatorLayout(), R.string.message_snackbar_beginning_of_script, Snackbar.LENGTH_LONG)
//                    .setAction(R.string.snackbar_action, myOnClickListener)
                    .show();
        }
    }

    private void showScreen(Screen screen) {
        Log.d(logTag(), String.format("Showing screen [type=%s]", screen.type));

        // Hide next button by default to prevent fast skipping
        mNextButton.hide();

        // Update current view with screen data
        setTitle(screen.title);
        setActionText(screen.actionText);
        setStepText(screen.step);

        // Show screen fragment
        Fragment screenFragment = null;
        switch (ScreenType.fromString(screen.type)) {
            case CHECKLIST:
                screenFragment = ChecklistScreenFragment.newInstance(screen);
                break;
            case FORM:
                screenFragment = FormScreenFragment.newInstance(screen);
                break;
            case LIST:
                screenFragment = SimpleListScreenFragment.newInstance(screen);
                break;
            case MANAGEMENT:
                screenFragment = ManagementScreenFragment.newInstance(screen);
                break;
            case MULTI_SELECT:
            case SINGLE_SELECT:
                screenFragment = SelectScreenFragment.newInstance(screen);
                break;
            case PROGRESS:
                screenFragment = ProgressScreenFragment.newInstance(screen);
                break;
            case TIMER:
                screenFragment = TimerScreenFragment.newInstance(screen);
                break;
            case YESNO:
                screenFragment = YesNoScreenFragment.newInstance(screen);
                break;
        }

        if (screenFragment != null) {
            AndroidHelper.replaceFragment(getFragmentManager(), R.id.script_player_content_frame, screenFragment, false);
        } else {
            Toast.makeText(this, "Unsupported screen type", Toast.LENGTH_LONG).show();
        }
    }

    private void showSummaryScreen() {
        mScriptPlayer.finishSession();

        mIsSummaryScreen = true;

        setTitle(R.string.title_activity_summary);
        setActionText(null);
        setStepText(null);

        mNextButton.hide();
        mFinishButton.show();

        AndroidHelper.replaceFragment(getFragmentManager(),
                R.id.script_player_content_frame,
                SummaryFragment.newInstance(getScriptPlayer().getSessionId(), true), false);
    }

    public void setActionText(String text) {
        if (mActionText == null) {
            return;
        }

        if (!TextUtils.isEmpty(text)) {
            mActionText.setText(text);
            mActionText.setVisibility(View.VISIBLE);
        } else {
            mActionText.setVisibility(View.GONE);
        }
    }

    public void setStepText(String text) {
        if (mStepText == null) {
            return;
        }

        if (!TextUtils.isEmpty(text)) {
            mStepText.setText(text);
            mStepText.setVisibility(View.VISIBLE);
        } else {
            mStepText.setVisibility(View.GONE);
        }
    }

    public ScriptPlayer getScriptPlayer() {
        return mScriptPlayer;
    }

    public Script getScript() {
        return mScript;
    }

//    @Override
//    protected void onEventReceived(Object event) {
//        if (event instanceof PreviousScreenEvent) {
//            Toast.makeText(this, "Show previous screen", Toast.LENGTH_SHORT).show();
//        } else if (event instanceof NextScreenEvent) {
//            showNextScreen();
//        }
//    }

}
