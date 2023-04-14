/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.buildimage;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.util.containers.ContainerUtil;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.microsoft.intellij.util.BuildArtifactBeforeRunTaskUtils.addTask;
import static com.microsoft.intellij.util.BuildArtifactBeforeRunTaskUtils.removeTasks;

public class DockerBuildTaskUtils {

    public static void updateDockerBuildBeforeRunTasks(@Nonnull final DataContext context, @Nonnull RunConfiguration configuration, @Nullable final DockerImage value) {
        final ConfigurationSettingsEditorWrapper editor = ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY.getData(context);
        if (Objects.isNull(editor)) {
            return;
        }
        final Boolean needDockerBuildBeforeRunTask = Optional.ofNullable(value).map(DockerImage::isDraft).orElse(false);
        if (needDockerBuildBeforeRunTask) {
            DockerBuildTaskUtils.addBeforeRunTask(editor, configuration);
        } else {
            DockerBuildTaskUtils.removeBeforeRunTask(editor, configuration);
        }
    }

    public static void addBeforeRunTask(@Nonnull ConfigurationSettingsEditorWrapper editor, @Nonnull RunConfiguration config) {
        final List<BeforeRunTask<?>> tasks = findDockerBuildTasks(editor);
        final DockerBuildTaskProvider.DockerBuildBeforeRunTask task = new DockerBuildTaskProvider().createTask(config);
        if (tasks.size() <= 1) {
            addTask(editor, tasks, task, config);
        } else {
            // in case there are more than one docker build tasks, remove all of them and add a new one
            removeTasks(editor, tasks);
            addTask(editor, null, task, config);
        }
    }

    public static void removeBeforeRunTask(@Nonnull ConfigurationSettingsEditorWrapper editor, @Nonnull RunConfiguration config) {
        final List<? extends BeforeRunTask<?>> tasks = findDockerBuildTasks(editor);
        if (CollectionUtils.isNotEmpty(tasks)) {
            removeTasks(editor, tasks);
        }
    }

    @Nonnull
    public static List<BeforeRunTask<?>> findDockerBuildTasks(@Nonnull ConfigurationSettingsEditorWrapper editor) {
        return ContainerUtil.findAll(editor.getStepsBeforeLaunch(), DockerBuildTaskProvider.DockerBuildBeforeRunTask.class).stream().collect(Collectors.toList());
    }
}
