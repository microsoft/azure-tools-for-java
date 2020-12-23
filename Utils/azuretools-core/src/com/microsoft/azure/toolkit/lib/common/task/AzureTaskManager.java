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

import lombok.extern.java.Log;
import rx.Emitter;
import rx.Observable;
import rx.observables.ConnectableObservable;

import java.util.function.BiConsumer;

@Log
public abstract class AzureTaskManager {

    private static AzureTaskManager instance;

    public static synchronized void register(AzureTaskManager manager) {
        if (AzureTaskManager.instance == null) {
            AzureTaskManager.instance = manager;
        }
    }

    public static AzureTaskManager getInstance() {
        return AzureTaskManager.instance;
    }

    public final Observable<Void> read(Runnable task) {
        return this.read(new AzureTask<>(task));
    }

    public final Observable<Void> write(Runnable task) {
        return this.write(new AzureTask<>(task));
    }

    public final Observable<Void> runLater(Runnable task) {
        return this.runLater(new AzureTask<>(task));
    }

    public final Observable<Void> runAndWait(Runnable task) {
        return this.runAndWait(new AzureTask<>(task));
    }

    public final Observable<Void> runLater(Runnable task, AzureTask.Modality modality) {
        return this.runLater(new AzureTask<>(task, modality));
    }

    public final Observable<Void> runAndWait(Runnable task, AzureTask.Modality modality) {
        return this.runAndWait(new AzureTask<>(task, modality));
    }

    public final Observable<Void> read(String title, Runnable task) {
        return this.read(new AzureTask<>(title, task));
    }

    public final Observable<Void> write(String title, Runnable task) {
        return this.write(new AzureTask<>(title, task));
    }

    public final Observable<Void> runLater(String title, Runnable task) {
        return this.runLater(new AzureTask<>(title, task));
    }

    public final Observable<Void> runAndWait(String title, Runnable task) {
        return this.runAndWait(new AzureTask<>(title, task));
    }

    public final Observable<Void> runLater(String title, Runnable task, AzureTask.Modality modality) {
        return this.runLater(new AzureTask<>(title, task, modality));
    }

    public final Observable<Void> runAndWait(String title, Runnable task, AzureTask.Modality modality) {
        return this.runAndWait(new AzureTask<>(title, task, modality));
    }

    public final <T> Observable<T> read(AzureTask<T> task) {
        return this.runInObservable(this::doRead, task);
    }

    public final <T> Observable<T> write(AzureTask<T> task) {
        return this.runInObservable(this::doWrite, task);
    }

    public final <T> Observable<T> runLater(AzureTask<T> task) {
        return this.runInObservable(this::doRunLater, task);
    }

    public final <T> Observable<T> runAndWait(AzureTask<T> task) {
        return this.runInObservable(this::doRunAndWait, task);
    }

    public final <T> Observable<T> runInBackground(AzureTask<T> task) {
        return this.runInObservable(this::doRunInBackground, task);
    }

    public final <T> Observable<T> runInModal(AzureTask<T> task) {
        return this.runInObservable(this::doRunInModal, task);
    }

    private <T> ConnectableObservable<T> runInObservable(final BiConsumer<? super Runnable, ? super AzureTask<T>> consumer, final AzureTask<T> task) {
        final ConnectableObservable<T> observable = Observable.create((Emitter<T> emitter) -> {
            final AzureTaskContext.Node context = AzureTaskContext.current().derive();
            task.setContext(context); // set for temp usage.
            final Runnable t = () -> AzureTaskContext.run(() -> {
                try {
                    // log.info(String.format("doing task[%s] in thread[%s]/context[%s]", task.getTitle(), Thread.currentThread().getId(), context));
                    emitter.onNext(task.getSupplier().get());
                    emitter.onCompleted();
                } catch (final Throwable e) {
                    emitter.onError(e);
                }
            }, context);
            consumer.accept(t, task);
        }, Emitter.BackpressureMode.BUFFER).publish();
        observable.connect();
        return observable;
    }

    protected abstract void doRead(Runnable runnable, AzureTask<?> task);

    protected abstract void doWrite(Runnable runnable, AzureTask<?> task);

    protected abstract void doRunLater(Runnable runnable, AzureTask<?> task);

    protected abstract void doRunAndWait(Runnable runnable, AzureTask<?> task);

    protected abstract void doRunInBackground(Runnable runnable, AzureTask<?> task);

    protected abstract void doRunInModal(Runnable runnable, AzureTask<?> task);
}
