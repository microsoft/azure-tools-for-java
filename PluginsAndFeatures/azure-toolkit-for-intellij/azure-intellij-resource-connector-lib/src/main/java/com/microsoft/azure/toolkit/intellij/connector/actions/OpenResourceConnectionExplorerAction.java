/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.connector.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoft.azure.toolkit.intellij.connector.ResourceConnectionExplorer;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.AzureAnAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpenResourceConnectionExplorerAction extends AzureAnAction {
    @Override
    @AzureOperation(name = "common.open_resource_connection_explorer", type = AzureOperation.Type.ACTION)
    public boolean onActionPerformed(@NotNull AnActionEvent anActionEvent, @Nullable Operation operation) {
        // open azure resource connection explorer
        final Project project = DataKeys.PROJECT.getData(anActionEvent.getDataContext());
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ResourceConnectionExplorer.ToolWindowFactory.ID);
        assert toolWindow != null;
        toolWindow.setAvailable(true);
        AzureTaskManager.getInstance().runLater(() -> toolWindow.activate(null));
        return true;
    }
}
