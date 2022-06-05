/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guideline;

import com.microsoft.azure.toolkit.ide.guideline.config.StepConfig;
import com.microsoft.azure.toolkit.ide.guideline.task.TaskManager;
import lombok.Data;
import lombok.RequiredArgsConstructor;

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

    public void execute(final Context context) throws Exception {
        if (getTask() == null) {
            return;
        }
        getTask().execute(context);
    }

    public void executeWithUI(final Context context) throws Exception {
        if (getTask() == null) {
            return;
        }
        getTask().executeWithUI(context);
    }
}
