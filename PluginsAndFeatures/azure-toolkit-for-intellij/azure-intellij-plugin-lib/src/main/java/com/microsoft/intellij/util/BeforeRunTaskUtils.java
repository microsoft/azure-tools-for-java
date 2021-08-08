/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.util;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper;
import com.intellij.openapi.externalSystem.model.project.ExternalProjectPojo;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTask;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTask;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import com.intellij.ui.CollectionListModel;
import com.intellij.util.containers.ContainerUtil;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTask;
import org.jetbrains.plugins.gradle.execution.GradleBeforeRunTaskProvider;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.annotation.Nonnull;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BeforeRunTaskUtils {
    private static final String GRADLE_TASK_ASSEMBLE = "assemble";
    private static final String MAVEN_TASK_PACKAGE = "package";

    public static void addBeforeRunTask(@Nonnull ConfigurationSettingsEditorWrapper editor, @Nonnull AzureArtifact artifact, @Nonnull RunConfiguration config) {
        final List<? extends BeforeRunTask<?>> tasks = getBuildTasks(editor, artifact);
        final BeforeRunTask<?> task = createBuildTask(artifact, config);
        if(Objects.nonNull(task)){ // task is null if artifact is File type.
            addTask(editor, tasks, task, config);
        }
    }

    public static void removeBeforeRunTask(@Nonnull ConfigurationSettingsEditorWrapper editor, @Nonnull AzureArtifact artifact) {
        final List<? extends BeforeRunTask<?>> tasks = getBuildTasks(editor, artifact);
        removeTasks(editor, tasks);
    }

    public static List<? extends BeforeRunTask<?>> getBuildTasks(@Nonnull ConfigurationSettingsEditorWrapper editor, @Nonnull AzureArtifact artifact) {
        switch (artifact.getType()) {
            case Maven:
                return getMavenPackageTasks(editor, (MavenProject) artifact.getReferencedObject());
            case Gradle:
                return getGradleAssembleTasks(editor, (ExternalProjectPojo) artifact.getReferencedObject());
            case Artifact:
                return getIntellijBuildTasks(editor, (Artifact) artifact.getReferencedObject());
            case File:
                return Collections.emptyList();
        }
        throw new AzureToolkitRuntimeException("unsupported project/artifact type");
    }

    public static @Nullable BeforeRunTask<?> createBuildTask(@Nonnull AzureArtifact artifact, @Nonnull RunConfiguration config) {
        switch (artifact.getType()) {
            case Maven:
                return createMavenPackageTask((MavenProject) artifact.getReferencedObject(), config);
            case Gradle:
                return createGradleAssembleTask((ExternalProjectPojo) artifact.getReferencedObject(), config);
            case Artifact:
                return createIntellijBuildTask((Artifact) artifact.getReferencedObject(), config);
            case File:
                return null;
        }
        throw new AzureToolkitRuntimeException("unsupported project/artifact type");
    }

    @NotNull
    public static List<BeforeRunTask<?>> getIntellijBuildTasks(@NotNull ConfigurationSettingsEditorWrapper editor, @NotNull Artifact artifact) {
        return ContainerUtil.findAll(editor.getStepsBeforeLaunch(), BuildArtifactsBeforeRunTask.class).stream()
            .filter(task -> Objects.nonNull(task) && Objects.nonNull(task.getArtifactPointers())
                && task.getArtifactPointers().size() == 1
                && Objects.equals(task.getArtifactPointers().get(0).getArtifact(), artifact))
            .collect(Collectors.toList());
    }

    @NotNull
    public static List<BeforeRunTask<?>> getMavenPackageTasks(@NotNull ConfigurationSettingsEditorWrapper editor, @Nonnull MavenProject project) {
        final String pomXmlPath = MavenUtils.getMavenModulePath(project);
        return ContainerUtil.findAll(editor.getStepsBeforeLaunch(), MavenBeforeRunTask.class).stream()
            .filter(task -> Objects.nonNull(task) && Objects.nonNull(task.getProjectPath()) && Objects.nonNull(pomXmlPath)
                && Paths.get(task.getProjectPath()).equals(Paths.get(pomXmlPath))
                && StringUtils.equals(MAVEN_TASK_PACKAGE, task.getGoal()))
            .collect(Collectors.toList());
    }

    @NotNull
    public static List<BeforeRunTask<?>> getGradleAssembleTasks(@NotNull ConfigurationSettingsEditorWrapper editor, @NotNull ExternalProjectPojo project) {
        return ContainerUtil.findAll(editor.getStepsBeforeLaunch(), ExternalSystemBeforeRunTask.class).stream()
            .filter(task -> Objects.nonNull(task)
                && StringUtils.equals(task.getTaskExecutionSettings().getExternalProjectPath(), project.getPath())
                && CollectionUtils.isEqualCollection(task.getTaskExecutionSettings().getTaskNames(),
                Collections.singletonList(GRADLE_TASK_ASSEMBLE)))
            .collect(Collectors.toList());
    }

    @NotNull
    public static BeforeRunTask<?> createIntellijBuildTask(@NotNull Artifact artifact, @NotNull RunConfiguration config) {
        final BuildArtifactsBeforeRunTaskProvider provider = new BuildArtifactsBeforeRunTaskProvider(config.getProject());
        final BuildArtifactsBeforeRunTask task = provider.createTask(config);
        task.addArtifact(artifact);
        return task;
    }

    @NotNull
    public static BeforeRunTask<?> createMavenPackageTask(@Nonnull MavenProject project, @NotNull RunConfiguration config) {
        final String pomXmlPath = MavenUtils.getMavenModulePath(project);
        final MavenBeforeRunTask task = new MavenBeforeRunTask();
        task.setEnabled(true);
        task.setProjectPath(pomXmlPath);
        task.setGoal(MAVEN_TASK_PACKAGE);
        return task;
    }

    @NotNull
    public static BeforeRunTask<?> createGradleAssembleTask(@NotNull ExternalProjectPojo project, @NotNull RunConfiguration config) {
        final GradleBeforeRunTaskProvider provider = new GradleBeforeRunTaskProvider(config.getProject());
        final ExternalSystemBeforeRunTask task = provider.createTask(config);
        task.getTaskExecutionSettings().setExternalSystemIdString(GradleConstants.SYSTEM_ID.toString());
        task.getTaskExecutionSettings().setExternalProjectPath(project.getPath());
        task.getTaskExecutionSettings().setTaskNames(Collections.singletonList(GRADLE_TASK_ASSEMBLE));
        return task;
    }

    @SneakyThrows
    private static synchronized <T extends BeforeRunTask<?>> void removeTasks(@Nonnull ConfigurationSettingsEditorWrapper editor, List<T> tasks) {
        // there is no way of removing tasks, use reflection
        final Object myBeforeRunStepsPanelField = FieldUtils.readField(editor, "myBeforeRunStepsPanel", true);
        final CollectionListModel<T> model = (CollectionListModel<T>) FieldUtils.readField(myBeforeRunStepsPanelField, "myModel", true);
        for (final T t : tasks) {
            t.setEnabled(false);
            model.remove(t);
        }
    }

    private static synchronized <T extends BeforeRunTask<?>> void addTask(
        @Nonnull ConfigurationSettingsEditorWrapper editor,
        List<? extends T> tasks, T task, RunConfiguration config
    ) {
        if (tasks.isEmpty()) {
            task.setEnabled(true);
            final RunManagerEx manager = RunManagerEx.getInstanceEx(config.getProject());
            final List<BeforeRunTask> tasksFromConfig = new ArrayList<>(manager.getBeforeRunTasks(config));
            // need to add the before run task back to runConfiguration since for the create scenario:
            // the before run task editor will reset tasks in runConfiguration, that's the reason why
            // here we need to add the task here
            tasksFromConfig.add(task);
            manager.setBeforeRunTasks(config, tasksFromConfig);
            editor.addBeforeLaunchStep(task);
        } else {
            for (final T t : tasks) {
                t.setEnabled(true);
            }
        }
    }
}
