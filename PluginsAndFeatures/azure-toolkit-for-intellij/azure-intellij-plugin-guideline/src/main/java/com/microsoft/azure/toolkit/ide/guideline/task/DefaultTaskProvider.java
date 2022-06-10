package com.microsoft.azure.toolkit.ide.guideline.task;

import com.microsoft.azure.toolkit.ide.guideline.Phase;
import com.microsoft.azure.toolkit.ide.guideline.Task;
import com.microsoft.azure.toolkit.ide.guideline.task.clone.GitCloneTask;
import com.microsoft.azure.toolkit.ide.guideline.task.create.webapp.CreateWebAppTask;
import com.microsoft.azure.toolkit.ide.guideline.task.deploy.DeployWebAppTask;

import javax.annotation.Nonnull;

public class DefaultTaskProvider implements GuidanceTaskProvider{
    @Override
    public Task createTask(@Nonnull String taskId, @Nonnull Phase phase) {
        switch (taskId) {
            case "tasks.clone":
                return new GitCloneTask(phase.getProcess());
            case "task.signin":
                return new SignInTask(phase.getProcess().getProject());
            case "task.select_subscription":
                return new SelectSubscriptionTask(phase.getProcess().getProject());
            case "task.webapp.create":
                return new CreateWebAppTask();
            case "task.webapp.deploy":
                return new DeployWebAppTask(phase.getProcess());
            case "task.resource.open_in_portal":
                return new OpenResourceInAzureAction();
            case "task.resource.clean_up":
                return new CleanUpResourceTask();
            default:
                return null;
        }
    }
}
