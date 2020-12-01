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

import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationsContext;

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

    public final void read(Runnable task) {
        this.read(new AzureTask(task));
    }

    public final void write(Runnable task) {
        this.write(new AzureTask(task));
    }

    public final void runLater(Runnable task) {
        this.runLater(new AzureTask(task));
    }

    public final void runAndWait(Runnable task) {
        this.runAndWait(new AzureTask(task));
    }

    public final void runLater(Runnable task, AzureTask.Modality modality) {
        this.runLater(new AzureTask(task, modality));
    }

    public final void runAndWait(Runnable task, AzureTask.Modality modality) {
        this.runAndWait(new AzureTask(task, modality));
    }

    public final void read(AzureTask task) {
        final Runnable withClosure = AzureOperationsContext.deriveClosure(task.getRunnable());
        task.setRunnable(withClosure);
        this.doRead(task);
    }

    public final void write(AzureTask task) {
        final Runnable withClosure = AzureOperationsContext.deriveClosure(task.getRunnable());
        task.setRunnable(withClosure);
        this.doWrite(task);
    }

    public final void runLater(AzureTask task) {
        final Runnable withClosure = AzureOperationsContext.deriveClosure(task.getRunnable());
        task.setRunnable(withClosure);
        this.doRunLater(task);
    }

    public final void runAndWait(AzureTask task) {
        final Runnable withClosure = AzureOperationsContext.deriveClosure(task.getRunnable());
        task.setRunnable(withClosure);
        this.doRunAndWait(task);
    }

    public final void runInBackground(AzureTask task) {
        final Runnable withClosure = AzureOperationsContext.deriveClosure(task.getRunnable());
        task.setRunnable(withClosure);
        this.doRunInBackground(task);
    }

    public final void runInModal(AzureTask task) {
        final Runnable withClosure = AzureOperationsContext.deriveClosure(task.getRunnable());
        task.setRunnable(withClosure);
        this.doRunInModal(task);
    }

    protected abstract void doRead(AzureTask task);

    protected abstract void doWrite(AzureTask task);

    protected abstract void doRunLater(AzureTask task);

    protected abstract void doRunAndWait(AzureTask task);

    protected abstract void doRunInBackground(AzureTask task);

    protected abstract void doRunInModal(AzureTask task);
}
