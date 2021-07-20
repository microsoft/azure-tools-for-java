/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.streaminglog;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.appservice.StreamingLogsToolWindowManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.intellij.helpers.ConsoleViewStatus;
import com.microsoft.intellij.util.PluginUtil;
import lombok.SneakyThrows;
import org.apache.http.HttpException;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.intellij.helpers.ConsoleViewStatus.ACTIVE;
import static com.microsoft.intellij.helpers.ConsoleViewStatus.STOPPED;

public class SpringCloudStreamingLogManager {

    private final Map<String, SpringCloudStreamingLogConsoleView> consoleViewMap = new HashMap<>();

    public static SpringCloudStreamingLogManager getInstance() {
        return SpringCloudStreamingLogManager.SingletonHolder.INSTANCE;
    }

    public void showStreamingLog(Project project, SpringCloudApp app, String instanceName) {
        final SpringCloudStreamingLogConsoleView consoleView = consoleViewMap.computeIfAbsent(
                instanceName, name -> new SpringCloudStreamingLogConsoleView(project, name));
        final AzureString title = AzureOperationBundle.title("springcloud|log_stream.start", instanceName);
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, title, false, () -> {
            try {
                consoleView.startLog(() -> {
                    try {
                        return getLogStream(app, instanceName, 0, 10, 0, true);
                    } catch (final IOException | HttpException e) {
                        return null;
                    }
                });
                StreamingLogsToolWindowManager.getInstance().showStreamingLogConsole(project, instanceName, instanceName, consoleView);
            } catch (final Throwable e) {
                AzureTaskManager.getInstance().runLater(() -> PluginUtil.displayErrorDialog("Failed to start streaming log", e.getMessage()));
                consoleView.shutdown();
            }
        }));
    }

    public void closeStreamingLog(String instanceName) {
        final AzureString title = AzureOperationBundle.title("springcloud|log_stream.close", instanceName);
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(null, title, false, () -> {
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

    @SneakyThrows
    public static InputStream getLogStream(SpringCloudApp app, String instanceName, int sinceSeconds, int tailLines, int limitBytes, boolean follow) throws IOException, HttpException {
        final URIBuilder endpoint = new URIBuilder(app.entity().getLogStreamingEndpoint(instanceName));
        endpoint.addParameter("follow", String.valueOf(follow));
        if (sinceSeconds > 0) {
            endpoint.addParameter("sinceSeconds", String.valueOf(sinceSeconds));
        }
        if (tailLines > 0) {
            endpoint.addParameter("tailLines", String.valueOf(tailLines));
        }
        if (limitBytes > 0) {
            endpoint.addParameter("limitBytes", String.valueOf(limitBytes));
        }
        final String password = app.getCluster().entity().getTestKey();
        final String userPass = "primary:" + password;
        final String basicAuth = "Basic " + new String(Base64.getEncoder().encode(userPass.getBytes()));
        final HttpURLConnection connection = (HttpURLConnection) endpoint.build().toURL().openConnection();
        connection.setRequestProperty("Authorization", basicAuth);
        connection.setReadTimeout(600000);
        connection.setConnectTimeout(3000);
        connection.setRequestMethod("GET");
        connection.connect();
        if (connection.getResponseCode() == 200) {
            return connection.getInputStream();
        } else {
            throw new HttpException("Failed to get log stream due to http error, unexpectedly status code: " + connection.getResponseCode());
        }
    }

    private static final class SingletonHolder {
        private static final SpringCloudStreamingLogManager INSTANCE = new SpringCloudStreamingLogManager();

        private SingletonHolder() {
        }
    }
}
