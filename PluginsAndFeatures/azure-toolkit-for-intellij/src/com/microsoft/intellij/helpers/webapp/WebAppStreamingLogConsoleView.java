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
package com.microsoft.intellij.helpers.webapp;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class WebAppStreamingLogConsoleView {

    private static final String SEPARATOR = System.getProperty("line.separator");
    private static final Map<String, ConsoleViewContentType> LOG_LEVEL = new HashMap<>();

    private Observable<String> logStreaming;
    private Subscription subscription;
    private ConsoleView logConsole;
    private boolean enable = false;

    static {
        LOG_LEVEL.put("INFO", ConsoleViewContentType.LOG_INFO_OUTPUT);
        LOG_LEVEL.put("WARNING", ConsoleViewContentType.LOG_WARNING_OUTPUT);
        LOG_LEVEL.put("ERROR", ConsoleViewContentType.LOG_ERROR_OUTPUT);
        LOG_LEVEL.put("DEBUG", ConsoleViewContentType.LOG_DEBUG_OUTPUT);
    }

    public WebAppStreamingLogConsoleView(Observable<String> logStreaming, ConsoleView logConsole) {
        this.logStreaming = logStreaming;
        this.logConsole = logConsole;
    }

    public void startStreamingLog() {
        if (!enable) {
            subscription = logStreaming.subscribeOn(Schedulers.io())
                    .doAfterTerminate(() -> logConsole.print("Disconnected from log-streaming service." + SEPARATOR, ConsoleViewContentType.SYSTEM_OUTPUT))
                    .subscribe((log) -> {
                        String logLevel = LOG_LEVEL.keySet().stream().filter(level -> log.contains(level)).findAny().orElse(null);
                        ConsoleViewContentType type = logLevel == null ? ConsoleViewContentType.NORMAL_OUTPUT : LOG_LEVEL.get(logLevel);
                        ApplicationManager.getApplication().invokeLater(() -> logConsole.print(log + SEPARATOR, type));
                    });
            enable = true;
        }
    }

    public void closeStreamingLog() {
        subscription.unsubscribe();
        logConsole.print("Disconnected from log-streaming service." + SEPARATOR, ConsoleViewContentType.SYSTEM_OUTPUT);
        enable = false;
    }

    public boolean isEnable() {
        return enable;
    }
}
