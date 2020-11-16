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

package com.microsoft.intellij.helpers.springcloud;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskRunner;
import com.microsoft.azuretools.core.mvp.model.springcloud.AzureSpringCloudMvpModel;
import com.microsoft.intellij.helpers.ConsoleViewStatus;
import com.microsoft.intellij.helpers.StreamingLogsToolWindowManager;
import com.microsoft.intellij.util.PluginUtil;
import org.apache.http.HttpException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.intellij.helpers.ConsoleViewStatus.ACTIVE;
import static com.microsoft.intellij.helpers.ConsoleViewStatus.STOPPED;

public class SpringCloudStreamingLogManager {

    private Map<String, SpringCloudStreamingLogConsoleView> consoleViewMap = new HashMap<>();

    public static SpringCloudStreamingLogManager getInstance() {
        return SpringCloudStreamingLogManager.SingletonHolder.INSTANCE;
    }

    public void showStreamingLog(Project project, String appId, String instanceName) {
        final SpringCloudStreamingLogConsoleView consoleView = consoleViewMap.computeIfAbsent(
                instanceName, name -> new SpringCloudStreamingLogConsoleView(project, name));
        AzureTaskRunner.getInstance().runInBackground(new AzureTask(project, "Starting Streaming Log...", false, () -> {
            try {
                consoleView.startLog(() -> {
                    try {
                        return AzureSpringCloudMvpModel.getLogStream(appId, instanceName, 0, 10, 0, true);
                    } catch (IOException | HttpException e) {
                        return null;
                    }
                });
                StreamingLogsToolWindowManager.getInstance().showStreamingLogConsole(project, instanceName, instanceName, consoleView);
            } catch (Throwable e) {
                AzureTaskRunner.getInstance().runLater(() -> PluginUtil.displayErrorDialog("Failed to start streaming log", e.getMessage()));
                consoleView.shutdown();
            }
        }));
    }

    public void closeStreamingLog(String instanceName) {
        AzureTaskRunner.getInstance().runInBackground(new AzureTask(null, "Closing Streaming Log...", false, () -> {
            final SpringCloudStreamingLogConsoleView consoleView = consoleViewMap.get(instanceName);
            if (consoleView != null && consoleView.getStatus() == ACTIVE) {
                consoleView.shutdown();
            } else {
                AzureTaskRunner.getInstance().runLater(() -> PluginUtil.displayErrorDialog(
                    "Failed to close streaming log", "Log is not started."));
            }
        }));
    }

    public void removeConsoleView(String instanceName) {
        consoleViewMap.remove(instanceName);
    }

    public ConsoleViewStatus getConsoleViewStatus(String instanceName) {
        return consoleViewMap.containsKey(instanceName) ? consoleViewMap.get(instanceName).getStatus() : STOPPED;
    }

    private static final class SingletonHolder {
        private static final SpringCloudStreamingLogManager INSTANCE = new SpringCloudStreamingLogManager();

        private SingletonHolder() {
        }
    }
}
