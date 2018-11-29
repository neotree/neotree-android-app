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

package org.neotree.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.database.DatabaseException;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.neotree.NeoTree;
import org.neotree.model.firebase.Screen;
import org.neotree.model.firebase.Script;
import org.neotree.model.realm.Session;
import org.neotree.model.realm.SessionValue;
import org.neotree.player.expression.BooleanExpressionEvaluator;
import org.neotree.player.validator.KeyValue;
import org.neotree.grammar.BooleanExpressionLexer;
import org.neotree.grammar.BooleanExpressionParser;
import org.neotree.support.datastore.RealmStore;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.realm.Realm;
import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * Created by matteo on 31/07/2016.
 */
public class ScriptPlayer {

    private static final String TAG = ScriptPlayer.class.getSimpleName();

    public interface ScriptPlayerListener {
        void onScriptReady();
        void onScriptEmpty();
        void onCurrentScreenUpdated(Screen screen);
        void onScriptError(String message, Throwable throwable);
    }

    private Context mContext;
    private Deque<Integer> mScreenStack;

    private Script mScript;
    private List<Screen> mScreens;
    private Map<String, Integer> mScreenIdToIndexMap;
    private Map<String, Object> mValues;
    private ScriptPlayerListener mListener;

    private Session mSession;
    private Realm mRealm;

    private PublishSubject<KeyValue> mValueChangeSubject = PublishSubject.create();

    public ScriptPlayer(Context context, Realm realm, ScriptPlayerListener listener) {
        mContext = context;
        mListener = listener;
        mRealm = realm;
    }

    public void setPlayerData(Script script, List<Screen> screens) {
        mScreens = screens;
        preparePlayer(mContext, script, screens);
    }

    private void preparePlayer(Context context, Script script, List<Screen> screens) {
        Log.d(TAG, "Preparing to play script");

        if (screens == null || screens.size() == 0) {
            mListener.onScriptEmpty();
            return;
        }

        mScreenIdToIndexMap = new HashMap<>();
        mScreenStack = new ArrayDeque<>();
        mScript = script;
        mScreens = screens;
        mValues = new HashMap<>();

        // Load global configuration values
        SharedPreferences configPrefs = NeoTree.getConfigurationPreferences(context);
        mValues.putAll(configPrefs.getAll());

        try {
            // Map each screen id to its position
            for (int i = 0; i < mScreens.size(); i++) {
                final Screen screen = mScreens.get(i);
                mScreenIdToIndexMap.put(screen.screenId, i);
            }
        } catch (DatabaseException e) {
            Log.e(TAG, "Database exception", e );
        }

        // Store session in datastore
        mSession = RealmStore.createSession(mRealm, UUID.randomUUID().toString(), mScript.scriptId);

        // Notify script ready
        mListener.onScriptReady();
    }

    public void finishSession() {
        RealmStore.finishSession(mRealm, mSession);
    }

    public boolean isFirstScreen() {
        return (mScreenStack.size() == 1);
    }

    public Observable<KeyValue> valueChangeObservable() {
        return mValueChangeSubject;
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String key) {
        return (T) mValues.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getValue(String key, T defaultValue) {
        T value = (T) mValues.get(key);
        return (value != null) ? value : defaultValue;
    }

    public boolean hasValue(String key) {
        return mValues.containsKey(key);
    }

    public void setValue(String key, Object value) {
        Object oldValue = mValues.get(key.trim());
        if ((oldValue == null && value != null)
                || (oldValue != null && value == null)
                || (oldValue != null && !oldValue.equals(value))) {
            Log.d(TAG, String.format("Setting value [key=%s, value=%s]", key.trim(), value));
            mValues.put(key.trim(), value);

            // Publish key/value/type? pair to RX observers (for form validation)
            mValueChangeSubject.onNext(new KeyValue(key.trim(), value));
        }
    }

    public void storeValue(String sectionTitle, SessionValue value) {
        final int position = currentScreen().position; // mScreenStack.peek()
        RealmStore.storeValue(mRealm, mScript.scriptId, mSession.getSessionId(), sectionTitle, position, value);
    }

    public void storeValue(String key, String label, String sectionTitle, ArrayList<SessionValue> values) {
        final int position = currentScreen().position; // mScreenStack.peek()
        RealmStore.storeValue(mRealm, mScript.scriptId, mSession.getSessionId(), sectionTitle, position, key.trim(), label, values);
    }

    public Screen currentScreen() {
        final int position = mScreenStack.peek();
        return mScreens.get(position);
    }

    private boolean isNextScreen(Screen screen) {
        final String condition = screen.condition;
        if (TextUtils.isEmpty(condition)) {
            // Short circuit: screen with null condition is always visible
            return true;
        }

        try {
            BooleanExpressionLexer lexer = new BooleanExpressionLexer(new ANTLRInputStream(condition.trim()));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            BooleanExpressionParser parser = new BooleanExpressionParser(tokens);
            BooleanExpressionParser.RootContext tree = parser.root();

            BooleanExpressionEvaluator evaluator = new BooleanExpressionEvaluator(this);
            ParseTreeWalker.DEFAULT.walk(evaluator, tree);

            boolean result = evaluator.getEvaluationResult();
            return result;
        } catch (Exception e) {
            if (mListener != null) {
                notifyScriptError("The current screen contains an invalid conditional expression. Please check the configuration.", e);
            }
        }
        return true;
    }

    public Screen nextScreen() {
        if (mScreenStack.isEmpty()) {
            mScreenStack.push(0);
            return currentScreen();
        }

        int nextIndex = mScreenStack.peek() + 1;
        while (nextIndex < mScreens.size()) {
            final Screen screen = mScreens.get(nextIndex);
            Log.d(TAG, "Check next screen: " + screen.title);
            if (isNextScreen(screen)) {
                Log.d(TAG, "Showing as next screen...");
                mScreenStack.push(nextIndex);
                return currentScreen();
            } else {
                Log.d(TAG, "Skip...");
            }
            nextIndex++;
        }
        return null;
    }

    public Screen previousScreen() throws ScriptPlayerException {
        // TODO: Verify order of operation is correct?
        mScreenStack.pop();

        if (mScreenStack.isEmpty()) {
            return null;
        }
        return currentScreen();
    }

    public void updateScreen(Screen screen) {
        if (mScreenIdToIndexMap.containsKey(screen.screenId)) {
            final int position = mScreenIdToIndexMap.get(screen.screenId);
            mScreens.set(position, screen);
            if (mListener != null) {
                mListener.onCurrentScreenUpdated(screen);
            }
        }
    }

    public void notifyScriptError(String message, Throwable throwable) {
        mListener.onScriptError(message, throwable);
    }

    public Session getSession() {
        return mSession;
    }

    public String getSessionId() {
        return mSession.getSessionId();
    }
}
