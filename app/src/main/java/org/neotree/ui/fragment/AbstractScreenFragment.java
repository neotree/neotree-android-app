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
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.mittsu.markedview.MarkedView;

import org.neotree.NeoTree;
import org.neotree.R;
import org.neotree.event.NextActionEnableEvent;
import org.neotree.model.firebase.Metadata;
import org.neotree.model.firebase.Screen;
import org.neotree.model.firebase.Script;
import org.neotree.player.ScriptPlayer;
import org.neotree.support.datastore.FirebaseStore;
import org.neotree.ui.activity.ScriptPlayerActivity;
import org.neotree.ui.core.EnhancedFragment;

/**
 * Created by matteo on 14/07/2016.
 */
public abstract class AbstractScreenFragment extends EnhancedFragment {

    private Screen mScreen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onFragmentViewCreated(View view, Bundle savedInstanceState) {
        super.onFragmentViewCreated(view, savedInstanceState);

        // Retrieve the screen object from arguments
        mScreen = getArguments().getParcelable(NeoTree.EXTRA_SCREEN);
        if (mScreen == null) {
            throw new IllegalStateException("Screen object is required");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isLiveReloadEnabled()) {
            addSubscription(FirebaseStore.get().observeScreens(getScript().scriptId).subscribe(event -> {
                if (event.isTypeChange()) {
                    Log.d(logTag(), String.format("Screen child has changed [key=%s]", event.key));
                    if (getScreenId().equals(event.key)) {
                        Log.d(logTag(), "Screen has changed, updating...");
                        getScriptPlayer().updateScreen(event.value);
                    }
                }
            }));
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.activity_script_player, menu);

        MenuItem menuScreenInfo = menu.findItem(R.id.menu_action_script_screen_info);
        Screen currentScreen = getScriptPlayer().currentScreen();
        menuScreenInfo.setVisible(!TextUtils.isEmpty(currentScreen.infoText));

        applyMenuItemTint(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_action_script_screen_info:
                showInfoTextDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected ScriptPlayer getScriptPlayer() {
        return ((ScriptPlayerActivity) getActivity()).getScriptPlayer();
    }

    protected Script getScript() {
        return ((ScriptPlayerActivity) getActivity()).getScript();
    }

    protected Screen getScreen() {
        return mScreen;
    }

    protected String getScreenId() {
        return mScreen.screenId;
    }

    protected Metadata getScreenMetadata() {
        return mScreen.metadata;
    }

    protected void setActionNextEnabled(boolean enabled) {
        getEventBus().send(new NextActionEnableEvent(enabled));
    }

    private void showInfoTextDialog() {
        Screen screen = getScriptPlayer().currentScreen();

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        int spacingHoriz = getActivity().getResources().getDimensionPixelSize(R.dimen.view_horizontal_spacing_default);
        int spacingVert = getActivity().getResources().getDimensionPixelSize(R.dimen.view_vertical_spacing_large);
        int dialogHeight = displaymetrics.heightPixels - (6 * spacingVert);

        View contentView = LayoutInflater.from(getActivity()).inflate(R.layout.view_dialog_content_info_text, null);
        contentView.setMinimumHeight(dialogHeight);

        MarkedView contentText = (MarkedView) contentView.findViewById(R.id.info_dialog_content_text);
        contentText.setMDText(screen.infoText);

//        TextView contentText = (TextView) contentView.findViewById(R.id.info_dialog_content_text);
//        NeoTreeHelper.processMarkdown(contentText, screen.infoText);

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setView(contentView)
                .setNegativeButton(R.string.label_action_close, (d, i) -> {
                    d.dismiss();
                })
                .create();
        dialog.show();
    }

}
