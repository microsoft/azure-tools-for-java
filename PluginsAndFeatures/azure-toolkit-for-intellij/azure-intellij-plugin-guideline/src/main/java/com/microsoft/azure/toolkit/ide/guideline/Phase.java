/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guideline;

import com.microsoft.azure.toolkit.ide.guideline.config.PhaseConfig;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Data
@RequiredArgsConstructor
public class Phase {
    @Nonnull
    private final Process process;

    @Nonnull
    private final String id;
    @Nonnull
    private final String type;
    @Nonnull
    private final String title;

    @Nullable
    private final String description;
    @Nonnull
    private final List<Step> steps;
    @Nonnull
    private Status status;

    private Step currentStep;
    @Nullable
    private IAzureMessager output;

    public Phase(@Nonnull final PhaseConfig config, @Nonnull Process parent) {
        this.process = parent;
        this.id = UUID.randomUUID().toString();
        this.status = Status.INITIAL;
        this.title = config.getTitle();
        this.type = config.getType();
        this.description = config.getDescription();
        this.steps = config.getSteps().stream().map(stepConfig -> new Step(stepConfig, this)).collect(Collectors.toList());
    }

    public void prepareLaunch() {
        this.setStatus(Status.READY);
    }

    // toso: -> getInputs
    public List<InputComponent> getInputs() {
        return steps.stream().map(Step::getInput).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public void setStatus(final Status status) {
        this.status = status;
        this.listenerList.forEach(listener -> listener.accept(status));
    }

    public void execute() {
        execute(process.getContext());
    }

    public void execute(final Context context) {
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(AzureString.format("Running phase : %s", this.getTitle()), () -> {
            final IAzureMessager currentMessager = AzureMessager.getMessager();
            OperationContext.current().setMessager(output);
            setStatus(Status.RUNNING);
            try {
                for (Step step : steps) {
                    step.execute(context);
                }
                setStatus(Status.SUCCEED);
            } catch (Exception e) {
                setStatus(Status.FAILED);
                AzureMessager.getMessager().error(e);
            } finally {
                OperationContext.current().setMessager(currentMessager);
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
