/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guidance;

import com.intellij.openapi.Disposable;
import com.microsoft.azure.toolkit.ide.guidance.config.StepConfig;
import com.microsoft.azure.toolkit.ide.guidance.input.GuidanceInput;
import com.microsoft.azure.toolkit.ide.guidance.input.InputManager;
import com.microsoft.azure.toolkit.ide.guidance.task.TaskManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
@Setter
@RequiredArgsConstructor
public class Step implements Disposable {
    public static final int TIME_OUT_MINUTES = 10;
    public static final String TIME_OUT_WARNING = "Failed to execute step (%s) in (%d) minute, please continue after related task finished";
    @Nonnull
    private final String id;
    @Nonnull
    private final String title;

    @Nullable
    private final String description;

    @Nonnull
    private final Task task;

    @Nonnull
    private final List<? extends GuidanceInput<?>> inputs;

    @Nonnull
    @ToString.Exclude
    private final Phase phase;
    private final Integer timeout;
    private final boolean continueOnError;
    @Nonnull
    private Status status = Status.INITIAL;
    private IAzureMessager output;
    private List<Consumer<Status>> listenerList = new CopyOnWriteArrayList<>();

    public Step(@Nonnull final StepConfig config, @Nonnull Phase phase) {
        this.phase = phase;
        this.id = UUID.randomUUID().toString();
        this.title = config.getTitle();
        this.timeout = Optional.ofNullable(config.getTimeout()).orElse(StepConfig.DEFAULT_TIMEOUT_IN_MINUTES);
        this.description = config.getDescription();
        this.continueOnError = config.isContinueOnError();
        this.task = TaskManager.createTask(config.getTask(), phase.getCourse().getContext());
        this.inputs = Optional.ofNullable(config.getInputs())
            .map(configs -> configs.stream().map(inputConfig ->
                InputManager.createInputComponent(inputConfig, phase.getCourse().getContext())).collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }

    public void setStatus(final Status status) {
        this.status = status;
        this.listenerList.forEach(listener -> AzureTaskManager.getInstance().runOnPooledThread(() -> listener.accept(status)));
    }

    public void execute() {
        final String title = this.getTitle().replaceAll("<[^>]*>", "");
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(AzureString.format("run task '%s'", title), () -> {
            OperationContext.current().setMessager(output);
            try {
                setStatus(Status.RUNNING);
                if (!this.phase.validateInputs()) {
                    setStatus(Status.FAILED);
                    return;
                }
                this.applyInputs();
                final Boolean result = Mono.fromCallable(this::executeTask)
                        .subscribeOn(Schedulers.boundedElastic())
                        .timeout(Duration.ofMinutes(timeout), Mono.error(new AzureToolkitException(String.format(TIME_OUT_WARNING, this.title, timeout)))).block();
                setStatus(Status.SUCCEED);
            } catch (final Exception e) {
                setStatus(continueOnError ? Status.PARTIAL_SUCCEED : Status.FAILED);
                AzureMessager.getMessager().error(e);
                // Also show error notification
                if (!Objects.equals(AzureMessager.getMessager(), AzureMessager.getDefaultMessager())) {
                    AzureMessager.getDefaultMessager().error(e);
                }
            }
        }));
    }

    @AzureOperation(name = "internal/guidance.execute_task.step", params = "this.title")
    private boolean executeTask() throws Exception {
        if (this.task.isDone() || this.task.isToSkip()) {
            return true;
        }
        OperationContext.current().setMessager(output);
        this.task.execute();
        return true;
    }

    public String getRenderedDescription() {
        return getContext().render(this.getDescription());
    }

    public String getRenderedTitle() {
        return getContext().render(this.getTitle());
    }

    public Context getContext() {
        return this.getPhase().getContext();
    }

    public void addStatusListener(Consumer<Status> listener) {
        listenerList.add(listener);
    }

    public void removeStatusListener(Consumer<Status> listener) {
        listenerList.remove(listener);
    }

    public void prepare() {
        task.prepare();
        this.setStatus(task.isDone()|| task.isToSkip() ? Status.SUCCEED : Status.READY);
    }

    public boolean isReady() {
        return task.isReady();
    }

    private void applyInputs() {
        this.phase.getInputs().forEach(GuidanceInput::applyResult);
    }

    @Override
    public void dispose() {
        Optional.of(this.getTask()).ifPresent(Task::dispose);
    }
}
