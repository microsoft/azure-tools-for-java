/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2018-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
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
        Project project = LangDataKeys.PROJECT.getData(event.getDataContext());
        ToolWindowManager.getInstance(project).getToolWindow(ServerExplorerToolWindowFactory.EXPLORER_WINDOW).activate(null);
        return true;
    }
}
