/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.util;

import com.intellij.openapi.externalSystem.model.project.ExternalProjectPojo;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.Project;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.plugins.gradle.GradleManager;
import org.jetbrains.plugins.gradle.model.ExternalProject;
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;

public class GradleUtils {

    public static List<ExternalProjectPojo> listGradleRootProjectPojo(Project project) {
        final GradleManager gradleManager = (GradleManager) ExternalSystemApiUtil.getManager(GradleConstants.SYSTEM_ID);
        final Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> projects =
                Optional.ofNullable(gradleManager).map(manager ->
                        manager.getLocalSettingsProvider().fun(project).getAvailableProjects()).orElse(Collections.emptyMap());
        return new ArrayList<>(projects.keySet());
    }

    public static List<ExternalProject> listGradleRootProjects(Project project) {
        return listGradleRootProjectPojo(project).stream().map(pojo -> ExternalProjectDataCache.getInstance(project).getRootExternalProject(pojo.getPath()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static List<ExternalProject> listGradleProjects(Project project) {
        final List<ExternalProject> result = new ArrayList<>();
        final Queue<ExternalProject> queue = new ArrayDeque<>(listGradleRootProjects(project));
        while (!queue.isEmpty()) {
            final ExternalProject externalProject = queue.poll();
            result.add(externalProject);
            queue.addAll(externalProject.getChildProjects().values());
        }
        return result;
    }

    public static boolean isGradleProject(Project project) {
        return CollectionUtils.isNotEmpty(listGradleRootProjectPojo(project));
    }

    @Nullable
    public static String getTargetFile(Project project, ExternalProjectPojo gradleProjectPojo) {
        final ExternalProject externalProject = ExternalProjectDataCache.getInstance(project).getRootExternalProject(gradleProjectPojo.getPath());
        if (Objects.isNull(externalProject)) {
            return null;
        }
        if (externalProject.getArtifacts().isEmpty() || Objects.isNull(externalProject.getArtifacts().get(0))) {
            return null;
        }

        return externalProject.getArtifacts().get(0).getAbsolutePath();
    }
}
