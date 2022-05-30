/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.guideline;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.guideline.config.ProcessConfig;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.net.MalformedURLException;
import java.net.URL;
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
    private URL repository;
    private List<Phase> phases;

    private Project project;
    private Context context;

    public Process (@Nonnull final ProcessConfig processConfig){
        this.id = UUID.randomUUID().toString();
        this.status = Status.INITIAL;
        this.name = processConfig.getName();
        this.title = processConfig.getTitle();
        this.description = processConfig.getDescription();
        try {
            this.repository = new URL(processConfig.getRepository());
        } catch (MalformedURLException e) {
            // test
        }
        this.context = new Context();
        this.phases = processConfig.getPhases().stream().map(config -> new Phase(config, this)).collect(Collectors.toList());
    }
}
