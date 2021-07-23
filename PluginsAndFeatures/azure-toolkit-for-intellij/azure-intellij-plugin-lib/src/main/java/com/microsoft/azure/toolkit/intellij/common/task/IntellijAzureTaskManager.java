/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

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

    protected void doRunOnPooledThread(final Runnable runnable, final AzureTask<?> task) {
        ApplicationManager.getApplication().executeOnPooledThread(runnable);
    }

    @Override
    protected void doRunAndWait(final Runnable runnable, final AzureTask<?> task) {
        final ModalityState state = toIntellijModality(task);
        ApplicationManager.getApplication().invokeAndWait(runnable, state);
    }

    @Override
    protected void doRunInBackground(final Runnable runnable, final AzureTask<?> task) {
        final String title = String.format("Azure: %s...", task.getTitle().toString());
        final Task.Backgroundable backgroundTask = new Task.Backgroundable((Project) task.getProject(), title, task.isCancellable()) {
            @Override
            public void run(@Nonnull final ProgressIndicator progressIndicator) {
                task.setBackgrounded(true);
                runnable.run();
            }
        };
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
        final Task.Modal modalTask = new Task.Modal((Project) task.getProject(), task.getTitle().toString(), task.isCancellable()) {
            @Override
            public void run(@Nonnull final ProgressIndicator progressIndicator) {
                task.setBackgrounded(false);
                runnable.run();
            }
        };
        ProgressManager.getInstance().run(modalTask);
    }

    protected void doRunInBackgroundableModal(final Runnable runnable, final AzureTask<?> task) {
        final PerformInBackgroundOption foreground = PerformInBackgroundOption.DEAF;
        // refer https://jetbrains.org/intellij/sdk/docs/basics/disposers.html
        final Disposable disposable = Disposer.newDisposable();
        // refer https://github.com/JetBrains/intellij-community/commit/077c5558993b97cfb6f68ccc3cbe13065ba3cba8
        Registry.get("ide.background.tasks").setValue(false, disposable);
        final String title = StringUtils.capitalize(task.getTitle().toString());
        final Task.Backgroundable modalTask = new Task.Backgroundable((Project) task.getProject(), title, task.isCancellable(), foreground) {
            @Override
            public void run(@Nonnull final ProgressIndicator progressIndicator) {
                task.setBackgrounded(false);
                runnable.run();
                Disposer.dispose(disposable);
            }

            @Override
            public void processSentToBackground() {
                task.setBackgrounded(true);
            }
        };
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
