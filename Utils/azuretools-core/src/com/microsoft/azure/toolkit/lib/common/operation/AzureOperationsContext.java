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
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.functions.Action1;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.*;

public class AzureOperationsContext {
    private static final ThreadLocal<Deque<AzureOperationRef>> operations = ThreadLocal.withInitial(ArrayDeque::new);
    private static final UncaughtExceptionHandler rxExceptionHandler = (t, e) -> AzureExceptionHandler.onRxException(e);
    private static final UncaughtExceptionHandler exceptionHandler = (t, e) -> AzureExceptionHandler.onUncaughtException(e);

    public static List<AzureOperationRef> getOperations() {
        final ArrayList<AzureOperationRef> ops = new ArrayList<>(AzureOperationsContext.operations.get());
        Collections.reverse(ops);
        return Collections.unmodifiableList(ops);
    }

    static void init(Deque<AzureOperationRef> closure) {
        AzureOperationsContext.operations.set(closure);
    }

    static void push(final AzureOperationRef operation) {
        AzureOperationsContext.operations.get().push(operation);
    }

    static AzureOperationRef pop() {
        return AzureOperationsContext.operations.get().pop();
    }

    static void clear() {
        AzureOperationsContext.operations.get().clear();
    }

    static void dispose() {
        AzureOperationsContext.operations.remove();
    }

    public static Runnable deriveClosure(final Runnable runnable) {
        final Deque<AzureOperationRef> closure = new ArrayDeque<>(AzureOperationsContext.getOperations());
        final long parentThread = Thread.currentThread().getId();
        return () -> act((none) -> runnable.run(), closure, parentThread, null);
    }

    public static Single.OnSubscribe<?> deriveClosure(final Single.OnSubscribe<?> action) {
        final Deque<AzureOperationRef> closure = new ArrayDeque<>(AzureOperationsContext.getOperations());
        final long parentThread = Thread.currentThread().getId();
        return (o) -> act(action, closure, parentThread, o);
    }

    public static Observable.OnSubscribe<?> deriveClosure(final Observable.OnSubscribe<?> action) {
        final Deque<AzureOperationRef> closure = new ArrayDeque<>(AzureOperationsContext.getOperations());
        final long parentThread = Thread.currentThread().getId();
        return (o) -> act(action, closure, parentThread, o);
    }

    public static Completable.OnSubscribe deriveClosure(final Completable.OnSubscribe action) {
        final Deque<AzureOperationRef> closure = new ArrayDeque<>(AzureOperationsContext.getOperations());
        final long parentThread = Thread.currentThread().getId();
        return (o) -> act(action, closure, parentThread, o);
    }

    private static <T> void act(final Action1<T> action, final Deque<AzureOperationRef> closure, final long parentThread, @Nullable final T subscriber) {
        final long currentThread = Thread.currentThread().getId();
        if (!Objects.equals(currentThread, parentThread)) {
            Thread.currentThread().setUncaughtExceptionHandler(rxExceptionHandler);
            AzureOperationsContext.init(closure);
        }
        try {
            action.call(subscriber);
        } catch (final Throwable throwable) {
            AzureExceptionHandler.onRxException(throwable);
        } finally {
            if (!Objects.equals(currentThread, parentThread)) {
                AzureOperationsContext.dispose();
            }
        }
    }
}
