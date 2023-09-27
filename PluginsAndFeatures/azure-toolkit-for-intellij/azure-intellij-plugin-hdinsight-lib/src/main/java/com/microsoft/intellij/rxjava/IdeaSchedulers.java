/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.rxjava;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task.Backgroundable;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import javax.swing.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.intellij.openapi.progress.PerformInBackgroundOption.DEAF;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static rx.schedulers.Schedulers.computation;
import static rx.schedulers.Schedulers.from;

@Slf4j
public class IdeaSchedulers implements IdeSchedulers, ILogger {
    @Nullable
    private final Project project;

    public IdeaSchedulers() {
        this(null);
    }

    public IdeaSchedulers(@Nullable final Project project) {
        this.project = project;
    }

    public Scheduler processBarVisibleAsync(@NotNull final String title) {
        return from(command -> AzureTaskManager.getInstance().runLater(() -> {
            final Backgroundable task = new Backgroundable(project, title, false) {
                @Override
                public void run(@NotNull final ProgressIndicator progressIndicator) {
                    command.run();
                }
            };
            final ProgressIndicator progressIndicator = new BackgroundableProcessIndicator(task);
            ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, progressIndicator);
        }, AzureTask.Modality.ANY));
    }

    public Scheduler processBarVisibleSync(@NotNull final String title) {
        return from(command -> AzureTaskManager.getInstance().runAndWait(() -> {
            final Backgroundable task = new Backgroundable(project, title, false) {
                @Override
                public void run(@NotNull final ProgressIndicator progressIndicator) {
                    command.run();
                }
            };

            final ProgressIndicator progressIndicator = new BackgroundableProcessIndicator(task);

            ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, progressIndicator);
        }));
    }

    public Scheduler dispatchUIThread() {
        return dispatchUIThread(ModalityState.any());
    }

    public Scheduler dispatchUIThread(final ModalityState state) {
        final Application application = ApplicationManager.getApplication();

        return from(command -> {
            try {
                if (application == null) {
                    SwingUtilities.invokeLater(command);
                } else {
                    application.invokeLater(command, state);
                }
            } catch (final ProcessCanceledException ignored) {
                // TODO!!! Not support process canceling currently, just ignore it
            }
        });
    }

    @Override
    public Scheduler dispatchPooledThread() {
        final Application application = ApplicationManager.getApplication();

        return from(command -> {
            try {
                if (application == null) {
                    Schedulers.io();
                } else {
                    application.executeOnPooledThread(command);
                }
            } catch (final ProcessCanceledException ignored) {
                // TODO!!! Not support process canceling currently, just ignore it
            }
        });
    }

    private static final ConcurrentMap<Thread, ProgressIndicator> thread2Indicator = new ConcurrentHashMap<>(32);

    public static void updateCurrentBackgroundableTaskIndicator(final Action1<? super ProgressIndicator> action) {
        final Thread currentThread = Thread.currentThread();
        final ProgressIndicator indicator = thread2Indicator.get(currentThread);

        if (indicator == null) {
            log.warn("No ProgressIndicator found for thread " + currentThread.getName());
            return;
        }

        action.call(indicator);
    }

    public Scheduler backgroundableTask(final String title) {
        return from(command -> ProgressManager.getInstance().run(new Backgroundable(project, title, false, DEAF) {
            @Override
            public void run(final @NotNull ProgressIndicator indicator) {
                final Thread workerThread = Thread.currentThread();

                // Check if indicator's cancelled every 0.5s and interrupt the worker thread if it be.
                Observable.interval(500, MILLISECONDS, computation())
                          .takeUntil(i -> indicator.isCanceled())
                          .filter(i -> indicator.isCanceled())
                          .subscribe(data -> workerThread.interrupt(),
                                     err -> log().warn("Can't interrupt thread {}", workerThread.getName(), err));

                thread2Indicator.putIfAbsent(workerThread, indicator);

                try {
                    command.run();
                } finally {
                    thread2Indicator.remove(workerThread);
                }
            }
        }));
    }
}
