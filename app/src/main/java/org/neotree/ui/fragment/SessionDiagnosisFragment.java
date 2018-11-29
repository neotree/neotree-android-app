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
import android.support.v7.app.AlertDialog;
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

import com.crashlytics.android.Crashlytics;

import org.neotree.NeoTree;
import org.neotree.R;
import org.neotree.diagnosis.DiagnosisException;
import org.neotree.diagnosis.DiagnosisManagement;
import org.neotree.diagnosis.DiagnosisResult;
import org.neotree.diagnosis.Doctor;
import org.neotree.model.firebase.Diagnosis;
import org.neotree.model.firebase.FileInfo;
import org.neotree.model.realm.Session;
import org.neotree.model.realm.SessionEntry;
import org.neotree.support.android.BottomOffsetDecoration;
import org.neotree.support.datastore.FirebaseStore;
import org.neotree.support.datastore.RealmStore;
import org.neotree.support.rx.RxHelper;
import org.neotree.ui.core.ButterknifeViewHolder;
import org.neotree.ui.core.EnhancedFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import rx.Observable;
import rx.exceptions.Exceptions;

/**
 * Created by matteo on 20/09/2016.
 */

public class SessionDiagnosisFragment extends EnhancedFragment {

    private static final String EXTRA_ADD_BOTTOM_PADDING = NeoTree.EXTRA_PREFIX + "add_bottom_padding";

    public static SessionDiagnosisFragment newInstance(String sessionId, boolean addBottomPadding) {
        Bundle args = new Bundle();
        args.putString(NeoTree.EXTRA_SESSION_ID, sessionId);
        args.putBoolean(EXTRA_ADD_BOTTOM_PADDING, addBottomPadding);

        SessionDiagnosisFragment fragment = new SessionDiagnosisFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @BindView(R.id.session_diagnosis_recycler_view) RecyclerView mRecyclerView;

    private String mSessionId;
    private String mScriptId;
    private List<Diagnosis> mDiagnosisList;
    private Map<String, Object> mEntriesMap;
    private DiagnosisViewAdapter mListViewAdapter;

    @Override
    protected int getFragmentViewId() {
        return R.layout.fragment_session_diagnosis;
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

        Session session = RealmStore.loadSession(getRealm(), mSessionId);
        mScriptId = session.getScriptId();

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
//        mRecyclerView.addItemDecoration(new VerticalSpacingItemDecoration(getActivity(), !getScreen().hasContentText(), true));
        mRecyclerView.addItemDecoration(new BottomOffsetDecoration(getActivity(), R.dimen.recycler_view_bottom_spacing));

        mListViewAdapter = new DiagnosisViewAdapter();
        mRecyclerView.setAdapter(mListViewAdapter);

        diagnose();
    }

    private void diagnose() {
        // Load diagnosis for script
        addSubscription(FirebaseStore.get().loadDiagnosis(mScriptId)
                .compose(RxHelper.applySchedulers())
                .flatMap(diagnosisList -> {
                    mDiagnosisList = diagnosisList;
                    return Observable.just(RealmStore.loadEntriesForSession(getRealm(), mSessionId, false));
                })
                .map(sessionEntries -> {
                    for (SessionEntry entry : sessionEntries) {
                        if (mEntriesMap == null) {
                            mEntriesMap = new HashMap<>();
                        }
                        mEntriesMap.put(entry.getKey(), entry.getValue());
                    }
                    try {
                        return Doctor.diagnose(mDiagnosisList, mEntriesMap);
                    } catch (DiagnosisException e) {
                        throw Exceptions.propagate(e);
                    }
                })
                .subscribe(result -> {
                    mListViewAdapter.setDiagnosisResult(result);
                }, this::showDiagnosisError)
        );
    }

    public void showDiagnosisError(Throwable throwable) {
        Log.e(logTag(), "Diagnosis Error", throwable);
        Crashlytics.logException(throwable);

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
                .setMessage(throwable.getMessage())
                .setNeutralButton(R.string.label_action_close, (d, i) -> d.dismiss())
                .create();
        dialog.show();
    }

    private class DiagnosisViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_DIAGNOSIS_TITLE = 100;
        private static final int VIEW_TYPE_DIAGNOSIS_ITEM = 101;
        private static final int VIEW_TYPE_MANAGEMENT_TITLE = 102;
        private static final int VIEW_TYPE_MANAGEMENT_ITEM = 103;

        private DiagnosisResult mDiagnosisResult;
        private ArrayList<Integer> mViewTypes;

        public DiagnosisViewAdapter() {
            super();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            switch (viewType) {
                case VIEW_TYPE_DIAGNOSIS_TITLE:
                case VIEW_TYPE_MANAGEMENT_TITLE:
                    return new DiagnosisTitleViewHolder(
                            inflater.inflate(R.layout.view_diagnosis_title, parent, false));
                case VIEW_TYPE_DIAGNOSIS_ITEM:
                    return new DiagnosisItemViewHolder(
                            inflater.inflate(R.layout.view_diagnosis_item, parent, false));
                case VIEW_TYPE_MANAGEMENT_ITEM:
                    return new DiagnosisManagementItemViewHolder(
                            inflater.inflate(R.layout.view_diagnosis_management_item, parent, false));
                default:
                    return null;
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int viewType = getItemViewType(position);
            if (viewType == VIEW_TYPE_DIAGNOSIS_TITLE) {
                DiagnosisTitleViewHolder vh = (DiagnosisTitleViewHolder) holder;
                vh.title.setText(R.string.label_diagnosis_title_diagnoses);
            } else if (viewType == VIEW_TYPE_MANAGEMENT_TITLE) {
                DiagnosisTitleViewHolder vh = (DiagnosisTitleViewHolder) holder;
                vh.title.setText(R.string.label_diagnosis_title_management);
            } else if (viewType == VIEW_TYPE_DIAGNOSIS_ITEM) {
                DiagnosisItemViewHolder vh = (DiagnosisItemViewHolder) holder;
                int index = position - 1;
                vh.text.setText(String.format("- %s", mDiagnosisResult.getDiagnosisName(index)));
            } else if (viewType == VIEW_TYPE_MANAGEMENT_ITEM) {
                DiagnosisManagementItemViewHolder vh = (DiagnosisManagementItemViewHolder) holder;

                int index = position - mDiagnosisResult.getDiagnosisCount() - 2;
                DiagnosisManagement management = mDiagnosisResult.getDiagnosisManagement(index);

                if (!TextUtils.isEmpty(management.text)) {
                    vh.text.setText(management.text);
                    vh.text.setVisibility(View.VISIBLE);
                } else {
                    vh.text.setVisibility(View.GONE);
                }

                if (management.fileInfo != null) {
                    vh.image.setVisibility(View.GONE);

                    try {
                        FileInfo info = management.fileInfo;
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
                        Log.e(logTag(), "Error decoding image", e);
                    }
                }
            }
        }

        @Override
        public int getItemCount() {
            return (mViewTypes != null) ? mViewTypes.size() : 0;
        }

        @Override
        public int getItemViewType(int position) {
            return (mViewTypes != null) ? mViewTypes.get(position) : -1;
        }

        public void setDiagnosisResult(DiagnosisResult diagnosisResult) {
            mDiagnosisResult = diagnosisResult;

            if (mDiagnosisResult == null) {
                mViewTypes = null;
            } else {
                ArrayList<Integer> viewTypes = new ArrayList<>();
                if (mDiagnosisResult.getDiagnosisCount() > 0) {
                    viewTypes.add(VIEW_TYPE_DIAGNOSIS_TITLE);
                    for (int i = 0; i < mDiagnosisResult.getDiagnosisCount(); i++) {
                        viewTypes.add(VIEW_TYPE_DIAGNOSIS_ITEM);
                    }
                }

                if (mDiagnosisResult.getManagementCount() > 0) {
                    viewTypes.add(VIEW_TYPE_MANAGEMENT_TITLE);
                    for (int i = 0; i < mDiagnosisResult.getManagementCount(); i++) {
                        viewTypes.add(VIEW_TYPE_MANAGEMENT_ITEM);
                    }
                }

                mViewTypes = viewTypes;
            }

            notifyDataSetChanged();
        }
    }

    static class DiagnosisTitleViewHolder extends ButterknifeViewHolder {
        @BindView(R.id.title) public TextView title;

        public DiagnosisTitleViewHolder(View view) {
            super(view);
        }
    }

    static class DiagnosisItemViewHolder extends ButterknifeViewHolder {
        @BindView(R.id.text) public TextView text;

        public DiagnosisItemViewHolder(View view) {
            super(view);
        }
    }

    static class DiagnosisManagementItemViewHolder extends ButterknifeViewHolder {
        @BindView(R.id.management_item_text) public TextView text;
        @BindView(R.id.management_item_image) public ImageView image;

        public DiagnosisManagementItemViewHolder(View view) {
            super(view);
        }
    }


}
