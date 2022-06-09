/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guideline;

import com.microsoft.azure.toolkit.ide.guideline.config.StepConfig;
import com.microsoft.azure.toolkit.ide.guideline.task.TaskManager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

@Data
@RequiredArgsConstructor
public class Step {
    @Nonnull
    private final String id;
    @Nonnull
    private final String title;

    @Nullable
    private final String description;

    @Nonnull
    private final Task task;

    @Nonnull
    private final Phase phase;

    @Nonnull
    private Status status = Status.INITIAL;

    private IAzureMessager output;

    public Step(@Nonnull final StepConfig config, @Nonnull Phase phase) {
        this.phase = phase;
        this.id = UUID.randomUUID().toString();
        this.title = config.getTitle();
        this.description = config.getDescription();
        this.task = TaskManager.getTaskById(config.getTask(), phase.getProcess());
    }

    public InputComponent getInput() {
        return getTask().getInput();
    }

    public void execute(final Context context) throws Exception {
        getTask().execute(context);
    }
}
