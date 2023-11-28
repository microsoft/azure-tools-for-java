/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guidance;

import com.intellij.openapi.Disposable;
import com.microsoft.azure.toolkit.ide.guidance.config.PhaseConfig;
import com.microsoft.azure.toolkit.ide.guidance.input.GuidanceInput;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
@Setter
@RequiredArgsConstructor
public class Phase implements Disposable {
    @Nonnull
    private final Course course;

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
    private Status status = Status.INITIAL;

    @ToString.Exclude
    private Step currentStep;

    @Nullable
    private IAzureMessager output;
    private List<Consumer<Status>> listenerList = new CopyOnWriteArrayList<>();

    private boolean autoExecute = false;

    public Phase(@Nonnull final PhaseConfig config, @Nonnull Course parent) {
        this.course = parent;
        this.id = UUID.randomUUID().toString();
        this.title = config.getTitle();
        this.type = config.getType();
        this.description = config.getDescription();
        this.steps = config.getSteps().stream().map(stepConfig -> new Step(stepConfig, this)).collect(Collectors.toList());
        initStepListener();
    }

    private void initStepListener() {
        this.steps.forEach(step -> step.addStatusListener(this::handleStepStatus));
    }

    private void handleStepStatus(final Status status) {
        if (status == Status.RUNNING) {
            this.setStatus(status);
        } else if (status == Status.FAILED) {
            this.autoExecute = false;
            this.setStatus(status);
        } else if (status == Status.SUCCEED || status == Status.PARTIAL_SUCCEED) {
            this.currentStep = getFollowingStep(); // update current step
            if (this.currentStep == null) {
                final boolean isPartialSucceed = this.steps.stream().anyMatch(step -> step.getStatus() == Status.PARTIAL_SUCCEED);
                this.setStatus(isPartialSucceed ? Status.PARTIAL_SUCCEED : Status.SUCCEED);
                return;
            }
            this.currentStep.prepare();
            if (isAutoExecute()) {
                currentStep.execute();
            }
        }
    }

    public void prepare() {
        this.setStatus(Status.READY);
        if (CollectionUtils.isNotEmpty(steps)) {
            currentStep = steps.get(0);
            currentStep.prepare();
        } else {
            this.setStatus(Status.SUCCEED);
        }
    }

    public void setStatus(final Status status) {
        if (this.status != status) {
            this.status = status;
            this.listenerList.forEach(listener -> AzureTaskManager.getInstance().runOnPooledThread(() -> listener.accept(status)));
        }
    }

    public boolean validateInputs() {
        final AzureValidationInfo azureValidationInfo = getInputs().stream().map(input -> input.getAllValidationInfos(true))
            .flatMap(List::stream)
            .filter(info -> !info.isValid()).findFirst().orElse(null);
        if (azureValidationInfo != null) {
            final IAzureMessager messager = Optional.ofNullable(output).orElseGet(AzureMessager::getMessager);
            messager.warning(azureValidationInfo.getMessage());
        }
        return azureValidationInfo == null;
    }

    public void setOutput(@Nullable IAzureMessager output) {
        this.output = output;
        this.steps.forEach(step -> step.setOutput(output));
    }

    public List<GuidanceInput<?>> getInputs() {
        return this.steps.stream().flatMap(step -> step.getInputs().stream()).collect(Collectors.toList());
    }

    public void execute(final boolean autoExecute) {
        this.autoExecute = autoExecute;
        execute();
    }

    public void execute() {
        setStatus(Status.RUNNING);
        AzureTaskManager.getInstance().runInBackground("Validating inputs", this::validateInputs).thenAccept(result -> {
            if (result) {
                final Step currentStep = getCurrentStep();
                if (currentStep != null) {
                    currentStep.execute();
                }
            } else {
                setStatus(Status.FAILED);
            }
        });
    }

    public Context getContext() {
        return this.getCourse().getContext();
    }

    public String getRenderedDescription() {
        return getContext().render(this.getDescription());
    }

    public String getRenderedTitle() {
        return getContext().render(this.getTitle());
    }

    @Nullable
    private synchronized Step getFollowingStep() {
        final Step currentStep = getCurrentStep();
        if (currentStep == null) {
            return null;
        }
        for (int i = 0; i < steps.size() - 1; i++) {
            if (StringUtils.equals(currentStep.getId(), steps.get(i).getId())) {
                return steps.get(i + 1);
            }
        }
        return null;
    }

    public void addStatusListener(Consumer<Status> listener) {
        listenerList.add(listener);
    }

    public void removeStatusListener(Consumer<Status> listener) {
        listenerList.remove(listener);
    }

    @Override
    public void dispose() {
        this.getSteps().forEach(Step::dispose);
    }

    public void expandPhasePanel() {
        AzureEventBus.emit("guidance.phase.expand", this);
    }
}
