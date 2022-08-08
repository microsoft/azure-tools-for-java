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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GradleUtils {

    public static List<ExternalProjectPojo> listGradleProjects(Project project) {
        GradleManager manager = (GradleManager) ExternalSystemApiUtil.getManager(GradleConstants.SYSTEM_ID);
        Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> projects =
                manager.getLocalSettingsProvider().fun(project).getAvailableProjects();
        return new ArrayList(projects.keySet());
    }
    public static boolean isGradleProject(Project project) {
        return CollectionUtils.isNotEmpty(listGradleProjects(project));
    }

    public static String getTargetFile(Project project, ExternalProjectPojo gradleProjectPojo) {
        ExternalProject externalProject = ExternalProjectDataCache.getInstance(project).getRootExternalProject(gradleProjectPojo.getPath());
        if (Objects.isNull(externalProject)) {
            return null;
        }
        if (externalProject.getArtifacts().isEmpty() || Objects.isNull(externalProject.getArtifacts().get(0))) {
            return null;
        }

        return externalProject.getArtifacts().get(0).getAbsolutePath();
    }
}
