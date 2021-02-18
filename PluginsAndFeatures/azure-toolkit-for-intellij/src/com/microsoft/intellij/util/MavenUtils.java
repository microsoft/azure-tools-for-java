/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.util;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.springcloud.dependency.SpringCloudDependencyManager;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.DocumentException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenExplicitProfiles;
import org.jetbrains.idea.maven.project.*;
import org.jetbrains.idea.maven.server.MavenEmbedderWrapper;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;

import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class MavenUtils {
    private static final String SPRING_BOOT_MAVEN_PLUGIN = "spring-boot-maven"
            + "-plugin";
    private static final String MAVEN_PROJECT_NOT_FOUND = "Cannot find maven project at folder: %s";

    public static boolean isMavenProject(Project project) {
        return MavenProjectsManager.getInstance(project).isMavenizedProject();
    }

    public static List<MavenProject> getMavenProjects(Project project) {
        return MavenProjectsManager.getInstance(project).getRootProjects();
    }

    public static String getTargetFile(@NotNull MavenProject mavenProject) {
        return Paths.get(mavenProject.getBuildDirectory(),
                         mavenProject.getFinalName() + "." + mavenProject.getPackaging()).toString();
    }

    public static String getSpringBootFinalJarFilePath(@NotNull Project ideaProject,
                                                       @NotNull MavenProject mavenProject) {
        String finalName = null;
        try {
            String xml = evaluateEffectivePom(ideaProject, mavenProject);
            if (StringUtils.isNotEmpty(xml) && xml.contains(SPRING_BOOT_MAVEN_PLUGIN)) {
                SpringCloudDependencyManager manager = new SpringCloudDependencyManager(xml);
                finalName = manager.getPluginConfiguration("org.springframework.boot",
                                                           SPRING_BOOT_MAVEN_PLUGIN, "finalName");

            }
        } catch (MavenProcessCanceledException | DocumentException ex) {
            // ignore
        }
        if (StringUtils.isEmpty(finalName)) {
            finalName = mavenProject.getFinalName();
        }
        return Paths.get(mavenProject.getBuildDirectory(), finalName + "." + mavenProject.getPackaging()).toString();
    }

    public static String evaluateEffectivePom(@NotNull Project ideaProject,
                                              @NotNull MavenProject mavenProject) throws MavenProcessCanceledException {
        final MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(ideaProject);

        MavenEmbeddersManager embeddersManager = projectsManager.getEmbeddersManager();
        MavenExplicitProfiles profiles = mavenProject.getActivatedProfilesIds();
        MavenEmbedderWrapper embedder = embeddersManager.getEmbedder(mavenProject,
                                                                     MavenEmbeddersManager.FOR_DEPENDENCIES_RESOLVE);
        embedder.clearCachesFor(mavenProject.getMavenId());
        return embedder.evaluateEffectivePom(mavenProject.getFile(),
                                             profiles.getEnabledProfiles(),
                                             profiles.getDisabledProfiles());
    }

    public static String getMavenModulePath(MavenProject mavenProject) {
        return Objects.isNull(mavenProject) ? null : mavenProject.getFile().getPath();
    }
}
