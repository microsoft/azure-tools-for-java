/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.utils.IProgressTaskImpl;
import com.microsoft.azuretools.utils.IWorker;

/**
 * Created by vlashch on 1/23/17.
 */
public class ProgressTaskModal implements IProgressTaskImpl {

    private Project project;

    public ProgressTaskModal(Project project) {
        this.project = project;
    }

    @Override
    public void doWork(IWorker worker) {
        AzureTaskManager.getInstance().runInModal(new AzureTask(project, worker.getName(), false, () -> {
            final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
            progressIndicator.setIndeterminate(true);
            try {
                worker.work(new UpdateProgressIndicator(progressIndicator));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }));
    }
}
