/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.cosmosserverlessspark.spark.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.IconPathBuilder;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public class CosmosServerlessSparkJobsToolWindowFactory implements ToolWindowFactory {

    @Override
    public void init(@org.jetbrains.annotations.NotNull ToolWindow toolWindow) {
        toolWindow.setToHideOnEmptyContent(true);
    }

    @Override
    public boolean shouldBeAvailable(@org.jetbrains.annotations.NotNull Project project) {
        return false;
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        toolWindow.setIcon(
                IconLoader.getIcon(IconPathBuilder
                        .custom(CommonConst.CosmosServerlessToolWindowIconName)
                        .build()));
    }
}
