/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guideline;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.guideline.config.ProcessConfig;
import com.microsoft.azure.toolkit.ide.guideline.phase.GitClonePhase;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@RequiredArgsConstructor
public class Process {
    private String id;
    private Status status;
    private String name;
    private String title;
    private String description;
    private String repository;
    private String uri;
    private List<Phase> phases;

    private Project project;
    private Context context;

    public Process(@Nonnull final ProcessConfig processConfig, @Nonnull Project project) {
        this.id = UUID.randomUUID().toString();
        this.project = project;
        this.status = Status.INITIAL;
        this.name = processConfig.getName();
        this.title = processConfig.getTitle();
        this.description = processConfig.getDescription();
        this.repository = processConfig.getRepository();
        this.context = new Context();
        this.uri = processConfig.getUri();
        createPhases(processConfig);
    }

    private void createPhases(@Nonnull final ProcessConfig processConfig) {
        this.phases = processConfig.getPhases().stream().map(config -> new Phase(config, this)).collect(Collectors.toList());
        this.phases.add(0, new GitClonePhase(this));
        for (int i = 0; i < phases.size(); i++) {
            phases.get(i).setPrevious(i > 0 ? phases.get(i - 1) : null);
            phases.get(i).setFollowing(i < phases.size() - 1 ? phases.get(i + 1) : null);
        }
    }
}
