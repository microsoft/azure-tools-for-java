/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.legacy.function.runner.core;

import com.intellij.openapi.externalSystem.model.project.ExternalProjectPojo;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionMethod;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionProject;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.plugins.gradle.GradleManager;
import org.jetbrains.plugins.gradle.model.ExternalLibraryDependency;
import org.jetbrains.plugins.gradle.model.ExternalProject;
import org.jetbrains.plugins.gradle.model.ExternalProjectDependency;
import org.jetbrains.plugins.gradle.model.ExternalSourceSet;
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache;
import org.jetbrains.plugins.gradle.service.task.GradleTaskManager;
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class IntellijGradleFunctionProject extends FunctionProject {
    private final Project workspace;
    private final ExternalProject externalProject;
    @Getter
    private final boolean isValid;

    public IntellijGradleFunctionProject(@Nonnull Project workspace, @Nonnull Module module) {
        this.workspace = workspace;
        final String externalProjectPath = ExternalSystemApiUtil.getExternalProjectPath(module);

        externalProject = externalProjectPath != null ? ExternalProjectDataCache.getInstance(workspace).getRootExternalProject(externalProjectPath) : null;
        isValid = externalProject != null;
        init();
    }

    public IntellijGradleFunctionProject(@Nonnull Project workspace, @Nonnull ExternalProjectPojo project) {
        this.workspace = workspace;
        externalProject = ExternalProjectDataCache.getInstance(workspace).getRootExternalProject(project.getPath());
        isValid = externalProject != null;
        init();
    }

    public void packageJar() {
        //TODO(andxu) this method will be removed after we add logic to add before run task for function run configuraiotn
        final GradleManager manager = (GradleManager) ExternalSystemApiUtil.getManager(GradleConstants.SYSTEM_ID);
        final GradleTaskManager gradleTaskManager = new GradleTaskManager();
        final ExternalSystemTaskId externalSystemTaskId = ExternalSystemTaskId.create(GradleConstants.SYSTEM_ID, ExternalSystemTaskType.EXECUTE_TASK, workspace);
        final GradleExecutionSettings settings = manager.getExecutionSettingsProvider().fun(Pair.create(workspace, externalProject.getProjectDir().toString()));
        final IAzureMessager messager = AzureMessager.getMessager();
        gradleTaskManager.executeTasks(externalSystemTaskId, List.of("jar"),
            externalProject.getProjectDir().toString(), settings, null, new ExternalSystemTaskNotificationListenerAdapter() {
                public void onTaskOutput(ExternalSystemTaskId id, String text, boolean stdOut) {
                    if (StringUtils.isNotBlank(text)) {
                        for (String line : text.split("\\r?\\n")) {
                            messager.info(line);
                        }
                    }
                }
            }
        );
    }

    private void init() {
        if (!isValid) {
            return;
        }

        this.setName(externalProject.getName());
        if (!externalProject.getArtifacts().isEmpty()) {
            this.setArtifactFile(externalProject.getArtifacts().get(0));
        }
        this.setBaseDirectory(externalProject.getProjectDir());

        final ExternalSourceSet main = externalProject.getSourceSets().get("main");
        final List<File> dependencies = new ArrayList<>();
        if (main != null) {
            main.getDependencies().forEach(t -> {
                if (t instanceof ExternalLibraryDependency) {
                    dependencies.add(((ExternalLibraryDependency) t).getFile());
                }
                if (t instanceof ExternalProjectDependency) {
                    final ExternalProject childProject = this.externalProject.getChildProjects().get(t.getName());
                    if (childProject != null) {
                        dependencies.addAll(childProject.getArtifacts());
                    }
                }

            });
        }
        setDependencies(dependencies);
    }

    @Override
    public List<FunctionMethod> findAnnotatedMethods() {
        throw new UnsupportedOperationException("findAnnotatedMethods in intellij is not supported by now.");
    }

    @Override
    public void installExtension(String funcPath) {
        throw new UnsupportedOperationException("installExtension in intellij is not supported by now.");
    }
}
