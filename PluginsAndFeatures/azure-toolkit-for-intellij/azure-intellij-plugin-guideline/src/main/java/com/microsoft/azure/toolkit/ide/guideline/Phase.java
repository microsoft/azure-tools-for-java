/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guideline;

import com.microsoft.azure.toolkit.ide.guideline.config.PhaseConfig;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@RequiredArgsConstructor
public class Phase {
    private Process process;

    private String id;
    private Status status;
    private String name;
    @Nonnull
    private String title;
    private String description;
    private List<Step> steps;

    public Phase(@Nonnull final PhaseConfig config, @Nonnull Process parent) {
        this.process = parent;
        this.id = UUID.randomUUID().toString();
        this.status = Status.INITIAL;
        this.name = config.getName();
        this.title = config.getTitle();
        this.description = config.getDescription();
        this.steps = config.getSteps().stream().map(stepConfig -> new Step(stepConfig, this)).collect(Collectors.toList());
    }

    public List<InputComponent> getInputComponent() {
        return steps.stream().map(Step::getInputComponent).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public void execute(final Context context) {
        AzureEventBus.emit("phase.run_phase", this);
        try {
            steps.forEach(step -> step.execute(context));
            AzureEventBus.emit("phase.run_phase_succeed", this);
        } catch (Exception e) {
            AzureEventBus.emit("phase.run_phase_failed", this);
            throw e;
        }
    }
}
