/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.streaminglog;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import com.microsoft.applicationinsights.internal.util.ThreadPoolUtils;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceUtils;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperationTitle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.intellij.helpers.ConsoleViewStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class SpringCloudStreamingLogConsoleView extends ConsoleViewImpl {

    private ConsoleViewStatus status;
    private ExecutorService executorService;

    private String resourceId;
    private InputStream logInputStream;

    public SpringCloudStreamingLogConsoleView(@NotNull Project project, String resourceId) {
        super(project, true);
        this.status = ConsoleViewStatus.STOPPED;
        this.resourceId = resourceId;
    }

    public ConsoleViewStatus getStatus() {
        return status;
    }

    private void setStatus(ConsoleViewStatus status) {
        this.status = status;
    }

    public void startLog(Supplier<InputStream> inputStreamSupplier) throws IOException {
        synchronized (this) {
            if (getStatus() != ConsoleViewStatus.STOPPED) {
                return;
            }
            setStatus(ConsoleViewStatus.STARTING);
        }
        logInputStream = inputStreamSupplier.get();
        if (logInputStream == null) {
            shutdown();
            throw new IOException("Failed to get log streaming content");
        }
        synchronized (this) {
            if (getStatus() != ConsoleViewStatus.STARTING) {
                return;
            }
            setStatus(ConsoleViewStatus.ACTIVE);
        }
        this.print("Streaming Log Start.\n", ConsoleViewContentType.SYSTEM_OUTPUT);
        executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> {
            try (final Scanner scanner = new Scanner(new InputStreamReader(logInputStream))) {
                while (getStatus() == ConsoleViewStatus.ACTIVE && scanner.hasNext()) {
                    final String log = scanner.nextLine();
                    SpringCloudStreamingLogConsoleView.this.print(log + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                // swallow interrupt exception while shutdown
                if (!(e instanceof InterruptedException)) {
                    this.print(String.format("Streaming Log is interrupted due to error : %s.\n", e.getMessage()),
                               ConsoleViewContentType.SYSTEM_OUTPUT);
                }
            } finally {
                print("Streaming Log stops.\n", ConsoleViewContentType.SYSTEM_OUTPUT);
                setStatus(ConsoleViewStatus.STOPPED);
            }
        });
    }

    public void shutdown() {
        synchronized (this) {
            if (getStatus() != ConsoleViewStatus.ACTIVE && getStatus() != ConsoleViewStatus.STARTING) {
                return;
            }
            setStatus(ConsoleViewStatus.STOPPING);
        }
        final IAzureOperationTitle title = AzureOperationBundle.title("springcloud|log_stream.close", ResourceUtils.nameFromResourceId(resourceId));
        AzureTaskManager.getInstance().runInBackground(new AzureTask(getProject(), title, false, () -> {
            try {
                if (logInputStream != null) {
                    try {
                        logInputStream.close();
                    } catch (IOException e) {
                        // swallow io exception when close
                    }
                }
                if (executorService != null) {
                    ThreadPoolUtils.stop(executorService, 100, TimeUnit.MICROSECONDS);
                }
            } finally {
                setStatus(ConsoleViewStatus.STOPPED);
            }
        }));
    }

    @Override
    public void dispose() {
        super.dispose();
        shutdown();
        SpringCloudStreamingLogManager.getInstance().removeConsoleView(resourceId);
    }
}
