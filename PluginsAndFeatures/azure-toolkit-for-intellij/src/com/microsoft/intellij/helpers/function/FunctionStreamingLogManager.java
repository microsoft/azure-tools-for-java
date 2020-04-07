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

import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import com.microsoft.intellij.helpers.StreamingLogsToolWindowManager;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import rx.Observable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public enum FunctionStreamingLogManager {
    INSTANCE;

    private static final String STREAMING_LOG_NOT_STARTED = "Streaming log is not started.";

    private Map<String, FunctionStreamingLogConsoleView> consoleViewMap = new HashMap<>();

    public void showStreamingLog(Project project, String subscriptionId, String appName, String appId) {
        DefaultLoader.getIdeHelper().runInBackground(project, "Starting Streaming Log...", false, true, null, () -> {
            try {
                if (isLinuxFunction(subscriptionId, appId)) {
                    // Todo: Open portal Application Insight pages for function logging
                    DefaultLoader.getIdeHelper().invokeLater(() -> PluginUtil.displayInfoDialog(
                            "Not supported",
                            "Log streaming for Linux Function App is not supported in current version."));
                    return;
                }
                FunctionStreamingLogConsoleView consoleView = consoleViewMap.get(appId);
                if (consoleView == null) {
                    consoleView = new FunctionStreamingLogConsoleView(project, appId);
                }
                if (!consoleView.isEnable()) {
                    FunctionApp functionApp = AzureFunctionMvpModel.getInstance()
                                                                   .getFunctionById(subscriptionId, appId);
                    // Todo: Check Whether file logging is enabled and prompt user to enable it
                    Observable<String> log = functionApp.streamAllLogsAsync();
                    consoleView.startStreamingLog(log);
                }
                consoleViewMap.put(appId, consoleView);
                StreamingLogsToolWindowManager.getInstance().showStreamingLogConsole(
                        project, appId, appName, consoleView);
            } catch (Throwable e) {
                DefaultLoader.getIdeHelper().invokeLater(() -> PluginUtil.displayErrorDialog(
                        "Failed to start streaming log",
                        e.getMessage()));
            }
        });
    }

    public void closeStreamingLog(Project project, String appId) {
        DefaultLoader.getIdeHelper().runInBackground(project, "Closing Streaming Log...", false, true, null, () -> {
            if (consoleViewMap.containsKey(appId) && consoleViewMap.get(appId).isEnable()) {
                final FunctionStreamingLogConsoleView consoleView = consoleViewMap.get(appId);
                consoleView.closeStreamingLog();
            } else {
                DefaultLoader.getIdeHelper().invokeLater(() -> PluginUtil.displayErrorDialog(
                        "Failed to close streaming log", STREAMING_LOG_NOT_STARTED));
            }
        });
    }

    public void removeConsoleView(String resourceId) {
        consoleViewMap.remove(resourceId);
    }

    private boolean isLinuxFunction(String subscription, String functionId) throws IOException {
        FunctionApp functionApp = AzureFunctionMvpModel.getInstance().getFunctionById(subscription, functionId);
        return functionApp.operatingSystem() == OperatingSystem.LINUX;
    }

}
