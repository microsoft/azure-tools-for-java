package com.microsoft.azure.toolkit.ide.guideline.task;

import com.microsoft.azure.toolkit.ide.guideline.Process;
import com.microsoft.azure.toolkit.ide.guideline.Task;
import com.microsoft.azure.toolkit.ide.guideline.task.clone.GitCloneTask;

public class TaskManager {
    public static Task getTaskById(String id, Process process) {
        switch (id) {
            case "tasks.clone":
                return new GitCloneTask(process);
            case "task.signin":
                return new SignInTask(process.getProject());
            case "task.select_subscription":
                return new SelectSubscriptionTask(process.getProject());
            default:
                return null;
        }
    }
}
