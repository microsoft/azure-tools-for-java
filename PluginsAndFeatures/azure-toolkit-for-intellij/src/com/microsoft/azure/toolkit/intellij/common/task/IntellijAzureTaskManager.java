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

package com.microsoft.azure.toolkit.intellij.common.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public class IntellijAzureTaskManager extends AzureTaskManager {

    @Override
    public void runLater(final AzureTask task) {
        ApplicationManager.getApplication().invokeLater(task.getRunnable(), ModalityState.any());
    }

    @Override
    public void runLater(final Runnable runnable) {
        ApplicationManager.getApplication().invokeLater(runnable, ModalityState.any());
    }

    @Override
    public void runAndWait(final Runnable runnable) {
        ApplicationManager.getApplication().invokeAndWait(runnable, ModalityState.any());
    }

    @Override
    public void runInBackground(final AzureTask task) {
        final Task.Backgroundable backgroundTask = new Task.Backgroundable((Project) task.getProject(), task.getTitle(), task.isCancellable()) {
            @Override
            public void run(@NotNull final ProgressIndicator progressIndicator) {
                task.getRunnable().run();
            }
        };
        ApplicationManager.getApplication().invokeLater(() -> ProgressManager.getInstance().run(backgroundTask), ModalityState.any());
    }

    @Override
    public void runInModal(final AzureTask task) {
        final Task.Modal modalTask = new Task.Modal((Project) task.getProject(), task.getTitle(), task.isCancellable()) {
            @Override
            public void run(@NotNull final ProgressIndicator progressIndicator) {
                task.getRunnable().run();
            }
        };
        ApplicationManager.getApplication().invokeLater(() -> ProgressManager.getInstance().run(modalTask), ModalityState.any());
    }
}
