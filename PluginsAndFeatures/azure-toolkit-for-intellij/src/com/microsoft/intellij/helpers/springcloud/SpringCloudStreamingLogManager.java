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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoft.azuretools.core.mvp.model.springcloud.AzureSpringCloudMvpModel;
import com.microsoft.intellij.helpers.StreamingLogsToolWindowManager;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SpringCloudStreamingLogManager {
    public static SpringCloudStreamingLogManager getInstance() {
        return SpringCloudStreamingLogManager.SingletonHolder.INSTANCE;
    }

    public Map<String, SpringCloudStreamingLogConsoleView> consoleViewMap = new HashMap<>();

    public void showStreamingLog(Project project, String appId, String instanceName) {
        DefaultLoader.getIdeHelper().runInBackground(project, "Starting Streaming Log...", false, true, null, () -> {
            try {
                SpringCloudStreamingLogConsoleView consoleView = consoleViewMap.get(instanceName);
                if (consoleView == null) {
                    consoleView = new SpringCloudStreamingLogConsoleView(project, instanceName);
                    consoleViewMap.put(instanceName, consoleView);
                }
                if (!consoleView.isEnable()) {
                    final InputStream logInputStream = AzureSpringCloudMvpModel
                            .getLogStream(appId, instanceName, 0, 10, 0, true);
                    consoleView.startLog(logInputStream);
                }
                StreamingLogsToolWindowManager.getInstance()
                                              .showStreamingLogConsole(project,
                                                                       instanceName,
                                                                       instanceName,
                                                                       consoleView);
            } catch (Throwable e) {
                ApplicationManager.getApplication().invokeLater(() -> PluginUtil.displayErrorDialog(
                        "Failed to start streaming log",
                        e.getMessage()));
            }
        });
    }

    public void closeStreamingLog(String instanceName) {
        ProgressManager.getInstance().run(new Task.Backgroundable(null, "Closing Streaming Log...", false) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {
                if (consoleViewMap.containsKey(instanceName) && consoleViewMap.get(instanceName).isEnable()) {
                    final SpringCloudStreamingLogConsoleView consoleView = consoleViewMap.get(instanceName);
                    consoleView.shutdown();
                } else {
                    ApplicationManager.getApplication().invokeLater(
                        () -> PluginUtil.displayErrorDialog("Failed to close streaming log",
                                                                "Log is not started."));
                }
            }
        });
    }

    public void removeConsoleView(String resourceId) {
        consoleViewMap.remove(resourceId);
    }

    private static final class SingletonHolder {
        private static final SpringCloudStreamingLogManager INSTANCE = new SpringCloudStreamingLogManager();

        private SingletonHolder() {
        }
    }
}
