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

package org.neotree.support.rx;

import android.support.annotation.IntDef;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

/**
 * Fork of https://gist.github.com/gsoltis/86210e3259dcc6998801
 */
public class RxFirebase {

    public static class FirebaseChildEvent <T> {
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({TYPE_ADD, TYPE_CHANGE, TYPE_MOVE, TYPE_REMOVE})
        public @interface EventType{}

        public static final int TYPE_ADD = 1;
        public static final int TYPE_CHANGE = 2;
        public static final int TYPE_REMOVE = 3;
        public static final int TYPE_MOVE = 4;

        public final String key;
        public final T value;
        public final @EventType int eventType;
        public final String prevName;

        public FirebaseChildEvent(String key, T value, @EventType int eventType, String prevName) {
            this.key = key;
            this.value = value;
            this.eventType = eventType;
            this.prevName = prevName;
        }

        public <V> FirebaseChildEvent<V> withValue(String key, V value){
            return new FirebaseChildEvent<>(key, value, eventType, prevName);
        }

        public boolean isTypeAdd() {
            return (eventType == TYPE_ADD);
        }

        public boolean isTypeChange() {
            return (eventType == TYPE_CHANGE);
        }

        public boolean isTypeRemove() {
            return (eventType == TYPE_REMOVE);
        }

        public boolean isTypeMove() {
            return (eventType == TYPE_MOVE);
        }

    }

    public static class DatabaseException extends IOException {
        private final DatabaseError error;

        public DatabaseException(DatabaseError error) {
            super(error.getMessage() + "\n" + error.getDetails(), error.toException());
            this.error = error;
        }

        public DatabaseError getError() {
            return error;
        }
    }

    public static Observable<FirebaseChildEvent<DataSnapshot>> observeChildren(final Query query) {
        return Observable.create(new Observable.OnSubscribe<FirebaseChildEvent<DataSnapshot>>() {
            @Override
            public void call(final Subscriber<? super FirebaseChildEvent<DataSnapshot>> subscriber) {
                final ChildEventListener childEventListener = query.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot snapshot, String prevName) {
                        subscriber.onNext(new FirebaseChildEvent<>(snapshot.getKey(), snapshot, FirebaseChildEvent.TYPE_ADD, prevName));
                    }

                    @Override
                    public void onChildChanged(DataSnapshot snapshot, String prevName) {
                        subscriber.onNext(new FirebaseChildEvent<>(snapshot.getKey(), snapshot, FirebaseChildEvent.TYPE_CHANGE, prevName));
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot snapshot) {
                        subscriber.onNext(new FirebaseChildEvent<>(snapshot.getKey(), snapshot, FirebaseChildEvent.TYPE_REMOVE, null));
                    }

                    @Override
                    public void onChildMoved(DataSnapshot snapshot, String prevName) {
                        subscriber.onNext(new FirebaseChildEvent<>(snapshot.getKey(), snapshot, FirebaseChildEvent.TYPE_MOVE, prevName));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        subscriber.onError(new DatabaseException(databaseError));
                    }
                });
                subscriber.add(Subscriptions.create(() -> query.removeEventListener(childEventListener)));
            }
        });
    }

    public static <T> Observable<FirebaseChildEvent<T>> observeChildren(final Query query, GenericTypeIndicator<T> type) {
        return Observable.create(new Observable.OnSubscribe<FirebaseChildEvent<T>>() {
            @Override
            public void call(final Subscriber<? super FirebaseChildEvent<T>> subscriber) {
                final ChildEventListener childEventListener = query.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot snapshot, String prevName) {
                        subscriber.onNext(new FirebaseChildEvent<>(snapshot.getKey(), snapshot.getValue(type), FirebaseChildEvent.TYPE_ADD, prevName));
                    }

                    @Override
                    public void onChildChanged(DataSnapshot snapshot, String prevName) {
                        subscriber.onNext(new FirebaseChildEvent<>(snapshot.getKey(), snapshot.getValue(type), FirebaseChildEvent.TYPE_CHANGE, prevName));
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot snapshot) {
                        subscriber.onNext(new FirebaseChildEvent<>(snapshot.getKey(), snapshot.getValue(type), FirebaseChildEvent.TYPE_REMOVE, null));
                    }

                    @Override
                    public void onChildMoved(DataSnapshot snapshot, String prevName) {
                        subscriber.onNext(new FirebaseChildEvent<>(snapshot.getKey(), snapshot.getValue(type), FirebaseChildEvent.TYPE_MOVE, prevName));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        subscriber.onError(new DatabaseException(databaseError));
                    }
                });
                subscriber.add(Subscriptions.create(() -> query.removeEventListener(childEventListener)));
            }
        });
    }

    public static Func1<FirebaseChildEvent, Boolean> makeEventFilter(final @FirebaseChildEvent.EventType int eventType) {
        return firebaseChildEvent -> firebaseChildEvent.eventType == eventType;
    }

    public static Observable<DataSnapshot> observe(final Query query) {
        return Observable.create(new Observable.OnSubscribe<DataSnapshot>() {
            @Override
            public void call(final Subscriber<? super DataSnapshot> subscriber) {
                final ValueEventListener listener = query.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        subscriber.onNext(snapshot);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        subscriber.onError(new DatabaseException(databaseError));
                    }
                });
                subscriber.add(Subscriptions.create(() -> query.removeEventListener(listener)));
            }
        });
    }

    public static <T> Observable<T> observe(final Query query, GenericTypeIndicator<T> type) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(final Subscriber<? super T> subscriber) {
                final ValueEventListener listener = query.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if(snapshot != null) {
                            subscriber.onNext(snapshot.getValue(type));
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        subscriber.onError(new DatabaseException(databaseError));
                    }
                });
                subscriber.add(Subscriptions.create(() -> query.removeEventListener(listener)));
            }
        });
    }

    public static Observable<DataSnapshot> observeOnce(final Query query) {
        return Observable.create(new Observable.OnSubscribe<DataSnapshot>() {
            @Override
            public void call(final Subscriber<? super DataSnapshot> subscriber) {
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        subscriber.onNext(snapshot);
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        subscriber.onError(new DatabaseException(databaseError));
                    }
                });
                subscriber.add(Subscriptions.create(() -> {}));
            }
        });
    }

    public static <T> Observable<T> observeOnce(final Query query, GenericTypeIndicator<T> type) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(final Subscriber<? super T> subscriber) {
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        subscriber.onNext(snapshot.getValue(type));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        subscriber.onError(new DatabaseException(databaseError));
                    }
                });
                subscriber.add(Subscriptions.create(() -> {}));
            }
        });
    }

    public static Observable<Void> setValue(final DatabaseReference reference, final Object value) {
        return toObservable(() -> reference.setValue(value));
    }

    public static Observable<Void> updateChildren(final DatabaseReference reference, final Map<String, ?> children) {
        return toObservable(() -> {
            //noinspection unchecked
            return reference.updateChildren((Map<String, Object>) children);
        });
    }

    public static Observable<FirebaseAuth> observeAuthChange(final FirebaseAuth auth) {
        return Observable
                .create(new Observable.OnSubscribe<FirebaseAuth>() {
                    @Override
                    public void call(final Subscriber<? super FirebaseAuth> subscriber) {
                        final FirebaseAuth.AuthStateListener listener = subscriber::onNext;
                        subscriber.add(Subscriptions.create(() -> auth.removeAuthStateListener(listener)));
                        auth.addAuthStateListener(listener);
                    }
                });
    }

    public static Observable<FirebaseUser> observeAuth(FirebaseAuth auth) {
        return observeAuthChange(auth)
                .map(FirebaseAuth::getCurrentUser)
                .distinctUntilChanged();
    }

    public static Observable<AuthResult> authenticateAnonymously(final FirebaseAuth firebaseAuth){
        return toObservable(firebaseAuth::signInAnonymously);
    }

    public static Observable<AuthResult> authenticateWithCredential(final FirebaseAuth firebaseAuth, final AuthCredential credential) {
        return toObservable(() -> firebaseAuth.signInWithCredential(credential));
    }

    public static Observable<AuthResult> authenticateWithEmailAndPassword(final FirebaseAuth firebaseAuth, String email, String password){
        return toObservable(() -> firebaseAuth.signInWithEmailAndPassword(email, password));
    }

    /**
     * Returns an Observable that signs out on subscription, emits null and completes.
     */
    public static Observable<Void> signOut(final FirebaseAuth auth) {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                auth.signOut();
                observeAuth(auth)
                        .filter(firebaseUser -> firebaseUser == null)
                        .take(1)
                        .cast(Void.class)
                        .subscribe(subscriber);
            }
        });
    }

    private static <T> Observable<T> toObservable(Func0<? extends Task<T>> factory) {
        return Observable.create(new TaskOnSubscribe<>(factory));
    }

    private static class TaskOnSubscribe<T> implements Observable.OnSubscribe<T> {
        private final Func0<? extends Task<T>> taskFactory;

        private TaskOnSubscribe(Func0<? extends Task<T>> taskFactory) {
            this.taskFactory = taskFactory;
        }

        @Override
        public void call(final Subscriber<? super T> subscriber) {
            taskFactory.call().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    subscriber.onNext(task.getResult());
                    subscriber.onCompleted();
                } else {
                    subscriber.onError(task.getException());
                }
            });
        }
    }

}