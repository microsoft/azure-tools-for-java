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

import com.intellij.openapi.externalSystem.model.project.ExternalProjectPojo;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.plugins.gradle.GradleManager;
import org.jetbrains.plugins.gradle.model.ExternalProject;
import org.jetbrains.plugins.gradle.service.project.data.ExternalProjectDataCache;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.*;

public class GradleUtils {
    public static List<ExternalProjectPojo> listGradleProjects(Project project) {
        GradleManager manager = (GradleManager) ExternalSystemApiUtil.getManager(GradleConstants.SYSTEM_ID);
        Map<ExternalProjectPojo, Collection<ExternalProjectPojo>> projects =
                manager.getLocalSettingsProvider().fun(project).getAvailableProjects();
        return new ArrayList(projects.keySet());
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
