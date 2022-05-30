package com.microsoft.azure.toolkit.ide.guideline.task;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.guideline.Context;
import com.microsoft.azure.toolkit.ide.guideline.InputComponent;
import com.microsoft.azure.toolkit.ide.guideline.Process;
import com.microsoft.azure.toolkit.ide.guideline.Step;
import com.microsoft.azure.toolkit.ide.guideline.Task;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

public class SelectSubscriptionTask implements Task {

    private Project project;

    public SelectSubscriptionTask(Project project) {
        this.project = project;
    }

    @Override
    public Step create(Process process) {
        return null;
    }

    @Override
    public InputComponent getInputComponent() {
        return null;
    }

    @Override
    public void execute(Context context) {

    }

    @Override
    public void executeWithUI(Context context) {
        final AnAction action = ActionManager.getInstance().getAction("AzureToolkit.SelectSubscriptions");
        final DataContext dataContext = dataId -> CommonDataKeys.PROJECT.getName().equals(dataId) ? project : null;
        AzureTaskManager.getInstance().runLater(() -> ActionUtil.invokeAction(action, dataContext, "AzurePluginStartupActivity", null, null));
    }
}
