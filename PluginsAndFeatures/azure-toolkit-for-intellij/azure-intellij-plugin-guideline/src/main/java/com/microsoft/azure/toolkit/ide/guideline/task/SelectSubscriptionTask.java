package com.microsoft.azure.toolkit.ide.guideline.task;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.guideline.Context;
import com.microsoft.azure.toolkit.ide.guideline.InputComponent;
import com.microsoft.azure.toolkit.ide.guideline.Task;

public class SelectSubscriptionTask implements Task {

    private Project project;

    public SelectSubscriptionTask(Project project) {
        this.project = project;
    }

    @Override
    public InputComponent getInput() {
        return null;
    }

    @Override
    public void execute(Context context) {

    }
}
