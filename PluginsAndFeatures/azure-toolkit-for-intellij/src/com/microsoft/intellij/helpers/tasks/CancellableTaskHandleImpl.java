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
package com.microsoft.intellij.helpers.tasks;

import com.intellij.openapi.progress.ProgressIndicator;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.tasks.CancellableTask;

public class CancellableTaskHandleImpl implements CancellableTask.CancellableTaskHandle {
    private ProgressIndicator progressIndicator;
    private Throwable exception;

    @Override
    public boolean isFinished() {
        return !progressIndicator.isRunning();
    }

    @Override
    public boolean isCancelled() {
        return progressIndicator.isCanceled();
    }

    @Override
    public boolean isSuccessful() {
        return isFinished() && !isCancelled() && exception == null;
    }

    @Nullable
    @Override
    public Throwable getException() {
        return exception;
    }

    public void setException(@NotNull Throwable exception) {
        this.exception = exception;
    }

    @Override
    public void cancel() {
        progressIndicator.cancel();
    }

    public void setProgressIndicator(@NotNull ProgressIndicator progressIndicator) {
        this.progressIndicator = progressIndicator;
    }
}