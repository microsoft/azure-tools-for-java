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
package com.microsoft.intellij.helpers;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import static com.intellij.execution.ui.ConsoleViewContentType.NORMAL_OUTPUT;
import static com.intellij.execution.ui.ConsoleViewContentType.SYSTEM_OUTPUT;

public class AppServiceStreamingLogConsoleView extends ConsoleViewImpl {

    private static final String SEPARATOR = System.getProperty("line.separator");
    private static final String START_LOG_STREAMING = "Connecting to log stream...";
    private static final String STOP_LOG_STREAMING = "Disconnected from log-streaming service.";

    private boolean isDisposed;
    private String resourceId;
    private Subscription subscription;

    public AppServiceStreamingLogConsoleView(@NotNull Project project, String resourceId) {
        super(project, true);
        this.isDisposed = false;
        this.resourceId = resourceId;
    }

    public void startStreamingLog(Observable<String> logStreaming) {
        if (!isActive()) {
            printlnToConsole(START_LOG_STREAMING, SYSTEM_OUTPUT);
            subscription = logStreaming.subscribeOn(Schedulers.io())
                                       .doAfterTerminate(() -> printlnToConsole(STOP_LOG_STREAMING, SYSTEM_OUTPUT))
                                       .subscribe((log) -> printlnToConsole(log, NORMAL_OUTPUT));
        }
    }

    public void closeStreamingLog() {
        if (isActive()) {
            subscription.unsubscribe();
            printlnToConsole(STOP_LOG_STREAMING, SYSTEM_OUTPUT);
        }
    }

    public boolean isActive() {
        return subscription != null && !subscription.isUnsubscribed();
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
