/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.monitor;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.monitor.view.AzureMonitorView;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.monitor.AzureLogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspace;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Objects;
import java.util.Optional;

public class AzureMonitorManager {
    public static final String AZURE_MONITOR_WINDOW = "Azure Monitor";
    public static final String AZURE_MONITOR_TRIGGERED = "AzureMonitor.Triggered";
    public static final String AZURE_MONITOR_SELECTED_WORKSPACE = "AzureMonitor.SelectedLogAnalyticsWorkspace";
    public static final String AZURE_MONITOR_CUSTOM_QUERY_LIST = "AzureMonitor.CustomQueryList";
    public static final String QUERIES_TAB_NAME = "Queries";
    private static final AzureMonitorManager instance = new AzureMonitorManager();
    public static AzureMonitorManager getInstance() {
        return instance;
    }

    @AzureOperation(name="user/monitor.open_azure_monitor")
    public void openMonitorWindow(@Nonnull Project project, @Nullable LogAnalyticsWorkspace workspace, @Nullable String resourceId) {
        final ToolWindow azureMonitorWindow = getToolWindow(project, workspace, resourceId);
        Optional.ofNullable(workspace).ifPresent(w -> AzureEventBus.emit("azure.monitor.change_workspace", w));
        Optional.ofNullable(azureMonitorWindow).ifPresent(it -> AzureTaskManager.getInstance().runLater(
                () -> it.activate(() -> {
                    it.setAvailable(true);
                    it.show();
                }))
        );
    }

    @Nullable
    public AzureMonitorView getContentViewByTabName(@Nonnull Project project, String tabName) {
        return Optional.ofNullable(ToolWindowManager.getInstance(project).getToolWindow(AZURE_MONITOR_WINDOW))
                .map(toolWindow -> toolWindow.getContentManager().findContent(tabName))
                .filter(content -> content.getComponent() instanceof AzureMonitorView)
                .map(content -> (AzureMonitorView) content.getComponent())
                .orElse(null);
    }

    public void changeContentView(@Nonnull Project project, String tabName) {
        Optional.ofNullable(ToolWindowManager.getInstance(project).getToolWindow(AZURE_MONITOR_WINDOW))
            .map(ToolWindow::getContentManager)
            .map(m -> m.findContent(tabName))
            .filter(c -> Objects.nonNull(c.getManager()))
            .ifPresent(c -> c.getManager().setSelectedContent(c));
    }

    @Nullable
    private ToolWindow getToolWindow(@Nonnull Project project, @Nullable LogAnalyticsWorkspace workspace, @Nullable String resourceId) {
        return initToolWindow(project, workspace, resourceId);
    }

    @Nullable
    private ToolWindow initToolWindow(@Nonnull Project project, @Nullable LogAnalyticsWorkspace workspace, @Nullable String resourceId) {
        final ToolWindow azureMonitorWindow = ToolWindowManager.getInstance(project).getToolWindow(AZURE_MONITOR_WINDOW);
        if (Objects.isNull(azureMonitorWindow)) {
            return null;
        }
        final Content tablesContent = azureMonitorWindow.getContentManager().findContent("Tables");
        if (Objects.isNull(tablesContent)) {
            final AzureMonitorView monitorTableView = new AzureMonitorView(project, workspace, true, resourceId);
            this.addContent(azureMonitorWindow, "Tables", monitorTableView);
        } else if (tablesContent.getComponent() instanceof AzureMonitorView) {
            ((AzureMonitorView) tablesContent.getComponent()).setSelectedWorkspace(workspace);
            ((AzureMonitorView) tablesContent.getComponent()).setInitResourceId(resourceId);
        }
        final Content queriesContent = azureMonitorWindow.getContentManager().findContent("Queries");
        if (Objects.isNull(queriesContent)) {
            final AzureMonitorView monitorQueryView = new AzureMonitorView(project, workspace, false, resourceId);
            this.addContent(azureMonitorWindow, "Queries", monitorQueryView);
        } else if (tablesContent.getComponent() instanceof AzureMonitorView) {
            ((AzureMonitorView) tablesContent.getComponent()).setSelectedWorkspace(workspace);
        }
        return azureMonitorWindow;
    }

    private void addContent(@Nonnull ToolWindow toolWindow, String contentName, AzureMonitorView view) {
        final Content tableContent = ContentFactory.SERVICE.getInstance().createContent(view, contentName, true);
        tableContent.setCloseable(false);
        tableContent.setDisposer(view);
        toolWindow.getContentManager().addContent(tableContent);
    }

    public static class AzureMonitorFactory implements ToolWindowFactory, DumbAware {
        private boolean isTriggered = false;

        @Override
        public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
            if (!isTriggered) {
                isTriggered = PropertiesComponent.getInstance().getBoolean(AzureMonitorManager.AZURE_MONITOR_TRIGGERED);
            }
            toolWindow.setIcon(isTriggered ? IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE_MONITOR) : IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE_MONITOR_NEW));
            try {
                final String workspaceResourceId = PropertiesComponent.getInstance().getValue(AzureMonitorManager.AZURE_MONITOR_SELECTED_WORKSPACE, "");
                final LogAnalyticsWorkspace workspace = Azure.az(AzureLogAnalyticsWorkspace.class).getById(workspaceResourceId);
                instance.initToolWindow(project, workspace, null);
            } catch (final Exception e) {
                instance.initToolWindow(project, null, null);
            }
        }
    }
}
