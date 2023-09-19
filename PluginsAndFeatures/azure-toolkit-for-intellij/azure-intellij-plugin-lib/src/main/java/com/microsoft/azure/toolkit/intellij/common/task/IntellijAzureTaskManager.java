/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.task;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManagerProvider;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Objects;

@Service
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
        final String title = String.format("Azure: %s...", Objects.requireNonNull(task.getDescription()).toString());
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
        final Task.Modal modalTask = new Task.Modal((Project) task.getProject(), Objects.requireNonNull(task.getDescription()).toString(), task.isCancellable()) {
            @Override
            public void run(@Nonnull final ProgressIndicator progressIndicator) {
                task.setMonitor(new IntellijTaskMonitor(progressIndicator));
                task.setBackgrounded(false);
                runnable.run();
            }
        };
        ProgressManager.getInstance().run(modalTask);
    }

    protected void doRunInBackgroundableModal(final Runnable runnable, final AzureTask<?> task) {
        final PerformInBackgroundOption foreground = PerformInBackgroundOption.DEAF;
        // refer https://jetbrains.org/intellij/sdk/docs/basics/disposers.html
        // refer https://github.com/JetBrains/intellij-community/commit/d7ac4e133fec7e4c1e63f4c1d7dda65e25258b81 ide.background.tasks has been removed
        final String title = StringUtils.capitalize(Objects.requireNonNull(task.getDescription()).toString());
        final Task.Backgroundable modalTask = new Task.ConditionalModal((Project) task.getProject(), title, task.isCancellable(), foreground) {
            @Override
            public void run(@Nonnull final ProgressIndicator progressIndicator) {
                task.setMonitor(new IntellijTaskMonitor(progressIndicator));
                task.setBackgrounded(false);
                runnable.run();
            }
        };
        if (ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().executeOnPooledThread(
                () -> ProgressManager.getInstance().run(modalTask));
        } else {
            ProgressManager.getInstance().run(modalTask);
        }
    }

    private ModalityState toIntellijModality(final AzureTask<?> task) {
        final AzureTask.Modality modality = task.getModality();
        return switch (modality) {
            case NONE -> ModalityState.NON_MODAL;
            case DEFAULT -> ModalityState.defaultModalityState();
            default -> ModalityState.any();
        };
    }

    @Override
    public boolean isUIThread() {
        return ApplicationManager.getApplication().isDispatchThread() || SwingUtilities.isEventDispatchThread();
    }

    @RequiredArgsConstructor
    private static class IntellijTaskMonitor implements AzureTask.Monitor {
        private final ProgressIndicator indicator;

        @Override
        public void cancel() {
            this.indicator.cancel();
        }

        @Override
        public boolean isCancelled() {
            return indicator.isCanceled();
        }
    }

    public static class Provider implements AzureTaskManagerProvider {
        @Override
        public AzureTaskManager getTaskManager() {
            return ApplicationManager.getApplication().getService(AzureTaskManager.class);
        }
    }
}
