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

package com.microsoft.intellij.rxjava;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import javax.swing.*;

public class IdeaSchedulers implements IdeSchedulers {
    @Nullable final private Project project;

    public IdeaSchedulers(@Nullable Project project) {
        this.project = project;
    }

    public Scheduler processBarVisibleAsync(@NotNull String title) {
        return Schedulers.from(command -> ApplicationManager.getApplication().invokeLater(() -> {
            final Task.Backgroundable task = new Task.Backgroundable(project, title, false) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    command.run();
                }
            };

            final ProgressIndicator progressIndicator = new BackgroundableProcessIndicator(task);

            ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, progressIndicator);
        }));
    }

    public Scheduler processBarVisibleSync( @NotNull String title) {
        return Schedulers.from(command -> ApplicationManager.getApplication().invokeAndWait(() -> {
            final Task.Backgroundable task = new Task.Backgroundable(project, title, false) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    command.run();
                }
            };

            final ProgressIndicator progressIndicator = new BackgroundableProcessIndicator(task);

            ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, progressIndicator);
        }));
    }

    public Scheduler dispatchUIThread() {
        return Schedulers.from(command -> {
            try {
                ApplicationManager.getApplication().invokeAndWait(command, ModalityState.any());
            } catch (ProcessCanceledException ignored) {
                // FIXME!!! Not support process canceling currently, just ignore it
            }
        });
    }
}
