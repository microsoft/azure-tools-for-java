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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.registry.Registry;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public class IntellijAzureTaskManager extends AzureTaskManager {

    @Override
    protected void doRead(Runnable runnable, final AzureTask<?> task) {
        ApplicationManager.getApplication().runReadAction(runnable);
    }

    @Override
    protected void doWrite(final Runnable runnable, final AzureTask<?> task) {
        ApplicationManager.getApplication().runWriteAction(runnable);
    }

    @Override
    protected void doRunLater(final Runnable runnable, final AzureTask<?> task) {
        final ModalityState state = toIntellijModality(task);
        ApplicationManager.getApplication().invokeLater(runnable, state);
    }

    @Override
    protected void doRunAndWait(final Runnable runnable, final AzureTask<?> task) {
        final ModalityState state = toIntellijModality(task);
        ApplicationManager.getApplication().invokeAndWait(runnable, state);
    }

    @Override
    protected void doRunInBackground(final Runnable runnable, final AzureTask<?> task) {
        final Task.Backgroundable backgroundTask = new Task.Backgroundable((Project) task.getProject(), task.getTitle(), task.isCancellable()) {
            @Override
            public void run(@NotNull final ProgressIndicator progressIndicator) {
                runnable.run();
            }
        };
        task.setRunningBackground(true);
        ApplicationManager.getApplication().invokeLater(() -> ProgressManager.getInstance().run(backgroundTask), ModalityState.any());
    }

    @Override
    protected void doRunInModal(final Runnable runnable, final AzureTask<?> task) {
        if (task.isBackgroundable()) {
            this.doRunInBackgroundableModal(runnable, task);
        } else {
            this.doRunInUnBackgroundableModal(runnable, task);
        }
    }

    protected void doRunInUnBackgroundableModal(final Runnable runnable, final AzureTask<?> task) {
        final Task.Modal modalTask = new Task.Modal((Project) task.getProject(), task.getTitle(), task.isCancellable()) {
            @Override
            public void run(@NotNull final ProgressIndicator progressIndicator) {
                runnable.run();
            }
        };
        task.setRunningBackground(false);
        ProgressManager.getInstance().run(modalTask);
    }

    protected void doRunInBackgroundableModal(final Runnable runnable, final AzureTask<?> task) {
        final PerformInBackgroundOption foreground = PerformInBackgroundOption.DEAF;
        // refer https://jetbrains.org/intellij/sdk/docs/basics/disposers.html
        final Disposable disposable = Disposer.newDisposable();
        // refer https://github.com/JetBrains/intellij-community/commit/077c5558993b97cfb6f68ccc3cbe13065ba3cba8
        Registry.get("ide.background.tasks").setValue(false, disposable);
        final Task.Backgroundable modalTask = new Task.Backgroundable((Project) task.getProject(), task.getTitle(), task.isCancellable(), foreground) {
            @Override
            public void run(@NotNull final ProgressIndicator progressIndicator) {
                runnable.run();
                Disposer.dispose(disposable);
            }

            @Override
            public void processSentToBackground() {
                task.setRunningBackground(true);
            }
        };
        task.setRunningBackground(false);
        ProgressManager.getInstance().run(modalTask);
    }

    private ModalityState toIntellijModality(final AzureTask<?> task) {
        final AzureTask.Modality modality = task.getModality();
        switch (modality) {
            case NONE:
                return ModalityState.NON_MODAL;
            case DEFAULT:
                return ModalityState.defaultModalityState();
            default:
                return ModalityState.any();
        }
    }
}
