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

package com.microsoft.azure.toolkit.lib.common.task;

import rx.Completable;
import rx.Observable;
import rx.Single;
import rx.functions.Func1;
import rx.plugins.RxJavaHooks;

import java.util.Objects;

public class AzureRxTaskManager {
    private static boolean registered = false;

    @SuppressWarnings("rawtypes")
    public static synchronized void register() {
        if (registered) {
            throw new IllegalStateException("rx task manager has already been registered.");
        }
        registered = true;
        final Func1<Observable.OnSubscribe, Observable.OnSubscribe> oldObservableCreateHooks = RxJavaHooks.getOnObservableCreate();
        final Func1<Single.OnSubscribe, Single.OnSubscribe> oldSingleCreateHooks = RxJavaHooks.getOnSingleCreate();
        final Func1<Completable.OnSubscribe, Completable.OnSubscribe> oldCompletableCreateHooks = RxJavaHooks.getOnCompletableCreate();
        RxJavaHooks.setOnObservableCreate(onSubscribe -> {
            final AzureTaskContext.Node context = AzureTaskContext.current().derive();
            final Observable.OnSubscribe<?> withClosure = (o) -> AzureTaskContext.run(() -> onSubscribe.call(o), context);
            if (Objects.isNull(oldObservableCreateHooks)) {
                return withClosure;
            }
            return oldObservableCreateHooks.call(withClosure);
        });
        RxJavaHooks.setOnCompletableCreate(onSubscribe -> {
            final AzureTaskContext.Node context = AzureTaskContext.current().derive();
            final Completable.OnSubscribe withClosure = (o) -> AzureTaskContext.run(() -> onSubscribe.call(o), context);
            if (Objects.isNull(oldCompletableCreateHooks)) {
                return withClosure;
            }
            return oldCompletableCreateHooks.call(withClosure);
        });
        RxJavaHooks.setOnSingleCreate(onSubscribe -> {
            final AzureTaskContext.Node context = AzureTaskContext.current().derive();
            final Single.OnSubscribe<?> withClosure = (o) -> AzureTaskContext.run(() -> onSubscribe.call(o), context);
            if (Objects.isNull(oldSingleCreateHooks)) {
                return withClosure;
            }
            return oldSingleCreateHooks.call(withClosure);
        });
    }
}
