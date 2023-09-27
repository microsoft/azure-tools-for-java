/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.streaminglog;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class StreamingLogsToolWindowManager {

    private static final String LOG_TOOL_WINDOW = "Azure Streaming Log";
    private final BidiMap<String, String> resourceIdToNameMap = new DualHashBidiMap<>();

    public static StreamingLogsToolWindowManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @AzureOperation(name = "boundary/common.open_log_streaming_console.resource", params = {"resourceName"})
    public void showStreamingLogConsole(Project project, String resourceId, String resourceName, ConsoleView consoleView) {
        final ToolWindow toolWindow = getToolWindow(project);
        final ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        final String consoleName = getConsoleViewName(resourceId, resourceName);
        Content content = toolWindow.getContentManager().findContent(consoleName);
        if (content == null) {
            content = contentFactory.createContent(consoleView.getComponent(), consoleName, false);
            content.setDisposer(consoleView);
            toolWindow.getContentManager().addContent(content);
        }
        toolWindow.getContentManager().setSelectedContent(content);
        toolWindow.setAvailable(true);
        toolWindow.activate(null);
    }

    @Nullable
    public StreamingLogsConsoleView getToolWindowContent(Project project, String resourceId) {
        final ToolWindow toolWindow = getToolWindow(project);
        final String consoleName = Optional.ofNullable(resourceIdToNameMap.get(resourceId)).orElse(StringUtils.EMPTY);
        return (StreamingLogsConsoleView) Optional.ofNullable(toolWindow.getContentManager().findContent(consoleName))
                .map(Content::getDisposer)
                .orElse(null);
    }

    public List<StreamingLogsConsoleView> getToolWindowContents(Project project, String resourceIdPrefix) {
        final ToolWindow toolWindow = getToolWindow(project);
        final List<String> consoleNames = resourceIdToNameMap.keySet().stream().filter(k -> k.contains(resourceIdPrefix)).map(resourceIdToNameMap::get).collect(Collectors.toList());
        return consoleNames.stream().filter(Objects::nonNull)
                .map(n -> toolWindow.getContentManager().findContent(n).getDisposer())
                .filter(d -> d instanceof StreamingLogsConsoleView)
                .map(d -> (StreamingLogsConsoleView)d).collect(Collectors.toList());
    }

    public void removeConsoleViewName(String value) {
        this.resourceIdToNameMap.removeValue(value);
    }

    public Map<String, String> getResourceIdToNameMap() {
        return Collections.unmodifiableMap(resourceIdToNameMap);
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
        return ToolWindowManager.getInstance(project).getToolWindow(LOG_TOOL_WINDOW);
    }

    private static final class SingletonHolder {
        private static final StreamingLogsToolWindowManager INSTANCE = new StreamingLogsToolWindowManager();

        private SingletonHolder() {
        }
    }
}
