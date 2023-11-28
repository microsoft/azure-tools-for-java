/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guidance;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.guidance.config.CourseConfig;
import com.microsoft.azure.toolkit.ide.guidance.phase.PhaseManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Getter
@Setter
@RequiredArgsConstructor
public class Course implements Disposable {
    @Nonnull
    private final String id;
    @Nonnull
    private final String name;
    @Nonnull
    private final String title;
    @Nullable
    private final String description;
    @Nonnull
    private final String repository;
    @Nullable
    private final String uri;
    @Nullable
    private final String icon;
    @Nonnull
    private final Integer priority;
    @Nonnull
    private final List<Phase> phases;
    @Nonnull
    private final Project project;
    @Nonnull
    private final Context context;
    @Nonnull
    private Status status = Status.INITIAL;
    private List<BiConsumer<Phase, Phase>> phaseListeners = new CopyOnWriteArrayList<>();
    private Phase currentPhase;

    public Course(@Nonnull final CourseConfig courseConfig, @Nonnull Project project) {
        this.id = UUID.randomUUID().toString();
        this.project = project;
        this.name = courseConfig.getName();
        this.title = courseConfig.getTitle();
        this.description = courseConfig.getDescription();
        this.repository = courseConfig.getRepository();
        this.context = new Context(this, courseConfig.getContext());
        this.uri = courseConfig.getUri();
        this.icon = courseConfig.getIcon();
        this.priority = courseConfig.getPriority();
        this.phases = courseConfig.getPhases().stream().map(config -> PhaseManager.createPhase(config, this)).collect(Collectors.toList());
        this.initPhaseStatusListener();
    }

    public void prepare() {
        this.setStatus(Status.READY);
        if (CollectionUtils.isNotEmpty(phases)) {
            this.setCurrentPhase(phases.get(0));
            currentPhase.prepare();
        } else {
            this.setStatus(Status.SUCCEED);
        }
    }

    private void initPhaseStatusListener() {
        this.phases.forEach(step -> step.addStatusListener(status -> {
            if (status == Status.RUNNING || status == Status.FAILED) {
                this.setStatus(status);
            } else if (status == Status.SUCCEED || status == Status.PARTIAL_SUCCEED) {
                this.setCurrentPhase(getNextPhase(step));
                if (currentPhase == null) {
                    this.setStatus(Status.SUCCEED);
                } else {
                    currentPhase.prepare();
                }
            }
        }));
    }

    @Nullable
    private Phase getNextPhase(Phase step) {
        for (int i = 0; i < phases.size() - 1; i++) {
            if (StringUtils.equals(step.getId(), phases.get(i).getId())) {
                return phases.get(i + 1);
            }
        }
        return null;
    }

    public void setCurrentPhase(final Phase phase) {
        final Phase oldPhase = this.currentPhase;
        this.currentPhase = phase;
        this.phaseListeners.forEach(listener -> listener.accept(oldPhase, phase));
    }

    public void addPhaseListener(BiConsumer<Phase, Phase> listener) {
        phaseListeners.add(listener);
    }

    public void removePhaseListener(BiConsumer<Phase, Phase> listener) {
        phaseListeners.remove(listener);
    }

    @Override
    public void dispose() {
        this.getPhases().forEach(Phase::dispose);
    }
}
