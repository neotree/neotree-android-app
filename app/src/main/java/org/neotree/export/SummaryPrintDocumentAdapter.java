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

package org.neotree.export;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.neotree.NeoTree;
import org.neotree.R;
import org.neotree.diagnosis.DiagnosisException;
import org.neotree.diagnosis.DiagnosisResult;
import org.neotree.diagnosis.Doctor;
import org.neotree.model.firebase.Diagnosis;
import org.neotree.model.realm.Session;
import org.neotree.model.realm.SessionEntry;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by matteo on 22/09/2016.
 */

public class SummaryPrintDocumentAdapter extends PrintDocumentAdapter {

    private static final String TAG = SummaryPrintDocumentAdapter.class.getSimpleName();

    private static final int MILS_IN_INCH = 1000;

    private static final boolean ENABLE_DIAGNOSIS_PRINT = false;

    private Context mContext;
    private Session mSession;
    private List<SessionEntry> mEntries;
    private List<Diagnosis> mDiagnosisList;
    private DiagnosisResult mDiagnosisResult;

    private Context mPrintContext;
    private PrintAttributes mPrintAttributes;
    private PrintDocumentInfo mPrintDocumentInfo;

    private int mDensity;
    private int mRenderPageMarginTop;
    private int mRenderPageMarginBottom;
    private int mRenderPageMarginLeft;
    private int mRenderPageMarginRight;
    private int mRenderPageWidth;
    private int mRenderPageHeight;
    private int mPageCount;

    public SummaryPrintDocumentAdapter(Context context, Session session, List<SessionEntry> entries, List<Diagnosis> diagnoses) {
        mContext = context;
        mSession = session;
        mEntries = entries;
        mDiagnosisList = diagnoses;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle metadata) {
        // If we are already cancelled, don't do any work.
        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        boolean layoutNeeded = false;

        mDensity = Math.max(newAttributes.getResolution().getHorizontalDpi(), newAttributes.getResolution().getVerticalDpi());
        mRenderPageMarginLeft = (int) (mDensity * (float) newAttributes.getMinMargins().getLeftMils() / MILS_IN_INCH);
        mRenderPageMarginRight = (int) (mDensity * (float) newAttributes.getMinMargins().getRightMils() / MILS_IN_INCH);

        final int contentWidth = (int) (mDensity * (float) newAttributes.getMediaSize().getWidthMils() / MILS_IN_INCH) - mRenderPageMarginLeft - mRenderPageMarginRight;

        if (mRenderPageWidth != contentWidth) {
            mRenderPageWidth = contentWidth;
            layoutNeeded = true;
        }

        mRenderPageMarginTop = (int) (mDensity * (float) newAttributes.getMinMargins().getTopMils() / MILS_IN_INCH);
        mRenderPageMarginBottom = (int) (mDensity * (float) newAttributes.getMinMargins().getBottomMils() / MILS_IN_INCH);

        final int contentHeight = (int) (mDensity * (float) newAttributes.getMediaSize().getHeightMils() / MILS_IN_INCH) - mRenderPageMarginTop - mRenderPageMarginBottom;

        if (mRenderPageHeight != contentHeight) {
            mRenderPageHeight = contentHeight;
            layoutNeeded = true;
        }

        if (mPrintContext == null || mPrintContext.getResources().getConfiguration().densityDpi != mDensity) {
            Configuration configuration = new Configuration();
            configuration.densityDpi = mDensity;
            mPrintContext = mContext.createConfigurationContext(configuration);
            mPrintContext.setTheme(android.R.style.Theme_Holo_Light);
        }

        if (!layoutNeeded) {
            callback.onLayoutFinished(mPrintDocumentInfo, false);
            return;
        }


        final LayoutInflater layoutInflater = LayoutInflater.from(mPrintContext);

        new AsyncTask<Void, Void, PrintDocumentInfo>() {
            @Override
            protected void onPreExecute() {
                // First register for cancellation requests.
                cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
                    @Override
                    public void onCancel() {
                        cancel(true);
                    }
                });
                // Stash the attributes as we will need them for rendering.
                mPrintAttributes = newAttributes;
            }

            @SuppressWarnings("WrongThread")
            @Override
            protected PrintDocumentInfo doInBackground(Void... params) {
                // Run diagnose once and cache the result
                mDiagnosisResult = diagnose();

                final SessionEntryViewAdapter adapter = new SessionEntryViewAdapter(new ArrayList<>(mEntries), mDiagnosisResult, layoutInflater);

                // Stash the attributes as we will need them for rendering.
                mPrintAttributes = newAttributes;

                // Calculate the layout
                try {
                    LinearLayout dummyParent = new LinearLayout(mPrintContext);
                    dummyParent.setOrientation(LinearLayout.VERTICAL);

                    int currentPage = 0;
                    int currentColumn = 0;
                    int columnRenderHeight = 0;
                    int columnContentHeight = 0;
                    boolean isPageOverflow = false;
                    boolean isColumnOverflow = false;

                    View pageView = null;
                    View sectionView;
                    View entryView;

                    LinearLayout columnView = null;
                    ArrayList<LinearLayout> columnViews = null;

                    SessionEntry entry;
                    String lastSectionTitle = null;

                    final int itemCount = adapter.getCount();
                    for (int i = 0; i < itemCount; i++) {
                        // Be nice and respond to cancellation.
                        if (cancellationSignal.isCanceled()) {
                            callback.onLayoutCancelled();
                            return null;
                        }

                        entry = (SessionEntry) adapter.getItem(i);

                        // Create the first page
                        if (pageView == null) {
                            pageView = createPageView(layoutInflater, dummyParent, currentPage);

                            final View leftColumn = pageView.findViewById(R.id.pdf_page_body_left);
                            final View rightColumn = pageView.findViewById(R.id.pdf_page_body_right);

                            columnViews = new ArrayList<>();
                            columnViews.add((LinearLayout) leftColumn);
                            columnViews.add((LinearLayout) rightColumn);

                            currentColumn = 0;

                            columnView = columnViews.get(currentColumn);
                            columnRenderHeight = columnView.getMeasuredHeight() - columnView.getPaddingTop() - columnView.getPaddingBottom();
                            columnContentHeight = 0;
                        }

                        // Create a new section if required
                        String entrySectionTitle = null;
                        boolean isSectionChanged = false;
                        if (entry != null) {
                            entrySectionTitle = (entry.getSectionTitle() != null) ? entry.getSectionTitle().trim() : "Generic answer section";
                            isSectionChanged = lastSectionTitle == null || !lastSectionTitle.equalsIgnoreCase(entrySectionTitle);
                            if (lastSectionTitle == null | isSectionChanged) {
                                lastSectionTitle = entrySectionTitle;
                            }
                        }

                        sectionView = null;
                        if (isSectionChanged) {
                            sectionView = createSectionView(layoutInflater, dummyParent, entrySectionTitle);
                            measureView(sectionView, columnView.getMeasuredWidth(), columnView.getMeasuredHeight());
                        }

                        // Get and measure the next entry view
                        entryView = adapter.getView(i, null, dummyParent);
                        measureView(entryView, columnView.getMeasuredWidth(), columnView.getMeasuredHeight());

                        // Check we can fit the entry in the current column
                        if (sectionView != null) {
                            isColumnOverflow = (columnContentHeight + sectionView.getMeasuredHeight() + entryView.getMeasuredHeight() > columnRenderHeight);
                        } else {
                            isColumnOverflow = (columnContentHeight + entryView.getMeasuredHeight() > columnRenderHeight);
                        }

                        // Check if we have a page overflow
                        isPageOverflow = isColumnOverflow && (currentColumn == columnViews.size() - 1);

                        if (isPageOverflow) {
                            // Increment page counter
                            currentPage++;

                            // Move content to the first columnt of the next page
                            pageView = createPageView(layoutInflater, dummyParent, currentPage);

                            final View leftColumn = pageView.findViewById(R.id.pdf_page_body_left);
                            final View rightColumn = pageView.findViewById(R.id.pdf_page_body_right);

                            columnViews = new ArrayList<>();
                            columnViews.add((LinearLayout) leftColumn);
                            columnViews.add((LinearLayout) rightColumn);

                            currentColumn = 0;

                            columnView = columnViews.get(currentColumn);
                            columnRenderHeight = columnView.getMeasuredHeight() - columnView.getPaddingTop() - columnView.getPaddingBottom();
                            columnContentHeight = 0;
                            if (sectionView != null) {
                                columnContentHeight += sectionView.getMeasuredHeight();
                            }
                            columnContentHeight += entryView.getMeasuredHeight();
                        } else {
                            if (isColumnOverflow) {
                                // Move content to the next column
                                currentColumn++;

                                columnView = columnViews.get(currentColumn);
                                columnRenderHeight = columnView.getMeasuredHeight() - columnView.getPaddingTop() - columnView.getPaddingBottom();
                                columnContentHeight = 0;
                                if (sectionView != null) {
                                    columnContentHeight += sectionView.getMeasuredHeight();
                                }
                                columnContentHeight += entryView.getMeasuredHeight();
                            } else {
                                // Add content to the current column
                                if (sectionView != null) {
                                    columnContentHeight += sectionView.getMeasuredHeight();
                                }
                                columnContentHeight += entryView.getMeasuredHeight();
                            }
                        }
                    }

                    mPageCount = currentPage + 1;

                    Log.d(TAG, String.format("Pdf layout completed. Total pages [count=%d]", mPageCount));

                    // Create a document info describing the result.
                    PrintDocumentInfo info = new PrintDocumentInfo
                            .Builder(String.format(Locale.getDefault(), "%s.pdf", mSession.getSessionId()))
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(mPageCount)
                            .build();

                    // Notify of layout completed
                    callback.onLayoutFinished(info, true);

                    // Update the print document info
                    return info;
                } catch (Exception e) {
                    callback.onLayoutFailed(null);
                    throw new RuntimeException(e);
                }
            }

            @Override
            protected void onPostExecute(PrintDocumentInfo result) {
                mPrintDocumentInfo = result;
            }

            @Override
            protected void onCancelled(PrintDocumentInfo result) {
                callback.onLayoutCancelled();
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination, CancellationSignal cancellationSignal, PrintDocumentAdapter.WriteResultCallback callback) {
        // If we are already cancelled, don't do any work.
        if (cancellationSignal.isCanceled()) {
            callback.onWriteCancelled();
            return;
        }

        new AsyncTask<Void, Void, Void>() {

            final SparseIntArray mWrittenPages = new SparseIntArray();
            final PrintedPdfDocument mPdfDocument = new PrintedPdfDocument(mPrintContext, mPrintAttributes);

            @Override
            protected void onPreExecute() {
                // First register for cancellation requests.
                cancellationSignal.setOnCancelListener(() -> cancel(true));
            }

            @SuppressWarnings("WrongThread")
            @Override
            protected Void doInBackground(Void... params) {
                // The content is laid out and rendered in screen pixels with the width and height of the
                // paper size times the print density but the PDF canvas size is in points which are 1/72",
                // so we will scale down the content.
                final float scale = Math.min(
                        (float) mPdfDocument.getPageContentRect().width() / mRenderPageWidth,
                        (float) mPdfDocument.getPageContentRect().height() / mRenderPageHeight);

                final LayoutInflater layoutInflater = LayoutInflater.from(mPrintContext);
                final SessionEntryViewAdapter adapter = new SessionEntryViewAdapter(mEntries, mDiagnosisResult, layoutInflater);

                PdfDocument.Page pdfPage = null;
                ArrayList<View> pageViews = new ArrayList<>();

                LinearLayout dummyParent = new LinearLayout(mPrintContext);
                dummyParent.setOrientation(LinearLayout.VERTICAL);

                int currentPage = 0;
                int currentColumn = 0;
                int columnRenderHeight = 0;
                int columnContentHeight = 0;
                boolean isPageOverflow = false;
                boolean isColumnOverflow = false;

                View pageView = null;
                View sectionView = null;
                View entryView = null;

                LinearLayout columnView = null;
                ArrayList<LinearLayout> columnViews = null;

                SessionEntry entry;
                String lastSectionTitle = null;

                final int itemCount = adapter.getCount();
                Log.d(TAG, String.format("Total items [count=%d]", itemCount));

                for (int i = 0; i < itemCount; i++) {
                    // Be nice and respond to cancellation.
                    if (cancellationSignal.isCanceled()) {
                        callback.onWriteCancelled();
                        return null;
                    }

                    final boolean isFirstItem = (i == 0);
                    entry = (SessionEntry) adapter.getItem(i);

                    // Create the first page
                    if (pageView == null) {
                        pageView = createPageView(layoutInflater, dummyParent, currentPage);

                        final View leftColumn = pageView.findViewById(R.id.pdf_page_body_left);
                        final View rightColumn = pageView.findViewById(R.id.pdf_page_body_right);

                        columnViews = new ArrayList<>();
                        columnViews.add((LinearLayout) leftColumn);
                        columnViews.add((LinearLayout) rightColumn);

                        currentColumn = 0;

                        columnView = columnViews.get(currentColumn);
                        columnRenderHeight = columnView.getMeasuredHeight() - columnView.getPaddingTop() - columnView.getPaddingBottom();
                        columnContentHeight = 0;
                    }

                    // Create a new section if required
                    String entrySectionTitle = null;
                    boolean isSectionChanged = false;
                    if (entry != null) {
                        entrySectionTitle = (entry.getSectionTitle() != null) ? entry.getSectionTitle().trim() : "Generic answer section";
                        isSectionChanged = lastSectionTitle == null || !lastSectionTitle.equalsIgnoreCase(entrySectionTitle);
                        if (lastSectionTitle == null | isSectionChanged) {
                            lastSectionTitle = entrySectionTitle;
                        }
                    }

                    sectionView = null;
                    if (isSectionChanged) {
                        sectionView = createSectionView(layoutInflater, dummyParent, entrySectionTitle);
                        measureView(sectionView, columnView.getMeasuredWidth(), columnView.getMeasuredHeight());
                    }

                    // Get and measure the next entry view
                    entryView = adapter.getView(i, null, dummyParent);
                    measureView(entryView, columnView.getMeasuredWidth(), columnView.getMeasuredHeight());

                    // Check we can fit the entry in the current column
                    if (sectionView != null) {
                        isColumnOverflow = (columnContentHeight + sectionView.getMeasuredHeight() + entryView.getMeasuredHeight() > columnRenderHeight);
                    } else {
                        isColumnOverflow = (columnContentHeight + entryView.getMeasuredHeight() > columnRenderHeight);
                    }

                    // Check if we have a page overflow
                    isPageOverflow = isColumnOverflow && (currentColumn == columnViews.size() - 1);

                    if (isPageOverflow) {
                        currentPage++;

                        // Done with the current page - finish it.
                        if (pdfPage != null) {
                            Log.d(TAG, String.format("Finish pdf page at item [i=%d]", i));
                            finishCurrentPage(pageView, pageViews, pdfPage);
                            mPdfDocument.finishPage(pdfPage);
                        }

                        // Move content to the first columnt of the next page
                        pageView = createPageView(layoutInflater, dummyParent, currentPage);

                        final View leftColumn = pageView.findViewById(R.id.pdf_page_body_left);
                        final View rightColumn = pageView.findViewById(R.id.pdf_page_body_right);

                        columnViews = new ArrayList<>();
                        columnViews.add((LinearLayout) leftColumn);
                        columnViews.add((LinearLayout) rightColumn);

                        currentColumn = 0;

                        columnView = columnViews.get(currentColumn);
                        columnRenderHeight = columnView.getMeasuredHeight() - columnView.getPaddingTop() - columnView.getPaddingBottom();
                        columnContentHeight = 0;
                        if (sectionView != null) {
                            sectionView.layout(columnView.getLeft(), columnContentHeight,
                                    columnView.getLeft() + sectionView.getMeasuredWidth(),
                                    columnContentHeight + sectionView.getMeasuredHeight());
                            columnContentHeight += sectionView.getMeasuredHeight();

                            if (pdfPage != null) {
                                pageViews.add(sectionView);
                            }
                        }
                        entryView.layout(columnView.getLeft(), columnContentHeight,
                                columnView.getLeft() + entryView.getMeasuredWidth(),
                                columnContentHeight + entryView.getMeasuredHeight());
                        columnContentHeight += entryView.getMeasuredHeight();

                    } else {
                        if (isColumnOverflow) {
                            // Move content to the next column
                            currentColumn++;

                            columnView = columnViews.get(currentColumn);
                            //columnRenderHeight = columnView.getMeasuredHeight() - columnView.getPaddingTop() - columnView.getPaddingBottom();
                            columnContentHeight = 0;
                            if (sectionView != null) {
                                sectionView.layout(columnView.getLeft(), columnContentHeight,
                                        columnView.getLeft() + sectionView.getMeasuredWidth(),
                                        columnContentHeight + sectionView.getMeasuredHeight());
                                columnContentHeight += sectionView.getMeasuredHeight();

                                if (pdfPage != null) {
                                    pageViews.add(sectionView);
                                }
                            }
                            entryView.layout(columnView.getLeft(), columnContentHeight,
                                    columnView.getLeft() + entryView.getMeasuredWidth(),
                                    columnContentHeight + entryView.getMeasuredHeight());
                            columnContentHeight += entryView.getMeasuredHeight();
                        } else {
                            // Add content to the current column
                            if (sectionView != null) {
                                sectionView.layout(columnView.getLeft(), columnContentHeight,
                                        columnView.getLeft() + sectionView.getMeasuredWidth(),
                                        columnContentHeight + sectionView.getMeasuredHeight());
                                columnContentHeight += sectionView.getMeasuredHeight();

                                if (isFirstItem || pdfPage != null) {
                                    pageViews.add(sectionView);
                                }
                            }
                            entryView.layout(columnView.getLeft(), columnContentHeight,
                                    columnView.getLeft() + entryView.getMeasuredWidth(),
                                    columnContentHeight + entryView.getMeasuredHeight());
                            columnContentHeight += entryView.getMeasuredHeight();
                        }
                    }

                    // If the page is requested, create the page for rendering.
                    if (isFirstItem || isPageOverflow) {
                        if (containsPage(pages, currentPage)) {
                            Log.d(TAG, String.format("Create pdf page at item [i=%d]", i));
                            pdfPage = mPdfDocument.startPage(currentPage);
                            pdfPage.getCanvas().scale(scale, scale);

                            // Render the pageView
                            pageView.layout(mRenderPageMarginLeft, mRenderPageMarginTop, pageView.getMeasuredWidth(), pageView.getMeasuredHeight());
                            pageView.draw(pdfPage.getCanvas());

                            // Keep track which pages are written.
                            mWrittenPages.append(mWrittenPages.size(), currentPage);

                            if (currentPage > 0) {
                                pageViews = new ArrayList<>();
                            }
                        } else {
                            pdfPage = null;
                            pageViews = new ArrayList<>();
                        }
                    }

                    // If the current view is on a requested page, add it to the render queue.
                    if (pdfPage != null) {
                        pageViews.add(entryView);
                    }
                }

                // Done with the last page.
                if (pdfPage != null) {
                    Log.d(TAG, "Finish last page");
                    finishCurrentPage(pageView, pageViews, pdfPage);
                    mPdfDocument.finishPage(pdfPage);
                }

                // Be nice and respond to cancellation.
                if (cancellationSignal.isCanceled()) {
                    callback.onWriteCancelled();
                    return null;
                }

                // Write the data and return success or failure.
                try {
                    mPdfDocument.writeTo(new FileOutputStream(destination.getFileDescriptor()));
                    // Compute which page ranges were written based on the bookkeeping we maintained.
                    PageRange[] pageRanges = computeWrittenPageRanges(mWrittenPages);
                    callback.onWriteFinished(pageRanges);
                } catch (IOException ioe) {
                    callback.onWriteFailed(null);
                } finally {
                    mPdfDocument.close();
                }

                return null;
            }

            @Override
            protected void onCancelled(Void result) {
                // Task was cancelled, report that.
                callback.onWriteCancelled();
                mPdfDocument.close();
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
    }

    private LinearLayout createPageView(LayoutInflater layoutInflater, ViewGroup parent, int currentPage) {
        LinearLayout pageView = (LinearLayout) layoutInflater.inflate(R.layout.view_pdf_page, parent, false);

        if (currentPage > 0) {
            pageView.findViewById(R.id.pdf_cover_title).setVisibility(View.GONE);
        }
        ((TextView) pageView.findViewById(R.id.pdf_page_session_id)).setText(mSession.getSessionId());
        ((TextView) pageView.findViewById(R.id.pdf_page_creation_date)).setText(formatDateTime(mPrintContext, mSession.getCreatedAt()));
        ((TextView) pageView.findViewById(R.id.pdf_page_number)).setText(mPrintContext.getString(R.string.pdf_label_page, currentPage + 1, mPageCount));

        final int widthMeasureSpec = ViewGroup.getChildMeasureSpec(
                View.MeasureSpec.makeMeasureSpec(mRenderPageWidth, View.MeasureSpec.EXACTLY), 0, pageView.getLayoutParams().width);
        final int heightMeasureSpec = ViewGroup.getChildMeasureSpec(
                View.MeasureSpec.makeMeasureSpec(mRenderPageHeight, View.MeasureSpec.EXACTLY), 0, pageView.getLayoutParams().height);
        pageView.measure(widthMeasureSpec, heightMeasureSpec);
        pageView.layout(0, 0, mRenderPageWidth, mRenderPageHeight);

        return pageView;
    }

    private View createSectionView(LayoutInflater layoutInflater, ViewGroup parent, String title) {
        View sectionView = layoutInflater.inflate(R.layout.view_pdf_section, parent, false);
        ((TextView) sectionView.findViewById(R.id.pdf_section_title)).setText(title);
        return sectionView;
    }

    private DiagnosisResult diagnose() {
        Map<String, Object> entriesMap = null;
        for (SessionEntry entry : mEntries) {
            if (entriesMap == null) {
                entriesMap = new HashMap<>();
            }
            entriesMap.put(entry.getKey(), entry.getValue());
        }

        try {
            return Doctor.diagnose(mDiagnosisList, entriesMap);
        } catch (DiagnosisException e) {
            Log.e(TAG, "Error running diagnosis", e);
        }
        return null;
    }

    private void measureView(View view, int parentWidth, int parentHeight) {
        final int widthMeasureSpec = ViewGroup.getChildMeasureSpec(
                View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY), 0, view.getLayoutParams().width);
        final int heightMeasureSpec = ViewGroup.getChildMeasureSpec(
                View.MeasureSpec.makeMeasureSpec(parentHeight, View.MeasureSpec.UNSPECIFIED), 0, view.getLayoutParams().height);
        view.measure(widthMeasureSpec, heightMeasureSpec);
    }

    private void finishCurrentPage(View pageView, ArrayList<View> bodyViews, PdfDocument.Page pdfPage) {
        final View bodyContainerView = pageView.findViewById(R.id.pdf_page_body_container);

        // Render all entries for the current page
        int bodyLeft = pageView.getPaddingLeft() + pageView.getLeft();
        int bodyTop = bodyContainerView.getTop() + pageView.getPaddingTop();

        Canvas pdfCanvas = pdfPage.getCanvas();
        pdfCanvas.translate(bodyLeft, bodyTop);

        for (View childView : bodyViews) {
            pdfCanvas.save();
            pdfCanvas.translate(childView.getLeft(), childView.getTop());
            childView.draw(pdfPage.getCanvas());
            pdfCanvas.restore();
        }
    }

    @SuppressLint("IntRange")
    private PageRange[] computeWrittenPageRanges(SparseIntArray writtenPages) {
        List<PageRange> pageRanges = new ArrayList<>();
        int start = -1;
        int end = -1;
        final int writtenPageCount = writtenPages.size();
        for (int i = 0; i < writtenPageCount; i++) {
            if (start < 0) {
                start = writtenPages.valueAt(i);
            }

            int oldEnd = end = start;
            while (i < writtenPageCount && (end - oldEnd) <= 1) {
                oldEnd = end;
                end = writtenPages.valueAt(i);
                i++;
            }

            //noinspection Range
            PageRange pageRange = new PageRange(start, end);
            pageRanges.add(pageRange);
            start = end = -1;
        }
        PageRange[] pageRangesArray = new PageRange[pageRanges.size()];
        pageRanges.toArray(pageRangesArray);
        return pageRangesArray;
    }

    private boolean containsPage(PageRange[] pageRanges, int page) {
        final int pageRangeCount = pageRanges.length;
        for (int i = 0; i < pageRangeCount; i++) {
            if (pageRanges[i].getStart() <= page && pageRanges[i].getEnd() >= page) {
                return true;
            }
        }
        return false;
    }

    private String formatDateTime(Context context, String value) {
        return (TextUtils.isEmpty(value)) ? context.getString(R.string.label_not_set)
                : DateTime.parse(value, ISODateTimeFormat.dateTimeNoMillis())
                .toString(NeoTree.NeoTreeFormat.DATETIME);
    }

    private class SessionEntryViewAdapter extends BaseAdapter {

        private static final int VIEW_TYPE_ENTRY = 100;
        private static final int VIEW_TYPE_DIAGNOSIS = 101;

        private final List<SessionEntry> mItems;
        private final LayoutInflater mInflater;
        private final DiagnosisResult mDiagnosisResult;

        public SessionEntryViewAdapter(List<SessionEntry> items, DiagnosisResult diagnosisResult, LayoutInflater inflater) {
            mItems = items;
            mDiagnosisResult = diagnosisResult;
            mInflater = inflater;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int viewType = getItemViewType(position);
            switch (viewType) {
                case VIEW_TYPE_ENTRY:
                    if (convertView == null) {
                        convertView = mInflater.inflate(R.layout.view_pdf_entry, parent, false);
                    }

                    SessionEntry entry = (SessionEntry) getItem(position);

                    TextView textEntryLabel = (TextView) convertView.findViewById(R.id.pdf_entry_label);
                    textEntryLabel.setText(entry.getLabel());

                    TextView textEntryValue = (TextView) convertView.findViewById(R.id.pdf_entry_value);
                    textEntryValue.setText(entry.getValueAsString(mPrintContext));
                    break;

                case VIEW_TYPE_DIAGNOSIS:
                    if (convertView == null) {
                        convertView = mInflater.inflate(R.layout.view_pdf_diagnosis_section, parent, false);
                    }

                    LinearLayout diagnosesContainer = (LinearLayout) convertView.findViewById(R.id.pdf_diagnosis_container);
                    LinearLayout managementContainer = (LinearLayout) convertView.findViewById(R.id.pdf_management_container);

                    if (mDiagnosisResult.getDiagnosisCount() > 0) {
                        for (int i = 0; i < mDiagnosisResult.getDiagnosisCount(); i++) {
                            View view = mInflater.inflate(R.layout.view_pdf_diagnosis_entry, diagnosesContainer, false);
                            ((TextView) view.findViewById(R.id.pdf_entry_label)).setText(mDiagnosisResult.getDiagnosisName(i));
                            diagnosesContainer.addView(view);
                        }
                    } else {
                        convertView.findViewById(R.id.pdf_diagnosis_title).setVisibility(View.GONE);
                    }

                    if (mDiagnosisResult.getManagementCount() > 0) {
                        for (int i = 0; i < mDiagnosisResult.getManagementCount(); i++) {
                            View view = mInflater.inflate(R.layout.view_pdf_diagnosis_entry, managementContainer, false);
                            ((TextView) view.findViewById(R.id.pdf_entry_label)).setText(mDiagnosisResult.getDiagnosisManagement(i).text);
                            managementContainer.addView(view);
                        }
                    } else {
                        convertView.findViewById(R.id.pdf_management_title).setVisibility(View.GONE);
                    }
                    break;
            }
            return convertView;
        }

        @Override
        public int getCount() {
            return ((mItems != null) ? mItems.size() : 0) + ((ENABLE_DIAGNOSIS_PRINT) ? 1 : 0);
        }

        @Override
        public Object getItem(int position) {
            if (ENABLE_DIAGNOSIS_PRINT) {
                if (position == getCount() - 1) {
                    // Last position is a fake entry to display the diagnosis section
                    return null;
                }
            }
            return (mItems != null) ? mItems.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            if (ENABLE_DIAGNOSIS_PRINT) {
                if (position == getCount() - 1) {
                    // Last position is a fake entry to display the diagnosis section
                    return VIEW_TYPE_DIAGNOSIS;
                }
            }
            return VIEW_TYPE_ENTRY;
        }

    }

}

