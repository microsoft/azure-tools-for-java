/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.rxjava;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import rx.Scheduler;
import rx.schedulers.Schedulers;

public class EclipseSchedulers implements IdeSchedulers {
    @NotNull
    private String pluginId = "unknown";

    /**
     * @param pluginId
     *            plug in ID for task execution status
     */
    public EclipseSchedulers(@NotNull String pluginId) {
        this.pluginId = pluginId;
    }

    public EclipseSchedulers() {
    }

    public Scheduler processBarVisibleAsync(@NotNull String title) {
        return Schedulers.from(command -> {
            Job job = Job.create(title, monitor -> {
                try {
                    command.run();
                } catch (Exception ex) {
                    return new Status(IStatus.ERROR, pluginId, ex.getMessage(), ex);
                }

                return Status.OK_STATUS;
            });

            job.schedule();
        });
    }

    public Scheduler processBarVisibleSync(@NotNull String title) {
        return Schedulers.from(command -> {
            Job job = Job.create(title, monitor -> {
                try {
                    command.run();
                } catch (Exception ex) {
                    return new Status(IStatus.ERROR, pluginId, ex.getMessage(), ex);
                }

                return Status.OK_STATUS;
            });

            job.schedule();

            // Waiting for job finished
            try {
                job.join();
            } catch (InterruptedException ignore) {
            }
        });
    }

    public Scheduler dispatchUIThread() {
        return Schedulers.from(command -> Display.getDefault().asyncExec(command));
    }

    public Scheduler dispatchPooledThread() {
        return Schedulers.io();
    }
}
