/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.guidance;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Context;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.ide.guidance.config.TaskConfig;
import com.microsoft.azure.toolkit.ide.guidance.task.GuidanceTaskProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ContainerAppTaskProvider implements GuidanceTaskProvider {
    @Nullable
    @Override
    public Task createTask(@Nonnull TaskConfig config, @Nonnull Context context) {
        final ComponentContext taskContext = new ComponentContext(config, context);
        return switch (config.getName()) {
            case "task.common.create_resource_group" -> new CreateResourceGroupTask(taskContext);
            case "task.common.create_log_analytics_workspace" -> new CreateLogWorkspaceTask(taskContext);
            case "task.containerapp.create_env" -> new CreateContainerAppEnvTask(taskContext);
            case "task.containerapp.create_app" -> new CreateContainerAppTask(taskContext);
            case "task.containerapp.deploy" -> new DeployContainerAppTask(taskContext);
            case "task.containerapp.create_container_registry" -> new CreateContainerRegistryTask(taskContext);
            case "task.containerapp.open_in_browser" -> new OpenInBrowserTask(taskContext);
            case "task.containerapp.log_streaming" -> new OpenLogStreamingTask(taskContext);
            default -> null;
        };
    }
}
