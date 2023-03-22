/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Context;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.ide.guidance.config.TaskConfig;
import com.microsoft.azure.toolkit.ide.guidance.task.GuidanceTaskProvider;
import com.microsoft.azure.toolkit.intellij.springcloud.task.CreateSpringAppTask;
import com.microsoft.azure.toolkit.intellij.springcloud.task.DeploySpringAppTask;
import com.microsoft.azure.toolkit.intellij.springcloud.task.OpenInBrowserTask;
import com.microsoft.azure.toolkit.intellij.springcloud.task.OpenLogStreamingTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class IntelliJSpringCloudTaskProvider implements GuidanceTaskProvider {
    @Nullable
    @Override
    public Task createTask(@Nonnull TaskConfig config, @Nonnull Context context) {
        final ComponentContext taskContext = new ComponentContext(config, context);
        return switch (config.getName()) {
            case "task.springcloud.create" -> new CreateSpringAppTask(taskContext);
            case "task.springcloud.deploy" -> new DeploySpringAppTask(taskContext);
            case "task.springcloud.open_in_browser" -> new OpenInBrowserTask(taskContext);
            case "task.springcloud.log_streaming" -> new OpenLogStreamingTask(taskContext);
            default -> null;
        };
    }
}
