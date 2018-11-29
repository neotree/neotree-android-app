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

package org.neotree.player.validator;

import android.text.TextUtils;
import android.util.Log;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.neotree.model.firebase.Field;
import org.neotree.model.firebase.Metadata;
import org.neotree.model.firebase.Screen;
import org.neotree.player.ScriptPlayer;
import org.neotree.player.expression.BooleanExpressionEvaluator;
import org.neotree.player.type.FieldType;
import org.neotree.grammar.BooleanExpressionLexer;
import org.neotree.grammar.BooleanExpressionParser;
import org.neotree.support.datastore.RealmStore;
import org.neotree.support.rx.RxHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rx.Observable;
import rx.Subscription;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by matteo on 18/08/2016.
 */
public class FormManager {

    private static final String TAG = FormManager.class.getSimpleName();

    private Screen mScreen;
    private ScriptPlayer mScriptPlayer;

    private HashMap<String, BehaviorSubject<Object>> mFieldValueObservables = new HashMap<>();
    private BehaviorSubject<Boolean> mFormValidSubject = BehaviorSubject.create(false);

    private PublishSubject<FieldStatusInfo> mFieldStatusSubject = PublishSubject.create();
    private ArrayList<Boolean> mFieldEnabledStatuses = new ArrayList<>();

    private CompositeSubscription mSubscription;

    public FormManager(Screen screen, ScriptPlayer scriptPlayer) {
        mScreen = screen;
        mScriptPlayer = scriptPlayer;
    }

    public Observable<Boolean> validFormObservable() {
        return mFormValidSubject;
    }

    public Observable<FieldStatusInfo> fieldStatusObservable() {
        return mFieldStatusSubject;
    }

    public void subscribe() {
        final Metadata metadata = mScreen.metadata;
        final ArrayList<Field> fields = metadata.fields;

        // Short circuit validation for entire form
        boolean skippableScreen = false;
        if (mScreen.skippable || metadata == null || (metadata.fields == null || metadata.fields.size() == 0)) {
            mFormValidSubject.onNext(true);
            skippableScreen = true;
        }

        final List<Observable<Boolean>> validatorObservables = new ArrayList<>();

        for (int i = 0; fields != null && i < fields.size(); i++) {
            Field field = fields.get(i);

            // Evaluete field enabled status
            mFieldEnabledStatuses.add(evaluateFieldCondition(field));

            // Add value publisher for field
            BehaviorSubject<Object> fieldValueSubject = BehaviorSubject.create();
            fieldValueSubject.onNext(mScriptPlayer.getValue(field.key));

            mFieldValueObservables.put(field.key, fieldValueSubject);

            // Add field validation if the screen is not skippable or field is not optional
            if (!skippableScreen) {
                // Add validator for specific type
                FieldType fieldType = FieldType.fromString(field.type);
                switch (fieldType) {
                    case NUMBER:
                        // Validate number
                        validatorObservables.add(createNumberValidatorObservable(i, field, fieldValueSubject));
                        break;
                    case TEXT:
                        // Validate string
                        validatorObservables.add(createStringValueObservable(i, field, fieldValueSubject));
                        break;
                    default:
                        // Validate object required
                        validatorObservables.add(createObjectValueObservable(i, field, fieldValueSubject));
                        break;
                }
            }
        }

        // Subscribe to field validators
        addSubscription(
                Observable.combineLatest(validatorObservables, (results) -> {
                    for (Object result : results) {
                        if (!((boolean) result)) {
                            return false;
                        }
                    }
                    return true;
                })
                .compose(RxHelper.applySchedulers())
//                .distinctUntilChanged()
                .subscribe((result) -> {
                    mFormValidSubject.onNext(result);
                })
        );

        // Subscribe to value changes
        addSubscription(mScriptPlayer.valueChangeObservable()
                .compose(RxHelper.applySchedulers())
                .subscribe(this::onValueChanged)
        );

    }

    public void destroy() {
        if (mSubscription != null && mSubscription.hasSubscriptions() && !mSubscription.isUnsubscribed()) {
            mSubscription.unsubscribe();
        }
        mSubscription = null;
    }

    public boolean isFieldEnabled(int index) {
        Boolean status = mFieldEnabledStatuses.get(index);
        return (status != null && status);
    }

    private void onValueChanged(KeyValue pair) {
        Log.d(TAG, "onValueChanged() - " + pair.toString());

        final Metadata metadata = mScreen.metadata;
        final ArrayList<Field> fields = metadata.fields;

        for (int i = 0; i < fields.size(); i++) {
            final Field field = fields.get(i);
            boolean currentStatus = isFieldEnabled(i);
            boolean newStatus = evaluateFieldCondition(field);
            if (currentStatus != newStatus) {
                mFieldEnabledStatuses.set(i, newStatus);
                mFieldStatusSubject.onNext(new FieldStatusInfo(i, newStatus));

                // Reset value of disabled field
                final String fieldKey = field.key;
                if (!newStatus) {
                    mScriptPlayer.setValue(fieldKey, null);
                    mScriptPlayer.storeValue(mScreen.sectionTitle, RealmStore.getSessionValue(field, null));
                }
                
                // Validate field when enabled
                notifyValueChanged(new KeyValue(fieldKey, mScriptPlayer.getValue(fieldKey)));
            }
        }
        notifyValueChanged(pair);
    }

    private void addSubscription(Subscription subscription) {
        if (mSubscription == null) {
            mSubscription = new CompositeSubscription();
        }
        mSubscription.add(subscription);
    }

    private void notifyValueChanged(KeyValue pair) {
        if (mFieldValueObservables.containsKey(pair.getKey())) {
            BehaviorSubject<Object> valuePublisher = mFieldValueObservables.get(pair.getKey());
            valuePublisher.onNext(pair.getValue());
        }
    }

    private Observable<Boolean> createNumberValidatorObservable(int index, Field field, BehaviorSubject<Object> fieldValueSubject) {
        final Double maxValue = (field.maxValue != null) ? Double.parseDouble(field.maxValue) : null;
        final Double minValue = (field.minValue != null) ? Double.parseDouble(field.minValue) : null;

        return fieldValueSubject.map((fieldValue) -> {
            boolean enabled = mFieldEnabledStatuses.get(index);
            Double value = (Double) fieldValue;
            return (!enabled || (field.isOptional() && value == null)
                    || (value != null && (minValue == null || value >= minValue) && (maxValue == null || value <= maxValue))
            );
        });
    }

    private Observable<Boolean> createStringValueObservable(int index, Field field, BehaviorSubject<Object> fieldValueSubject) {
        return fieldValueSubject.map((fieldValue) -> {
            boolean enabled = mFieldEnabledStatuses.get(index);
            boolean isEmpty = TextUtils.isEmpty((String) fieldValue);
            return (!enabled || field.isOptional() || !isEmpty);
        });
    }

    private Observable<Boolean> createObjectValueObservable(int index, Field field, BehaviorSubject<Object> fieldValueSubject) {
        boolean enabled = mFieldEnabledStatuses.get(index);
        return fieldValueSubject.map((fieldValue) -> !enabled || field.isOptional() || fieldValue != null);
    }

    private boolean evaluateFieldCondition(Field field) {
        final String condition = field.condition;

        if (TextUtils.isEmpty(condition)) {
            // Short circuit: screen with null condition is always visible
            return true;
        }

        try {
            BooleanExpressionLexer lexer = new BooleanExpressionLexer(new ANTLRInputStream(condition.trim()));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            BooleanExpressionParser parser = new BooleanExpressionParser(tokens);
            BooleanExpressionParser.RootContext tree = parser.root();

            BooleanExpressionEvaluator evaluator = new BooleanExpressionEvaluator(mScriptPlayer);
            ParseTreeWalker.DEFAULT.walk(evaluator, tree);

            return evaluator.getEvaluationResult();
        } catch (Exception e) {
            mScriptPlayer.notifyScriptError(
                    String.format("The field \"%s\" contains an invalid conditional expression. Please check the configuration.", field.label), e);
        }
        return true;
    }

}
