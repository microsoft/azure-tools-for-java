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

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManagerAdapter;
import com.intellij.ui.content.ContentManagerEvent;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.intellij.ui.util.UIUtils;
import org.jetbrains.annotations.NotNull;
import rx.Observable;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public enum WebAppStreamingLogConsoleViewProvider {
    INSTANCE;

    public static final String LOG_TOOL_WINDOW = "Azure Streaming Log";
    public static final String STREAMING_LOG_NOT_STARTED = "Streaming log is not started.";

    private Map<String, WebAppStreamingLogConsoleView> consoleViewMap = new HashMap<>();
    private Map<Project, ToolWindow> toolWindowMap = new HashMap<>();

    public void startStreamingLogs(Project project, String subscriptionId, String webAppId, String webAppName) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                ToolWindow toolWindow = getToolWindow(project);
                WebAppStreamingLogConsoleView consoleView = consoleViewMap.containsKey(webAppId) ?
                        consoleViewMap.get(webAppId) :
                        createConsoleView(toolWindow, project, subscriptionId, webAppId, webAppName);
                consoleView.startStreamingLog();
                showConsoleView(toolWindow, webAppId);
            } catch (Exception e) {
                UIUtils.showNotification(project, e.getMessage(), MessageType.ERROR);
            }
        });
    }

    public void stopStreamingLogs(String webAppId) {
        if (consoleViewMap.containsKey(webAppId)) {
            consoleViewMap.get(webAppId).closeStreamingLog();
        } else {
            throw new RuntimeException(STREAMING_LOG_NOT_STARTED);
        }
    }

    private WebAppStreamingLogConsoleView createConsoleView(ToolWindow toolWindow, Project project,
                                                            String subscriptionId, String webAppId,
                                                            String webAppName) throws IOException {
        ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
        Content content = toolWindow.getContentManager().getFactory()
                .createContent(consoleView.getComponent(), webAppName, false);
        content.setDescription(webAppId);
        toolWindow.getContentManager().addContent(content);

        Observable<String> streamingLogs = AzureMvpModel.getInstance()
                .getAppServiceStreamingLogs(subscriptionId, webAppId);
        WebAppStreamingLogConsoleView logConsoleView = new WebAppStreamingLogConsoleView(streamingLogs, consoleView);
        consoleViewMap.put(webAppId, logConsoleView);
        return logConsoleView;
    }

    private void showConsoleView(ToolWindow toolWindow, String webAppId) {
        toolWindow.show(() -> {
        });
        Content consoleViewContent = Arrays.stream(toolWindow.getContentManager().getContents())
                .filter(content -> content.getDescription().equals(webAppId))
                .findAny().orElse(null);
        ApplicationManager.getApplication().invokeLater(
                () -> toolWindow.getContentManager().setSelectedContent(consoleViewContent));
    }

    private ToolWindow getToolWindow(Project project) {
        if (toolWindowMap.containsKey(project)) {
            return toolWindowMap.get(project);
        }
        // Add content manager listener when get tool window at the first time
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(LOG_TOOL_WINDOW);
        toolWindow.getContentManager().addContentManagerListener(new ContentManagerAdapter() {
            @Override
            public void contentRemoved(@NotNull ContentManagerEvent contentManagerEvent) {
                Content content = contentManagerEvent.getContent();
                if (consoleViewMap.containsKey(content.getDescription())) {
                    consoleViewMap.get(content.getDescription()).closeStreamingLog();
                    consoleViewMap.remove(content.getDescription());
                }
            }
        });
        return toolWindow;
    }

}
