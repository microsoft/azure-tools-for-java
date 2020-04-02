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
package com.microsoft.intellij.helpers.function;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.intellij.execution.ui.ConsoleViewContentType.*;

public class FunctionStreamingLogConsoleView extends ConsoleViewImpl {

    private static final String SEPARATOR = System.getProperty("line.separator");
    private static final String START_LOG_STREAMING = "Connecting to log stream...";
    private static final String STOP_LOG_STREAMING = "Disconnected from log-streaming service.";

    private Observable<String> logStreaming;
    private Subscription subscription;
    private String resourceId;

    private AtomicBoolean enable;
    private ExecutorService executorService;
    private InputStream logInputStream;

    public FunctionStreamingLogConsoleView(@NotNull Project project, String resourceId) {
        super(project, true);
        this.resourceId = resourceId;
        this.enable = new AtomicBoolean();
    }

    public void startStreamingLog(Observable<String> logStreaming) {
        if (!isEnable()) {
            printlnToConsole(START_LOG_STREAMING, SYSTEM_OUTPUT);
            subscription = logStreaming.subscribeOn(Schedulers.io())
                    .doAfterTerminate(() -> printlnToConsole(STOP_LOG_STREAMING, SYSTEM_OUTPUT))
                    .subscribe((log) -> printlnToConsole(log, NORMAL_OUTPUT));
        }
    }

    public void closeStreamingLog() {
        subscription.unsubscribe();
        printlnToConsole(STOP_LOG_STREAMING, SYSTEM_OUTPUT);
    }

    public boolean isEnable() {
        return subscription != null && !subscription.isUnsubscribed();
    }


    private void printlnToConsole(String message, ConsoleViewContentType consoleViewContentType) {
        this.print(message + SEPARATOR, consoleViewContentType);
    }

    @Override
    public void dispose() {
        super.dispose();
        closeStreamingLog();
        FunctionStreamingLogManager.INSTANCE.removeConsoleView(resourceId);
    }
}
