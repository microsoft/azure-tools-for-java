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

    public final void runLater(Runnable task) {
        final Runnable runnable = this.initClosure(task);
        this.doRunLater(runnable);
    }

    public final void runAndWait(Runnable task) {
        final Runnable runnable = this.initClosure(task);
        this.doRunAndWait(runnable);
    }

    public final void runLater(AzureTask task) {
        final Runnable runnable = this.initClosure(task.getRunnable());
        task.setRunnable(runnable);
        this.doRunLater(task);
    }

    public final void runInBackground(AzureTask task) {
        final Runnable runnable = this.initClosure(task.getRunnable());
        task.setRunnable(runnable);
        this.doRunInBackground(task);
    }

    public final void runInModal(AzureTask task) {
        final Runnable runnable = this.initClosure(task.getRunnable());
        task.setRunnable(runnable);
        this.doRunInModal(task);
    }

    protected abstract void doRunLater(AzureTask task);

    protected abstract void doRunLater(final Runnable runnable);

    protected abstract void doRunAndWait(Runnable runnable);

    protected abstract void doRunInBackground(AzureTask task);

    protected abstract void doRunInModal(AzureTask task);

    private Runnable initClosure(final Runnable runnable) {
        return AzureOperationsContext.deriveClosure(() -> {
            try {
                runnable.run();
            } catch (final RuntimeException e) {
                //TODO: @miller handle exception
            }
        });
    }
}
