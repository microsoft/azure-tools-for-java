/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.util;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.externalSystem.model.project.ExternalProjectPojo;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemBeforeRunTask;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTask;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import com.intellij.ui.CollectionListModel;
import com.intellij.util.Producer;
import com.intellij.util.containers.ContainerUtil;
import com.microsoft.intellij.ui.components.AzureArtifact;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.tasks.MavenBeforeRunTask;
import org.jetbrains.plugins.gradle.execution.GradleBeforeRunTaskProvider;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BeforeRunTaskUtils {
    private static final String GRADLE_TASK_ASSEMBLE = "assemble";
    private static final String MAVEN_TASK_PACKAGE = "package";

    public static void addOrRemoveBeforeRunTask(JComponent runConfigurationEditorComponent,
                                                 AzureArtifact azureArtifact,
                                                 RunConfiguration configuration,
                                                 final boolean add) throws IllegalAccessException {
        switch (azureArtifact.getType()) {
            case Maven:
                addOrRemoveBuildMavenProjectBeforeRunOption(runConfigurationEditorComponent,
                                                                               (MavenProject) azureArtifact.getReferencedObject(),
                                                                               configuration, add);
                break;
            case Gradle:
                addOrRemoveBuildGradleProjectBeforeRunOption(runConfigurationEditorComponent,
                                                                                (ExternalProjectPojo) azureArtifact.getReferencedObject(),
                                                                                configuration, add);
                break;
            case Artifact:
                addOrRemoveBuildArtifactBeforeRunOption(runConfigurationEditorComponent,
                                                                           (Artifact) azureArtifact.getReferencedObject(),
                                                                           configuration, add);
                break;
            default:
                // do nothing

        }
    }

    public static void addOrRemoveBuildArtifactBeforeRunOption(@NotNull JComponent runConfigurationEditorComponent,
                                                               @NotNull Artifact artifact,
                                                               RunConfiguration runConfiguration,
                                                               boolean add) throws IllegalAccessException {
        addOrRemoveBeforeRunOption(runConfigurationEditorComponent, BuildArtifactsBeforeRunTask.class,
            task ->
                  Objects.nonNull(task) &&
                          Objects.nonNull(task.getArtifactPointers())
                          && task.getArtifactPointers().size() == 1
                          && Objects.equals(task.getArtifactPointers().get(0).getArtifact(),
                                                               artifact), () -> {
                BuildArtifactsBeforeRunTaskProvider provider =
                      new BuildArtifactsBeforeRunTaskProvider(runConfiguration.getProject());
                BuildArtifactsBeforeRunTask task = provider.createTask(runConfiguration);
                task.addArtifact(artifact);
                return task;
            }, runConfiguration, add);
    }

    public static void addOrRemoveBuildMavenProjectBeforeRunOption(@NotNull JComponent runConfigurationEditorComponent,
                                                                   @NotNull MavenProject mavenProject,
                                                                   RunConfiguration runConfiguration,
                                                                   boolean add) throws IllegalAccessException {
        String pomXmlPath = MavenUtils.getMavenModulePath(mavenProject);
        addOrRemoveBeforeRunOption(runConfigurationEditorComponent, MavenBeforeRunTask.class, task ->
                                      Objects.nonNull(task) && StringUtils.equals(task.getProjectPath(), pomXmlPath)
                                              && StringUtils.equals(MAVEN_TASK_PACKAGE, task.getGoal()),
            () -> {
                MavenBeforeRunTask task = new MavenBeforeRunTask();
                task.setEnabled(true);
                task.setProjectPath(pomXmlPath);
                task.setGoal(MAVEN_TASK_PACKAGE);
                return task;
            }, runConfiguration, add);
    }

    public static void addOrRemoveBuildGradleProjectBeforeRunOption(@NotNull JComponent runConfigurationEditorComponent,
                                                                    @NotNull ExternalProjectPojo gradleProject,
                                                                    RunConfiguration runConfiguration, boolean add)
            throws IllegalAccessException {
        addOrRemoveBeforeRunOption(runConfigurationEditorComponent, ExternalSystemBeforeRunTask.class, task ->
                                      Objects.nonNull(task) &&
                                              Objects.nonNull(task.getTaskExecutionSettings())
                                              && StringUtils.equals(task.getTaskExecutionSettings()
                                                                        .getExternalProjectPath(),
                                                                    gradleProject.getPath())
                                              && CollectionUtils.isEqualCollection(task.getTaskExecutionSettings().getTaskNames(),
                                                                                   Arrays.asList(GRADLE_TASK_ASSEMBLE)),
            () -> {
                GradleBeforeRunTaskProvider provider = new GradleBeforeRunTaskProvider(
                        runConfiguration.getProject());
                ExternalSystemBeforeRunTask task = provider.createTask(runConfiguration);
                task.getTaskExecutionSettings()
                    .setExternalSystemIdString(GradleConstants.SYSTEM_ID.toString());
                task.getTaskExecutionSettings().setExternalProjectPath(gradleProject.getPath());
                task.getTaskExecutionSettings().setTaskNames(Arrays.asList(GRADLE_TASK_ASSEMBLE));
                return task;
            }, runConfiguration, add);
    }

    public static synchronized <T extends BeforeRunTask> void addOrRemoveBeforeRunOption(
            @NotNull JComponent runConfigurationEditorComponent,
            @NotNull Class<T> runTaskClass,
            @NotNull Predicate<T> filter,
            Producer<T> producer,
            RunConfiguration runConfiguration,
            boolean add) throws IllegalAccessException {
        DataContext dataContext = DataManager.getInstance().getDataContext(runConfigurationEditorComponent);
        ConfigurationSettingsEditorWrapper editor = ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY.getData(
                dataContext);
        if (editor != null) {
            List<T> matchedTasks = ContainerUtil.findAll(editor.getStepsBeforeLaunch(), runTaskClass)
                                         .stream()
                                         .filter(filter)
                                         .collect(Collectors.toList());
            if (add && matchedTasks.isEmpty()) {
                T task = producer.produce();
                task.setEnabled(true);
                RunManagerEx manager = RunManagerEx.getInstanceEx(runConfiguration.getProject());
                List<BeforeRunTask> tasksFromConfig = new ArrayList<>(manager.getBeforeRunTasks(runConfiguration));
                // need to add the before run task back to runConfiguration since for the create scenario:
                // the before run task editor will reset tasks in runConfiguration, that's the reason why
                // here we need to add the task here
                tasksFromConfig.add(task);
                manager.setBeforeRunTasks(runConfiguration, tasksFromConfig);
                editor.addBeforeLaunchStep(task);
            } else {
                if (add) {
                    for (T task : matchedTasks) {
                        task.setEnabled(true);
                    }
                } else {
                    // there is no way of removing tasks, use reflection
                    Object myBeforeRunStepsPanelField = FieldUtils.readField(editor, "myBeforeRunStepsPanel", true);
                    CollectionListModel model = (CollectionListModel) FieldUtils.readField(myBeforeRunStepsPanelField,
                                                                                           "myModel",
                                                                                           true);
                    if (model != null) {
                        for (T task : matchedTasks) {
                            task.setEnabled(false);
                            model.remove(task);
                        }
                    } else {
                        for (T task : matchedTasks) {
                            task.setEnabled(false);
                        }
                    }
                }
            }
        }
    }
}
