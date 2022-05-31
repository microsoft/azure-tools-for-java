/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guideline;

import com.microsoft.azure.toolkit.ide.guideline.config.PhaseConfig;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
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

    private Phase previous;
    private Phase following;

    public Phase(@Nonnull final PhaseConfig config, @Nonnull Process parent) {
        this.process = parent;
        this.id = UUID.randomUUID().toString();
        this.status = Status.INITIAL;
        this.name = config.getName();
        this.title = config.getTitle();
        this.description = config.getDescription();
        this.steps = config.getSteps().stream().map(stepConfig -> new Step(stepConfig, this)).collect(Collectors.toList());
    }

    // todo: clean previous listener
    public void setPrevious(final Phase previous) {
        this.previous = previous;
        if (previous != null) {
            previous.addStatusListener(previousStatus -> {
                if (previousStatus == Status.SUCCEED) {
                    this.prepareLaunch();
                }
            });
        }
    }

    public void prepareLaunch() {
        this.setStatus(Status.READY);
    }

    public List<InputComponent> getInputComponent() {
        return steps.stream().map(Step::getInputComponent).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public void setStatus(final Status status) {
//        AzureEventBus.emit("phase.update_status", this);
        this.status = status;
        this.listenerList.forEach(listener -> listener.accept(status));
    }

    public void execute() {
        execute(process.getContext());
    }

    public void execute(final Context context) {
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(AzureString.format("Running phase : %s", this.getName()), () -> {
            setStatus(Status.RUNNING);
            try {
                steps.forEach(step -> step.execute(context));
                setStatus(Status.SUCCEED);
            } catch (Exception e) {
                setStatus(Status.FAILED);
                AzureMessager.getMessager().error(e);
            }
        }));
    }

    private List<Consumer<Status>> listenerList = new ArrayList<>();

    public void addStatusListener(Consumer<Status> listener) {
        listenerList.add(listener);
    }

    public void removeStatusListener(Consumer<Status> listener) {
        listenerList.remove(listener);
    }

}
