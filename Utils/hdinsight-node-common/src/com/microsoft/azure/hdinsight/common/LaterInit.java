/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
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
