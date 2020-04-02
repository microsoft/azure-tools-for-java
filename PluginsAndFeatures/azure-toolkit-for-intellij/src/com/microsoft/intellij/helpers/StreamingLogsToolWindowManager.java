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

import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManagerAdapter;
import com.intellij.ui.content.ContentManagerEvent;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import java.util.HashMap;
import java.util.Map;

public class StreamingLogsToolWindowManager {

    private static final String LOG_TOOL_WINDOW = "Azure Streaming Log";

    private Map<Project, ToolWindow> toolWindowMap = new HashMap<>();
    private BidiMap<String, String> resourceIdToNameMap = new DualHashBidiMap<>();

    public static StreamingLogsToolWindowManager getInstance() {
        return StreamingLogsToolWindowManager.SingletonHolder.INSTANCE;
    }

    public void showStreamingLogConsole(Project project, String resourceId, String resourceName,
                                        ConsoleView consoleView) {
        final ToolWindow toolWindow = getToolWindow(project);
        ApplicationManager.getApplication().invokeLater(() -> {
            toolWindow.show(null);
            final ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
            final String consoleName = getConsoleViewName(resourceId, resourceName);
            Content content = toolWindow.getContentManager().findContent(consoleName);
            if (content == null) {
                content = contentFactory.createContent(consoleView.getComponent(), consoleName, false);
                content.setDisposer(consoleView);
                toolWindow.getContentManager().addContent(content);
            }
            toolWindow.getContentManager().setSelectedContent(content);
        });
    }

    public void removeConsoleView(String resourceId) {
        resourceIdToNameMap.remove(resourceId);
    }

    private String getConsoleViewName(String resourceId, String resourceName) {
        if (resourceIdToNameMap.containsKey(resourceId)) {
            return resourceIdToNameMap.get(resourceId);
        }
        String result = resourceName;
        int i = 1;
        while (resourceIdToNameMap.containsValue(result)) {
            result = String.format("%s(%s)", resourceName, i++);
        }
        resourceIdToNameMap.put(resourceId, result);
        return result;
    }

    private ToolWindow getToolWindow(Project project) {
        if (toolWindowMap.containsKey(project)) {
            return toolWindowMap.get(project);
        }
        // Add content manager listener when get tool window at the first time
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(LOG_TOOL_WINDOW);
        toolWindow.getContentManager().addContentManagerListener(new ContentManagerAdapter() {
            @Override
            public void contentRemoved(ContentManagerEvent contentManagerEvent) {
                final String displayName = contentManagerEvent.getContent().getDisplayName();
                resourceIdToNameMap.removeValue(displayName);
            }
        });
        toolWindowMap.put(project, toolWindow);
        return toolWindow;
    }

    private static final class SingletonHolder {
        private static final StreamingLogsToolWindowManager INSTANCE = new StreamingLogsToolWindowManager();

        private SingletonHolder() {
        }
    }
}
