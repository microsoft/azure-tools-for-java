/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common;

import rx.Observable;
import rx.subjects.BehaviorSubject;

import com.microsoft.azuretools.azurecommons.helpers.Nullable;

public class LaterInit<T> {
    public static class InitializedException extends RuntimeException {
        public InitializedException(final String message) {
            super(message);
        }
    }

    public static class NotInitializedException extends RuntimeException {
        public NotInitializedException(final String message) {
            super(message);
        }
    }

    private final BehaviorSubject<T> delegation = BehaviorSubject.create();

    public Observable<T> observable() {
        return delegation.filter(obj -> obj != null).first();
    }

    public synchronized void set(final T value) {
        if (isInitialized()) {
            throw new InitializedException(this.toString() + " delegation has already been initialized.");
        }

        delegation.onNext(value);
    }

    public synchronized void setIfNull(final T value) {
        try {
            set(value);
        } catch (InitializedException ignored) {
        }
    }

    public @Nullable
    T getWithNull() {
        return delegation.getValue();
    }

    public T get() {
        if (!isInitialized()) {
            throw new NotInitializedException(this.toString() + " delegation has not been initialized.");
        }

        return delegation.getValue();
    }

    public boolean isInitialized() {
        return delegation.hasValue();
    }
}
