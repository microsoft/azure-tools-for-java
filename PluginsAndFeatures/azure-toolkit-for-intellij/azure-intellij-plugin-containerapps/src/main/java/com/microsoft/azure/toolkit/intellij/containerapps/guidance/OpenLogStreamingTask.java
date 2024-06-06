/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.guidance;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.intellij.containerapps.IntelliJContainerAppsActionsContributor;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;

import javax.annotation.Nonnull;
import java.util.Objects;

public class OpenLogStreamingTask implements Task {
    public static final String CONTAINER_APP = "containerApp";

    private final ComponentContext context;

    public OpenLogStreamingTask(@Nonnull final ComponentContext context) {
        this.context = context;
    }

    @Override
    @AzureOperation(name = "user/guidance.open_container_app_log_streaming")
    public void execute() throws Exception {
        final ContainerApp app = Objects.requireNonNull((ContainerApp) context.getParameter(CONTAINER_APP), "`containerApp` is required.");
        IntelliJContainerAppsActionsContributor.showConsoleStreamingLog(context.getProject(), app);
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.containerapp.log_streaming";
    }
}
