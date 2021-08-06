/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.appservice;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import static com.intellij.execution.ui.ConsoleViewContentType.NORMAL_OUTPUT;
import static com.intellij.execution.ui.ConsoleViewContentType.SYSTEM_OUTPUT;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class AppServiceStreamingLogConsoleView extends ConsoleViewImpl {

    private static final String SEPARATOR = System.getProperty("line.separator");

    private boolean isDisposed;
    private String resourceId;
    private Disposable subscription;

    public AppServiceStreamingLogConsoleView(@NotNull Project project, String resourceId) {
        super(project, true);
        this.isDisposed = false;
        this.resourceId = resourceId;
    }

    public void startStreamingLog(Flux<String> logStreaming) {
        if (!isActive()) {
            printlnToConsole(message("appService.logStreaming.hint.connect"), SYSTEM_OUTPUT);
            subscription = logStreaming.subscribeOn(Schedulers.boundedElastic())
                                       .doAfterTerminate(() -> printlnToConsole(message("appService.logStreaming.hint.disconnected"), SYSTEM_OUTPUT))
                                       .subscribe((log) -> printlnToConsole(log, NORMAL_OUTPUT));
        }
    }

    public void closeStreamingLog() {
        if (isActive()) {
            subscription.dispose();
            printlnToConsole(message("appService.logStreaming.hint.disconnected"), SYSTEM_OUTPUT);
        }
    }

    public boolean isActive() {
        return subscription != null && !subscription.isDisposed();
    }

    public boolean isDisposed() {
        return this.isDisposed;
    }

    private void printlnToConsole(String message, ConsoleViewContentType consoleViewContentType) {
        this.print(message + SEPARATOR, consoleViewContentType);
    }

    @Override
    public void dispose() {
        super.dispose();
        this.isDisposed = true;
        closeStreamingLog();
    }
}
