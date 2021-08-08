/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.util;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenExplicitProfiles;
import org.jetbrains.idea.maven.project.MavenEmbeddersManager;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.server.MavenEmbedderWrapper;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;

import javax.annotation.Nullable;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        String finalName = mavenProject.getFinalName();
        try {
            final String xml = evaluateEffectivePom(ideaProject, mavenProject);
            if (StringUtils.isNotEmpty(xml) && xml.contains(SPRING_BOOT_MAVEN_PLUGIN)) {
                finalName = StringUtils.firstNonBlank(
                        getPluginConfiguration(xml, "org.springframework.boot", SPRING_BOOT_MAVEN_PLUGIN, "finalName"), finalName);
            }
        } catch (final Exception ex) {
            AzureMessager.getMessager().warning(String.format("Can not evaluate effective pom, fall back to final jar %s", finalName));
        }
        return Paths.get(mavenProject.getBuildDirectory(), finalName + "." + mavenProject.getPackaging()).toString();
    }

    public static String evaluateEffectivePom(@NotNull Project ideaProject,
                                              @NotNull MavenProject mavenProject) throws MavenProcessCanceledException {
        final MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(ideaProject);

        final MavenEmbeddersManager embeddersManager = projectsManager.getEmbeddersManager();
        final MavenExplicitProfiles profiles = mavenProject.getActivatedProfilesIds();
        final MavenEmbedderWrapper embedder = embeddersManager.getEmbedder(mavenProject,
                                                                     MavenEmbeddersManager.FOR_DEPENDENCIES_RESOLVE);
        embedder.clearCachesFor(mavenProject.getMavenId());
        return embedder.evaluateEffectivePom(mavenProject.getFile(),
                                             profiles.getEnabledProfiles(),
                                             profiles.getDisabledProfiles());
    }

    public static String getMavenModulePath(MavenProject mavenProject) {
        return Objects.isNull(mavenProject) ? null : mavenProject.getFile().getPath();
    }

    public static final String POM_NAMESPACE = "http://maven.apache.org/POM/4.0.0";

    @Nullable
    public static String getPluginConfiguration(String effectivePomXml, String groupId, String artifactId, String configurationName) throws DocumentException {
        final Map<String, String> nsContext = new HashMap<>();
        nsContext.put("ns", POM_NAMESPACE);
        DocumentFactory.getInstance().setXPathNamespaceURIs(nsContext);
        final Document doc = DocumentHelper.parseText(effectivePomXml);
        for (final Node node : doc.selectNodes("//ns:project/ns:build/ns:plugins/ns:plugin")) {
            final String myGroupId = ((Element) node).elementTextTrim("groupId");
            final String myArtifactId = ((Element) node).elementTextTrim("artifactId");
            if (StringUtils.equals(groupId, myGroupId) && StringUtils.equals(artifactId, myArtifactId)) {
                final Element configurationNode = ((Element) node).element("configuration");
                return configurationNode == null ? null : configurationNode.elementTextTrim(configurationName);
            }
        }
        return null;
    }

}
