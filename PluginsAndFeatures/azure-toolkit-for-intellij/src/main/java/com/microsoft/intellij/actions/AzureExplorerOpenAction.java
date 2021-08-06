/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.intellij.AzureAnAction;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.ui.ServerExplorerToolWindowFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AzureExplorerOpenAction extends AzureAnAction {
    @Override
    @AzureOperation(name = "common|explorer.open", type = AzureOperation.Type.ACTION)
    public boolean onActionPerformed(@NotNull AnActionEvent event, @Nullable Operation operation) {
        Project project = DataKeys.PROJECT.getData(event.getDataContext());
        ToolWindowManager.getInstance(project).getToolWindow(ServerExplorerToolWindowFactory.EXPLORER_WINDOW).activate(null);
        return true;
    }
}
