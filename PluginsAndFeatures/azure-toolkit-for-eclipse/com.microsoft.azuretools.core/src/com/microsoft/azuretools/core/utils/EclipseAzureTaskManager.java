/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.utils;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.SneakyThrows;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EclipseAzureTaskManager extends AzureTaskManager {

	private static final ExecutorService executor = Executors.newFixedThreadPool(4);

	@Override
	protected void doRead(Runnable runnable, final AzureTask<?> task) {
		throw new UnsupportedOperationException("not support");
	}

	@Override
	protected void doWrite(final Runnable runnable, final AzureTask<?> task) {
		throw new UnsupportedOperationException("not support");
	}

	@Override
	protected void doRunOnPooledThread(Runnable runnable, AzureTask<?> task) {
		executor.submit(runnable);
	}

	@Override
	protected void doRunLater(Runnable runnable, AzureTask<?> task) {
		Display.getDefault().asyncExec(runnable);
	}

	@Override
	protected void doRunAndWait(Runnable runnable, AzureTask<?> task) {
		Display.getDefault().syncExec(runnable);
	}

	@Override
	protected void doRunInBackground(Runnable runnable, AzureTask<?> task) {
		final String title = String.format("Azure: %s...", Objects.requireNonNull(task.getTitle()));
		Job.create(title, (monitor) -> {
			monitor.beginTask(title, IProgressMonitor.UNKNOWN);
			try {
				task.setBackgrounded(true);
				runnable.run();
			} finally {
				monitor.done();
			}
			return Status.OK_STATUS;
		}).schedule();
	}

	@Override
	protected void doRunInModal(final Runnable runnable, final AzureTask<?> task) {
		if (task.isBackgroundable()) {
			this.doRunInBackground(runnable, task);
		} else {
			this.doRunInUnBackgroundableModal(runnable, task);
		}
	}

	protected void doRunInUnBackgroundableModal(final Runnable runnable, final AzureTask<?> task) {
		final String title = String.format("Azure: %s...", Objects.requireNonNull(task.getTitle()));
		try {
			new ProgressMonitorDialog(new Shell()).run(true, task.isCancellable(), monitor -> {
				monitor.beginTask(title, IProgressMonitor.UNKNOWN);
				try {
					task.setBackgrounded(false);
					runnable.run();
				} finally {
					monitor.done();
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			String msg = String.format("failed to execute task (%s)", task.getTitle());
			throw new AzureToolkitRuntimeException(msg, e);
		}
	}
}
