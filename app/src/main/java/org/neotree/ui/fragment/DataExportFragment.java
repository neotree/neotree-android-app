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

import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v13.app.FragmentCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import com.crashlytics.android.Crashlytics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.neotree.R;
import org.neotree.model.firebase.Field;
import org.neotree.model.firebase.Item;
import org.neotree.model.firebase.Metadata;
import org.neotree.model.firebase.Screen;
import org.neotree.model.firebase.Script;
import org.neotree.model.realm.SessionEntry;
import org.neotree.model.realm.SessionValue;
import org.neotree.player.type.DataType;
import org.neotree.player.type.ScreenType;
import org.neotree.support.datastore.FirebaseStore;
import org.neotree.support.datastore.RealmStore;
import org.neotree.support.rx.RxHelper;
import org.neotree.support.rx.data.Pair;
import org.neotree.ui.core.EnhancedFragment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import butterknife.BindView;
import butterknife.OnClick;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import rx.Observable;

/**
 * Created by matteo on 20/09/2016.
 */

public class DataExportFragment extends EnhancedFragment {

    private static final String TAG = DataExportFragment.class.getSimpleName();

    private static final int REQUEST_WRITE_STORAGE = 1000;

    private enum ExportFormat {
        EXCEL, JSON
    }

    public static DataExportFragment newInstance() {
        return new DataExportFragment();
    }

    @BindView(R.id.export_radio_format_excel)
    RadioButton mExcelRadioButton;
    @BindView(R.id.export_radio_format_json)
    RadioButton mJsonRadioButton;
    @BindView(R.id.export_export_action)
    Button mExportButton;
    @BindView(R.id.export_wip_overlay)
    LinearLayout mExportingOverlay;

    private ExportFormat mExportFormat = ExportFormat.EXCEL;
    private AtomicInteger mRunningExportCount = new AtomicInteger(0);

    @Override
    protected int getFragmentViewId() {
        return R.layout.fragment_data_export;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onFragmentViewCreated(View view, Bundle savedInstanceState) {
        super.onFragmentViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkWriteStoragePermission();
    }

    @Override
    @TargetApi(23)
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE:
                boolean granted = (grantResults[0] == PackageManager.PERMISSION_GRANTED);
                mExportButton.setEnabled(granted);
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    @TargetApi(23)
    private void checkWriteStoragePermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            FragmentCompat.requestPermissions(this,
                    new String[] { android.Manifest.permission.WRITE_EXTERNAL_STORAGE }, REQUEST_WRITE_STORAGE);
        }
    }

    @OnClick({R.id.export_radio_format_excel, R.id.export_radio_format_json})
    void onOutputFormatClick(View v) {
        if (v == mExcelRadioButton) {
            mExportFormat = ExportFormat.EXCEL;
        } else if (v == mJsonRadioButton) {
            mExportFormat = ExportFormat.JSON;
        }
    }

    @OnClick(R.id.export_export_action)
    void onExportActionClick() {
        addSubscription(fetchExportData()
                .compose(RxHelper.applySchedulers())
                .doOnNext(exportData -> {
                    mRunningExportCount.incrementAndGet();
                })
                .doOnError(throwable -> {
                    showExportInProgress(false);
                    Crashlytics.logException(throwable);
                })
                .doOnSubscribe(() -> {
                    mRunningExportCount.set(0);
                    showExportInProgress(true);
                })
                .map(exportData -> {
                    Log.d(TAG, String.format("Exporting data for script: %s", exportData.getScript().title));
                    return export(exportData);
                })
                .subscribe(result -> {
                    int count = mRunningExportCount.decrementAndGet();
                    if (count == 0) {
                        showExportInProgress(false);
                        Snackbar.make(getCoordinatorLayout(), R.string.message_snackbar_export_done, Snackbar.LENGTH_SHORT)
                                .show();
                    }
                })
        );
    }

    private Observable<ExportData> fetchExportData() {
        final FirebaseStore store = FirebaseStore.get();
        return store.loadScripts()
                .compose(RxHelper.applySchedulers())
                .flatMap(scripts -> {
                    if (scripts == null) {
                        return Observable.empty();
                    }
                    List<Observable<ExportData>> loaders = new ArrayList<>();
                    for (Script script : scripts) {
                        loaders.add(store.loadScreens(script.scriptId)
                                .flatMap(screens -> Observable.just(new Pair<>(script, screens)))
                                .flatMap(this::fetchScriptSessionData)
                        );
                    }
                    return Observable.merge(loaders);
                });
    }

    private Observable<ExportData> fetchScriptSessionData(Pair<Script, List<Screen>> metadata) {
        return Observable.just(RealmStore.loadEntriesForScript(getRealm(), metadata.getValue1().scriptId, true))
                .map((data -> new ExportData(metadata.getValue1(), metadata.getValue2(), data)));
    }

    private boolean export(ExportData exportData) {
        switch (mExportFormat) {
            case EXCEL:
                return exportAsExcelSpreadsheet(exportData);
            case JSON:
                return exportAsJson(exportData);
            default:
                return false;
        }
    }

    private boolean exportAsExcelSpreadsheet(ExportData exportData) {
        if (exportData.getEntries() == null || exportData.getEntries().size() == 0) {
            Log.d(TAG, "Nothing to export for script");
            return false;
        }

        final ArrayList<String> headers = new ArrayList<>();
        final HashMap<String, Integer> columnMap = new HashMap<>();
        final List<Screen> screens = exportData.getScreens();

        int columnIndex = 0;

        for (int screenIndex = 0; screens != null && screenIndex < screens.size(); screenIndex++) {
            final Screen screen = screens.get(screenIndex);
            final ScreenType screenType = ScreenType.fromString(screen.type);
            final Metadata metadata = screen.metadata;

            // Map screen keys
            String screenKey = (TextUtils.isEmpty(metadata.key)) ? null : metadata.key.trim();
            if (screenKey != null && !metadata.confidential) {
                if (screenType == ScreenType.MULTI_SELECT) {
                    String itemKey;
                    for (int itemIndex = 0; itemIndex < metadata.items.size(); itemIndex++) {
                        final Item item = metadata.items.get(itemIndex);
                        itemKey = String.format("%s_%s", screenKey, item.id);
                        Log.d(TAG, String.format("Mapping item key (multiple selection) [key=%s, index=%d]", itemKey, columnIndex));
                        columnMap.put(itemKey, columnIndex++);
                        headers.add(itemKey);
                    }
                } else {
                    Log.d(TAG, String.format("Mapping screen key [key=%s, index=%d]", screenKey, columnIndex));
                    columnMap.put(screenKey, columnIndex++);
                    headers.add(screenKey);
                }
            } else {
                if (screenType == ScreenType.FORM) {
                    // Map field keys
                    String fieldKey = null;
                    if (metadata.fields != null && metadata.fields.size() > 0) {
                        for (int fieldIndex = 0; fieldIndex < metadata.fields.size(); fieldIndex++) {
                            final Field field = metadata.fields.get(fieldIndex);
                            fieldKey = (TextUtils.isEmpty(field.key)) ? null : field.key.trim();

                            if (fieldKey != null && !field.confidential) {
                                Log.d(TAG, String.format("Mapping field key [key=%s, index=%d]", fieldKey, columnIndex));
                                columnMap.put(fieldKey, columnIndex++);
                                headers.add(fieldKey);
                            }
                        }
                    }
                } else {
                    // Map item keys
                    if (metadata.items != null) {
                        if (screenType == ScreenType.CHECKLIST) {
                            String itemKey;
                            for (int itemIndex = 0; itemIndex < metadata.items.size(); itemIndex++) {
                                final Item item = metadata.items.get(itemIndex);
                                itemKey = (TextUtils.isEmpty(item.key)) ? null : item.key.trim();

                                if (itemKey != null && !item.confidential) {
                                    Log.d(TAG, String.format("Mapping item key (checklist) [key=%s, index=%d]", itemKey, columnIndex));
                                    columnMap.put(itemKey, columnIndex++);
                                    headers.add(itemKey);
                                }
                            }
                        }
                    }
                }
            }
        }

        WritableWorkbook workbook = null;
        try {
            File exportRootDir = Environment.getExternalStoragePublicDirectory("NeoTree");
            if(!exportRootDir.isDirectory()){
                if (!exportRootDir.mkdirs()) {
                    throw new IOException("Error creating output directory: " + exportRootDir.getAbsolutePath());
                }
            }

            File noMediaFile = new File(exportRootDir, ".nomedia");
            if (!noMediaFile.exists()) {
                if (!noMediaFile.createNewFile()) {
                    throw new IOException("Error creating .nomedia file: " + noMediaFile.getAbsolutePath());
                }
            }

            String title = exportData.getScript().title;
            String filename = String.format("%s-%s.xls",
                    DateTime.now().toString(DateTimeFormat.forPattern("yyyyMMddHHmm")),
                    title.replaceAll("[^a-zA-Z0-9]", "_")
            );
            File exportFile = new File(exportRootDir, filename);

            Log.d(TAG, "Filename :" + filename);
            Log.d(TAG, "File path:" + exportFile.getAbsolutePath());
            WorkbookSettings wbSettings = new WorkbookSettings();
            wbSettings.setLocale(Locale.ENGLISH);

            workbook = Workbook.createWorkbook(exportFile, wbSettings);

            WritableSheet sheet = workbook.createSheet(title, 0);

            // Add headers
            for (int c = 0; c < headers.size(); c++) {
                sheet.addCell(new Label(c, 0, headers.get(c)));
            }

            // Add rows
            String sessionId = null;
            int rowIndex = 0;

            for (SessionEntry entry : exportData.getEntries()) {
                if (sessionId == null || !sessionId.equals(entry.getSessionId())) {
                    sessionId = entry.getSessionId();
                    rowIndex++;
                }

                DataType dataType = entry.getDataTypeAsObject();
                switch (dataType) {
                    case SET_ID:
                        if (entry.getValues() != null) {
                            for (SessionValue value : entry.getValues()) {
                                String itemKey = String.format("%s_%s", value.getKey(), value.getStringValue());
                                try {
                                    Label cell = new Label(columnMap.get(itemKey), rowIndex, "Yes");
                                    sheet.addCell(cell);
                                } catch (Exception e) {
                                    Log.e(TAG, String.format("item key for set does not exist: %s", itemKey), e);
                                }
                            }
                        }
                        break;
                    default:
                        String key = entry.getKey();
                        try {
                            if (!TextUtils.isEmpty(key) && key.contains(" ")) {
                                key = key.replaceAll("\\s+", "");
                            }

                            String content = entry.getSingleValue().getValueAsExportString(getActivity());
                            sheet.addCell(new Label(columnMap.get(key), rowIndex, content));
                        } catch (Exception e) {
                            Log.e(TAG, String.format("item key does not exist: %s", key), e);
                        }
                }
            }

            workbook.write();

            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(getActivity(),
                    new String[] { exportFile.toString() }, null, (path, uri) -> {
                        Log.d(TAG, String.format("Success exporting data [path=%s, uri=%s]", path, uri));
                    });

        } catch (IOException | WriteException e) {
            Log.e(TAG, "Error exporting Excel file", e);
            Crashlytics.logException(e);
            return false;
        } finally {
            try {
                if (workbook != null) {
                    workbook.close();
                }
            } catch (IOException | WriteException e) {
                Log.e(TAG, "Error closing workbook file", e);
                Crashlytics.logException(e);
                return false;
            }
        }
        return true;
    }

    private boolean exportAsJson(ExportData exportData) {
        if (exportData.getEntries() == null || exportData.getEntries().size() == 0) {
            Log.d(TAG, "Nothing to export for script");
            return false;
        }

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode();
        ArrayNode jsonSessions = root.putArray("sessions");

        ObjectNode jsonSession = null;
        ArrayNode jsonSessionEntries = null;
        ObjectNode jsonEntry;
        ArrayNode jsonEntryValues;
        String sessionId = null;

        for (SessionEntry entry : exportData.getEntries()) {
            if (sessionId == null || !sessionId.equals(entry.getSessionId())) {
                if (jsonSession != null) {
                    jsonSessions.add(jsonSession);
                }
                sessionId = entry.getSessionId();
                jsonSession = mapper.createObjectNode();
                jsonSession.put("scriptTitle", sessionId);

                ObjectNode jsonScript = jsonSession.putObject("script");
                jsonScript.put("id", exportData.getScript().scriptId);
                jsonScript.put("title", exportData.getScript().title);

                jsonSessionEntries = jsonSession.putArray("entries");
            }

            jsonEntry = mapper.createObjectNode();
            jsonEntry.put("key", entry.getKey());
            jsonEntry.put("type", entry.getDataType());
            jsonEntryValues = jsonEntry.putArray("values");

            jsonSessionEntries.add(jsonEntry);

            DataType dataType = entry.getDataTypeAsObject();
            ObjectNode jsonValue;
            SessionValue value;
            switch (dataType) {
                case BOOLEAN:
                case DATE:
                case DATETIME:
                case STRING:
                case ID:
                case NUMBER:
                case PERIOD:
                case TIME:
                    value = entry.getSingleValue();

                    jsonValue = mapper.createObjectNode();
                    jsonValue.put("label", value.getValueLabel());
                    switch (dataType) {
                        case BOOLEAN:
                            jsonValue.put("value", value.getBooleanValue());
                            break;
                        case DATE:
                        case DATETIME:
                        case STRING:
                        case ID:
                            jsonValue.put("value", value.getStringValue());
                            break;
                        case NUMBER:
                            jsonValue.put("value", value.getDoubleValue());
                            break;
                        case PERIOD:
                        case TIME:
                            jsonValue.put("value", value.getValueAsFormattedString(getActivity()));
                            break;
                    }
                    jsonEntryValues.add(jsonValue);
                    break;

                case SET_ID:
                    if (entry.getValues() != null) {
                        for (SessionValue sessionValue : entry.getValues()) {
                            jsonValue = mapper.createObjectNode();
                            jsonValue.put("label", sessionValue.getValueLabel());
                            jsonValue.put("value", sessionValue.getStringValue());
                            jsonEntryValues.add(jsonValue);
                        }
                    }
                    break;

                default:
                    break;
            }
        }

        try {
            File exportRootDir = Environment.getExternalStoragePublicDirectory("NeoTree");
            if(!exportRootDir.isDirectory()){
                if (!exportRootDir.mkdirs()) {
                    throw new IOException("Error creating output directory: " + exportRootDir.getAbsolutePath());
                }
            }

            File noMediaFile = new File(exportRootDir, ".nomedia");
            if (!noMediaFile.exists()) {
                if (!noMediaFile.createNewFile()) {
                    throw new IOException("Error creating .nomedia file: " + noMediaFile.getAbsolutePath());
                }
            }

            String title = exportData.getScript().title;
            String filename = String.format("%s-%s.json",
                    DateTime.now().toString(DateTimeFormat.forPattern("yyyyMMddHHmm")),
                    title.replaceAll("[^a-zA-Z0-9]", "_")
            );
            File exportFile = new File(exportRootDir, filename);

            // Write JSON output
            mapper.writeValue(exportFile, root);

            // Tell the media scanner about the new file so that it is
            // immediately available to the user.
            MediaScannerConnection.scanFile(getActivity(),
                    new String[] { exportFile.toString() }, null, (path, uri) -> {
                        Log.d(TAG, String.format("Success exporting data [path=%s, uri=%s]", path, uri));
                    });

        } catch (IOException e) {
            Log.e(TAG, "Error exporting Excel file", e);
            Crashlytics.logException(e);
            return false;
        }

        return true;
    }

    class ExportData {
        private Script mScript;
        private List<Screen> mScreens;
        private List<SessionEntry> mEntries;

        public ExportData(Script script, List<Screen> screens, List<SessionEntry> entries) {
            mScript = script;
            mScreens = screens;
            mEntries = entries;
        }

        public Script getScript() {
            return mScript;
        }

        public List<Screen> getScreens() {
            return mScreens;
        }

        public List<SessionEntry> getEntries() {
            return mEntries;
        }

        @Override
        public String toString() {
            return "ExportData{" +
                    "mScript=" + mScript +
                    ", mScreens=" + mScreens +
                    ", mEntries=" + mEntries +
                    '}';
        }
    }

    private void showExportInProgress(boolean show) {
        Log.d(TAG, "Show dialog: " + show);
        mExportingOverlay.setVisibility((show) ? View.VISIBLE : View.GONE);
        mExportButton.setEnabled(!show);
    }

}
