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
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import rx.Observable;

import javax.swing.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

//import static com.microsoft.azuretools.core.mvp.model.AzureMvpModel.APPLICATION_LOG_NOT_ENABLED;

public enum WebAppStreamingLogConsoleViewProvider {
    INSTANCE;

    public static final String LOG_TOOL_WINDOW = "Azure Streaming Log";
    public static final String STREAMING_LOG_NOT_STARTED = "Streaming log is not started.";

    private Map<String, WebAppStreamingLogConsoleView> consoleViewMap = new HashMap<>();
    private Map<Project, ToolWindow> toolWindowMap = new HashMap<>();

    public void startStreamingLogs(Project project, String subscriptionId, String webAppId, String webAppName, String slotName) {
        String consoleViewId = getConsoleViewId(webAppId, slotName);
        ToolWindow toolWindow = getToolWindow(project);
        try {
            WebAppStreamingLogConsoleView consoleView = consoleViewMap.containsKey(consoleViewId) ?
                    consoleViewMap.get(consoleViewId) :
                    createConsoleView(toolWindow, project, subscriptionId, webAppId, webAppName, slotName);
            consoleView.startStreamingLog();
            showConsoleView(toolWindow, consoleView.getLogConsole());
        } catch (IOException e) {
//            if (e instanceof IOException && e.getMessage().equals(APPLICATION_LOG_NOT_ENABLED)) {
//                enableStreamingLog(project, subscriptionId, webAppId, webAppName, slotName);
//            } else {
            UIUtils.showNotification(project, e.getMessage(), MessageType.ERROR);
//            }
        }
    }

//    private void enableStreamingLog(Project project, String subscriptionId, String webAppId, String webAppName, String slotName) {
//        int res = JOptionPane.showConfirmDialog(null,
//                String.format("Do you want to enable file logging for %s", getConsoleName(webAppName, slotName)),
//                "Enable logging",
//                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
//        if (res == JOptionPane.OK_OPTION) {
//            try {
//                if (StringUtils.isEmpty(slotName)) {
//                    AzureMvpModel.getInstance().enableAppServiceContainerLogging(subscriptionId, webAppId);
//                } else {
//                    AzureMvpModel.getInstance().enableDeploymentSlotLogging(subscriptionId, webAppId, slotName);
//                }
//                startStreamingLogs(project, subscriptionId, webAppId, webAppName, slotName);
//            } catch (Exception e) {
//                UIUtils.showNotification(project, e.getMessage(), MessageType.ERROR);
//            }
//        }
//    }

    public void stopStreamingLogs(String webAppId, String slotName) {
        String id = getConsoleViewId(webAppId, slotName);
        WebAppStreamingLogConsoleView consoleView = consoleViewMap.get(id);
        if (consoleView != null && consoleView.isEnable()) {
            consoleViewMap.get(id).closeStreamingLog();
        } else {
            throw new RuntimeException(STREAMING_LOG_NOT_STARTED);
        }
    }

    private WebAppStreamingLogConsoleView createConsoleView(ToolWindow toolWindow, Project project,
                                                            String subscriptionId, String webAppId,
                                                            String webAppName, String slotName) throws IOException {
        Observable<String> streamingLogs = StringUtils.isEmpty(slotName) ?
                AzureMvpModel.getInstance().getAppServiceStreamingLogs(subscriptionId, webAppId) :
                AzureMvpModel.getInstance().getAppServiceSlotStreamingLogs(subscriptionId, webAppId, slotName);

        String consoleName = getConsoleName(webAppName, slotName);
        ConsoleView consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).getConsole();
        ApplicationManager.getApplication().invokeLater(() -> {
            Content content = toolWindow.getContentManager().getFactory()
                    .createContent(consoleView.getComponent(), consoleName, false);
            // Set id to content description, in order to find and close it
            content.setDescription(getConsoleViewId(webAppId, slotName));
            toolWindow.getContentManager().addContent(content);
        });

        WebAppStreamingLogConsoleView logConsoleView = new WebAppStreamingLogConsoleView(streamingLogs, consoleView);
        consoleViewMap.put(webAppId, logConsoleView);
        return logConsoleView;
    }

    private void showConsoleView(ToolWindow toolWindow, ConsoleView consoleView) {
        ApplicationManager.getApplication().invokeLater(() -> {
            toolWindow.show(() -> {
            });
            Content consoleViewContent = toolWindow.getContentManager().getContent((JComponent) consoleView);
            toolWindow.getContentManager().setSelectedContent(consoleViewContent);
        });
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
                String consoleViewId = contentManagerEvent.getContent().getDescription();
                if (consoleViewMap.containsKey(consoleViewId)) {
                    consoleViewMap.get(consoleViewId).closeStreamingLog();
                    consoleViewMap.remove(consoleViewId);
                }
            }
        });
        return toolWindow;
    }

    private String getConsoleViewId(String webappId, String slotName) {
        return StringUtils.isEmpty(slotName) ? webappId : String.format("%s-%s", webappId, slotName);
    }

    private String getConsoleName(String webappName, String slotName) {
        return StringUtils.isEmpty(slotName) ? webappName : String.format("%s-%s", webappName, slotName);
    }

}
