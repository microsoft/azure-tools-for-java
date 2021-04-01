/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.utils;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.utils.IProgressTaskImpl;
import com.microsoft.azuretools.utils.IWorker;

public class ProgressTaskModal implements IProgressTaskImpl {
    private static ILog LOG = Activator.getDefault().getLog();
    private Shell parentShell;

    public ProgressTaskModal(Shell parentShell) {
        this.parentShell = parentShell;
    }

    @Override
    public void doWork(IWorker worker) {
        IRunnableWithProgress op = new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor) {
                monitor.beginTask(worker.getName(), IProgressMonitor.UNKNOWN);
                worker.work(new UpdateProgressIndicator(monitor));
                monitor.done();
            }
        };
        try {
            ProgressDialog.get(parentShell, worker.getName()).run(true, true, op);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@ProgressDialog@doWork@ProgressTaskModal", ex));
        }
    }
}
