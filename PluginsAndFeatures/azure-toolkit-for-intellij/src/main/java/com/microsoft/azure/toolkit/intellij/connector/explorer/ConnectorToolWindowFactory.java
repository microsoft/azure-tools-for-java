/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.explorer;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.jetbrains.annotations.NotNull;

public class ConnectorToolWindowFactory implements ToolWindowFactory {
    public static final String ID = "Azure Resource Connector";

    @Override
    @AzureOperation(name = "connector|explorer.initialize", type = AzureOperation.Type.SERVICE)
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {
        final ConnectorToolWindow myToolWindow = new ConnectorToolWindow(project);
        final ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        final Content content = contentFactory.createContent(myToolWindow, "", false);
        toolWindow.getContentManager().addContent(content);
    }

}
