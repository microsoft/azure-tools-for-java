/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.streaminglog;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperationTitle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.core.mvp.model.springcloud.AzureSpringCloudMvpModel;
import com.microsoft.intellij.helpers.ConsoleViewStatus;
import com.microsoft.azure.toolkit.intellij.appservice.StreamingLogsToolWindowManager;
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
        final IAzureOperationTitle title = AzureOperationBundle.title("springcloud|log_stream.start", instanceName);
        AzureTaskManager.getInstance().runInBackground(new AzureTask(project, title, false, () -> {
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
                AzureTaskManager.getInstance().runLater(() -> PluginUtil.displayErrorDialog("Failed to start streaming log", e.getMessage()));
                consoleView.shutdown();
            }
        }));
    }

    public void closeStreamingLog(String instanceName) {
        final IAzureOperationTitle title = AzureOperationBundle.title("springcloud|log_stream.close", instanceName);
        AzureTaskManager.getInstance().runInBackground(new AzureTask(null, title, false, () -> {
            final SpringCloudStreamingLogConsoleView consoleView = consoleViewMap.get(instanceName);
            if (consoleView != null && consoleView.getStatus() == ACTIVE) {
                consoleView.shutdown();
            } else {
                AzureTaskManager.getInstance().runLater(() -> PluginUtil.displayErrorDialog(
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
