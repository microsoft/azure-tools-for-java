/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azure.toolkit.lib.common.handler.AzureExceptionHandler;
import rx.Completable;
import rx.Observable;
import rx.Single;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.*;

public class AzureOperationsContext {
    static final ThreadLocal<Deque<AzureOperationRef>> operations = ThreadLocal.withInitial(ArrayDeque::new);
    static final UncaughtExceptionHandler rxExceptionHandler = (t, e) -> AzureExceptionHandler.onRxException(e);
    static final UncaughtExceptionHandler exceptionHandler = (t, e) -> AzureExceptionHandler.onUncaughtException(e);

    public static List<AzureOperationRef> getOperations() {
        final ArrayList<AzureOperationRef> ops = new ArrayList<>(operations.get());
        Collections.reverse(ops);
        return Collections.unmodifiableList(ops);
    }

    static void push(final AzureOperationRef operation) {
        operations.get().push(operation);
    }

    static void pop() {
        operations.get().pop();
    }

    public static Runnable deriveClosure(final Runnable runnable) {
        final Deque<AzureOperationRef> closure = new ArrayDeque<>(AzureOperationsContext.getOperations());
        return () -> {
            operations.set(closure);
            try {
                runnable.run();
            } catch (final Throwable throwable) {
                AzureExceptionHandler.onUncaughtException(throwable);
            }
        };
    }

    public static Single.OnSubscribe<?> deriveClosure(final Single.OnSubscribe<?> action) {
        final Deque<AzureOperationRef> closure = new ArrayDeque<>(AzureOperationsContext.getOperations());
        return (o) -> {
            Thread.currentThread().setUncaughtExceptionHandler(rxExceptionHandler);
            operations.set(closure);
            try {
                action.call(o);
            } catch (final Throwable throwable) {
                AzureExceptionHandler.onRxException(throwable);
            }
        };
    }

    public static Observable.OnSubscribe<?> deriveClosure(final Observable.OnSubscribe<?> action) {
        final Deque<AzureOperationRef> closure = new ArrayDeque<>(AzureOperationsContext.getOperations());
        return (o) -> {
            Thread.currentThread().setUncaughtExceptionHandler(rxExceptionHandler);
            operations.set(closure);
            try {
                action.call(o);
            } catch (final Throwable throwable) {
                AzureExceptionHandler.onRxException(throwable);
            }
        };
    }

    public static Completable.OnSubscribe deriveClosure(final Completable.OnSubscribe action) {
        final Deque<AzureOperationRef> closure = new ArrayDeque<>(AzureOperationsContext.getOperations());
        return (o) -> {
            Thread.currentThread().setUncaughtExceptionHandler(rxExceptionHandler);
            operations.set(closure);
            try {
                action.call(o);
            } catch (final Throwable throwable) {
                AzureExceptionHandler.onRxException(throwable);
            }
        };
    }

    @FunctionalInterface
    public interface OperationProceedable {
        Object proceed() throws Throwable;
    }
}
