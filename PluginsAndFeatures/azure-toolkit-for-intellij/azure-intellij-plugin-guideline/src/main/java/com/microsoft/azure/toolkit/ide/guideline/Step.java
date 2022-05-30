/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guideline;

import com.microsoft.azure.toolkit.ide.guideline.config.StepConfig;
import com.microsoft.azure.toolkit.ide.guideline.task.TaskManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import rx.Observable;

import javax.annotation.Nonnull;
import java.util.UUID;

@Data
@RequiredArgsConstructor
public class Step {
    private String id;
    private Status status;
    @Nonnull
    private String title;
    private String description;
    private String taskId;
    private Task task;

    private Phase phase;

    public Step(@Nonnull final StepConfig config, @Nonnull Phase phase) {
        this.phase = phase;
        this.id = UUID.randomUUID().toString();
        this.status = Status.INITIAL;
        this.title = config.getTitle();
        this.description = config.getDescription();
        this.taskId = config.getTask();
    }

    public Task getTask() {
        if (task == null) {
            task = TaskManager.getTaskById(taskId, phase.getProcess());
        }
        return task;
    }

    public InputComponent getInputComponent() {
        return getTask() == null ? null : getTask().getInputComponent();
    }

    public void execute(final Context context) {
        if (getTask() == null) {
            return;
        }
        AzureTaskManager.getInstance().runInBackground(AzureString.fromString(this.getTitle()), true,
                () -> getTask().execute(context));
    }

    public void executeWithUI(final Context context) {
        if (getTask() == null) {
            return;
        }
        AzureTaskManager.getInstance().runInBackground(AzureString.fromString(this.getTitle()), true,
                () -> getTask().executeWithUI(context));
    }
}
