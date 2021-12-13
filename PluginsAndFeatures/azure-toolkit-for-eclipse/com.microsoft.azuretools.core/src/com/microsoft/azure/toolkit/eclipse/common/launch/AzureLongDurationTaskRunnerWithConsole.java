/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.common.launch;

import com.microsoft.azure.toolkit.eclipse.common.console.AzureAsyncConsoleJob;
import com.microsoft.azure.toolkit.eclipse.common.console.EclipseConsoleMessager;
import com.microsoft.azure.toolkit.eclipse.common.console.JobConsole;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;

public class AzureLongDurationTaskRunnerWithConsole {
    static class AzureLongDurationTaskRunnerWithConsoleHolder {
        static final AzureLongDurationTaskRunnerWithConsole instance = new AzureLongDurationTaskRunnerWithConsole();
    }
    public static AzureLongDurationTaskRunnerWithConsole getInstance() {
        return AzureLongDurationTaskRunnerWithConsoleHolder.instance;
    }

    public void runTask(String title, Runnable runnable, boolean removeConsoleAfterSucceed) {
        final AzureAsyncConsoleJob job = new AzureAsyncConsoleJob(title);
        JobConsole myConsole = new JobConsole(title, job);
        EclipseConsoleMessager messager = new EclipseConsoleMessager(myConsole);

        myConsole.activate();
        ConsolePlugin.getDefault().getConsoleManager().addConsoles(new JobConsole[]{myConsole});
        ConsolePlugin.getDefault().getConsoleManager().showConsoleView(myConsole);

        job.setMessager(messager);
        job.setSupplier(() -> {
            final AzureTask<Void> task = new AzureTask<Void>(title, runnable);
            task.setType(AzureOperation.Type.ACTION.name());
            try {
                AzureTaskManager.getInstance().runImmediatelyAsObservable(() -> {
                    AzureMessager.getContext().setMessager(messager);
                    try {
                        task.getSupplier().get();
                        messager.info("Done.");
                        if (removeConsoleAfterSucceed) {
                            ConsolePlugin.getDefault().getConsoleManager().removeConsoles(new IConsole[] { myConsole });
                        }
                    } catch (Throwable ex) {
                        if (ExceptionUtils.getRootCause(ex) instanceof InterruptedException) {
                            messager.error("Cancelled.");
                        }
                    }
                }).subscribe();
                return Status.OK_STATUS;
            } catch (Exception ex) {
                if (ExceptionUtils.hasCause(ex, InterruptedException.class)) {
                    messager.info("User cancelled the task.");
                    return Status.CANCEL_STATUS;
                }
                messager.error(ExceptionUtils.getStackTrace(ex));
                return Status.error(ex.getMessage());
            }
        });
        job.schedule();
    }
}
