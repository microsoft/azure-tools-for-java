package com.microsoft.azure.toolkit.ide.guideline.task;

import com.microsoft.azure.toolkit.ide.guideline.Process;
import com.microsoft.azure.toolkit.ide.guideline.Task;
import com.microsoft.azure.toolkit.ide.guideline.task.clone.GitCloneTask;
import com.microsoft.azure.toolkit.ide.guideline.task.create.webapp.CreateWebAppTask;
import com.microsoft.azure.toolkit.ide.guideline.task.deploy.DeployWebAppTask;

import javax.annotation.Nonnull;

public class TaskManager {
    @Nonnull
    public static Task getTaskById(String id, Process process) {
        switch (id) {
            case "tasks.clone":
                return new GitCloneTask(process);
            case "task.signin":
                return new SignInTask(process.getProject());
            case "task.select_subscription":
                return new SelectSubscriptionTask(process.getProject());
            case "task.webapp.create":
                return new CreateWebAppTask();
            case "task.webapp.deploy":
                return new DeployWebAppTask(process);
            case "task.resource.open_in_portal":
                return new OpenResourceInAzureAction();
            case "task.resource.clean_up":
                return new CleanUpResourceTask();
            default:
                return null;
        }
    }
}
