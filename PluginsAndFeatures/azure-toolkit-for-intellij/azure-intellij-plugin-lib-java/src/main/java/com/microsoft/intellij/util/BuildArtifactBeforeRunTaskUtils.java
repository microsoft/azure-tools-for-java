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
import com.intellij.util.containers.ContainerUtil;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.runconfig.IWebAppRunConfiguration;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTask;
import org.jetbrains.plugins.gradle.execution.GradleBeforeRunTaskProvider;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BuildArtifactBeforeRunTaskUtils {
    private static final String GRADLE_TASK_ASSEMBLE = "assemble";
    private static final String GRADLE_TASL_SKIP_TESTS = "-x test";
    private static final String MAVEN_TASK_PACKAGE = "package";
    private static final String MAVEN_TASK_PACKAGE_SKIP_TESTS = "package -DskipTests";

    public static void addBeforeRunTask(
        @Nonnull ConfigurationSettingsEditorWrapper editor,
        @Nonnull AzureArtifact artifact,
        @Nonnull RunConfiguration config) {
        final List<? extends BeforeRunTask<?>> tasks = getBuildTasks(editor, artifact);
        final BeforeRunTask<?> task = createBuildTask(artifact, config);
        if (Objects.nonNull(task)) { // task is null if artifact is File type.
            addTask(editor, tasks, task, config);
        }
        updateConnectorBeforeRunTask(config, editor);
    }

    public static void removeBeforeRunTask(
        @Nonnull ConfigurationSettingsEditorWrapper editor,
        @Nonnull AzureArtifact artifact,
        @Nonnull RunConfiguration config) {
        final List<BeforeRunTask<?>> tasks = getBuildTasks(editor, artifact);
        removeTasks(editor, tasks, config);
    }

    public static void updateConnectorBeforeRunTask(@Nonnull RunConfiguration config, @Nonnull ConfigurationSettingsEditorWrapper editor) {
        config.getProject().getMessageBus()
            .syncPublisher(IWebAppRunConfiguration.MODULE_CHANGED)
            .artifactMayChanged(config, editor);
    }

    public static List<BeforeRunTask<?>> getBuildTasks(@Nonnull ConfigurationSettingsEditorWrapper editor, @Nonnull AzureArtifact artifact) {
        switch (artifact.getType()) {
            case Maven:
                return getMavenPackageTasks(editor, (MavenProject) artifact.getReferencedObject());
            case Gradle:
                return getGradleAssembleTasks(editor, (ExternalProjectPojo) artifact.getReferencedObject());
            case Artifact:
                return getIntellijBuildTasks(editor, (Artifact) artifact.getReferencedObject());
            case File:
                return Collections.emptyList();
            default:
                throw new AzureToolkitRuntimeException("unsupported project/artifact type");
        }
    }

    public static @Nullable BeforeRunTask<?> createBuildTask(@Nonnull AzureArtifact artifact, @Nonnull RunConfiguration config) {
        return createBuildTask(artifact, config, false);
    }

    public static @Nullable BeforeRunTask<?> createBuildTask(@Nonnull AzureArtifact artifact, @Nonnull RunConfiguration config, boolean skipTests) {
        switch (artifact.getType()) {
            case Maven:
                return createMavenPackageTask((MavenProject) artifact.getReferencedObject(), config, skipTests);
            case Gradle:
                return createGradleAssembleTask((ExternalProjectPojo) artifact.getReferencedObject(), config, skipTests);
            case Artifact:
                return createIntellijBuildTask((Artifact) artifact.getReferencedObject(), config);
            case File:
                return null;
            default:
                throw new AzureToolkitRuntimeException("unsupported project/artifact type");
        }
    }

    @Nonnull
    public static List<BeforeRunTask<?>> getIntellijBuildTasks(@Nonnull ConfigurationSettingsEditorWrapper editor, @Nonnull Artifact artifact) {
        return ContainerUtil.findAll(editor.getStepsBeforeLaunch(), BuildArtifactsBeforeRunTask.class).stream()
            .filter(task -> Objects.nonNull(task) && Objects.nonNull(task.getArtifactPointers())
                && task.getArtifactPointers().size() == 1
                && Objects.equals(task.getArtifactPointers().get(0).getArtifact(), artifact))
            .collect(Collectors.toList());
    }

    @Nonnull
    public static List<BeforeRunTask<?>> getMavenPackageTasks(@Nonnull ConfigurationSettingsEditorWrapper editor, @Nonnull MavenProject project) {
        final String pomXmlPath = MavenUtils.getMavenModulePath(project);
        return ContainerUtil.findAll(editor.getStepsBeforeLaunch(), MavenBeforeRunTask.class).stream()
            .filter(task -> Objects.nonNull(task) && Objects.nonNull(task.getProjectPath()) && Objects.nonNull(pomXmlPath)
                && Paths.get(task.getProjectPath()).equals(Paths.get(pomXmlPath))
                && StringUtils.equals(MAVEN_TASK_PACKAGE, task.getGoal()))
            .collect(Collectors.toList());
    }

    @Nonnull
    public static List<BeforeRunTask<?>> getGradleAssembleTasks(@Nonnull ConfigurationSettingsEditorWrapper editor, @Nonnull ExternalProjectPojo project) {
        return ContainerUtil.findAll(editor.getStepsBeforeLaunch(), ExternalSystemBeforeRunTask.class).stream()
            .filter(task -> Objects.nonNull(task)
                && StringUtils.equals(task.getTaskExecutionSettings().getExternalProjectPath(), project.getPath())
                && CollectionUtils.isEqualCollection(task.getTaskExecutionSettings().getTaskNames(),
                Collections.singletonList(GRADLE_TASK_ASSEMBLE)))
            .collect(Collectors.toList());
    }

    @Nullable
    public static BeforeRunTask<?> createIntellijBuildTask(@Nonnull Artifact artifact, @Nonnull RunConfiguration config) {
        final BuildArtifactsBeforeRunTaskProvider provider = new BuildArtifactsBeforeRunTaskProvider(config.getProject());
        final BuildArtifactsBeforeRunTask task = provider.createTask(config);
        Optional.ofNullable(task).ifPresent(t -> t.addArtifact(artifact));
        return task;
    }

    @Nonnull
    public static BeforeRunTask<?> createMavenPackageTask(@Nonnull MavenProject project, @Nonnull RunConfiguration config, boolean skipTests) {
        final String pomXmlPath = MavenUtils.getMavenModulePath(project);
        final MavenBeforeRunTask task = new MavenBeforeRunTask();
        task.setEnabled(true);
        task.setProjectPath(pomXmlPath);
        task.setGoal(skipTests ? MAVEN_TASK_PACKAGE_SKIP_TESTS : MAVEN_TASK_PACKAGE);
        return task;
    }

    @Nullable
    public static BeforeRunTask<?> createGradleAssembleTask(@Nonnull ExternalProjectPojo project, @Nonnull RunConfiguration config, boolean skipTests) {
        final GradleBeforeRunTaskProvider provider = new GradleBeforeRunTaskProvider(config.getProject());
        final ExternalSystemBeforeRunTask task = provider.createTask(config);
        if (Objects.isNull(task)) {
            return null;
        }
        task.getTaskExecutionSettings().setExternalSystemIdString(GradleConstants.SYSTEM_ID.toString());
        task.getTaskExecutionSettings().setExternalProjectPath(project.getPath());
        task.getTaskExecutionSettings().setTaskNames(Collections.singletonList(GRADLE_TASK_ASSEMBLE));
        if (skipTests) {
            task.getTaskExecutionSettings().setScriptParameters(GRADLE_TASL_SKIP_TESTS);
        }
        return task;
    }

    @SneakyThrows
    public static synchronized void removeTasks(@Nonnull ConfigurationSettingsEditorWrapper editor,
                                                @Nonnull Predicate<BeforeRunTask<?>> cond, @Nonnull RunConfiguration config) {
        final List<BeforeRunTask<?>> tasks = editor.getStepsBeforeLaunch().stream().filter(cond).collect(Collectors.toList());
        removeTasks(editor, tasks, config);
    }

    @SneakyThrows
    public static synchronized void removeTasks(@Nonnull ConfigurationSettingsEditorWrapper editor,
                                                @Nonnull List<BeforeRunTask<?>> tasks, @Nonnull RunConfiguration config) {
        final List<BeforeRunTask<?>> stepsBeforeLaunch = editor.getStepsBeforeLaunch();
        final List<BeforeRunTask<?>> remainTasks = ListUtils.removeAll(stepsBeforeLaunch, tasks);
        tasks.forEach(t -> t.setEnabled(false));
        editor.replaceBeforeLaunchSteps(remainTasks);
        RunManagerEx.getInstanceEx(config.getProject()).setBeforeRunTasks(config, new ArrayList<>(remainTasks));
    }

    public static synchronized <T extends BeforeRunTask<?>> void addTask(
        @Nonnull ConfigurationSettingsEditorWrapper editor,
        List<? extends T> tasks, T task, RunConfiguration config
    ) {
        if (CollectionUtils.isEmpty(tasks)) {
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
