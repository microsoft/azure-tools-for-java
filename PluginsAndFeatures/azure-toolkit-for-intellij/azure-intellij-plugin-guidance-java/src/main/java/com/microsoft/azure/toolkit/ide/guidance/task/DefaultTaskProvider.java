package com.microsoft.azure.toolkit.ide.guidance.task;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Context;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.ide.guidance.config.TaskConfig;

import javax.annotation.Nonnull;

public class DefaultTaskProvider implements GuidanceTaskProvider {
    @Override
    public Task createTask(@Nonnull TaskConfig config, @Nonnull Context context) {
        final ComponentContext taskContext = new ComponentContext(config, context);
        switch (config.getName()) {
            case "task.clone":
                return new GitCloneTask(taskContext);
            case "task.signin":
                return new SignInTask(taskContext);
            case "task.select_subscription":
                return new SelectSubscriptionTask(taskContext);
            case "task.resource.open_in_portal":
                return new OpenResourceInAzureTask(taskContext);
            case "task.resource.clean_up":
                return new CleanUpResourceTask(taskContext);
            case FocusResourceInAzureExplorerTask.ID:
                return new FocusResourceInAzureExplorerTask(taskContext);
            default:
                return null;
        }
    }
}
