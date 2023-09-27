/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.util;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenExplicitProfiles;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.model.MavenPlugin;
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
    private static final String SPRING_BOOT_MAVEN_PLUGIN = "spring-boot-maven-plugin";
    private static final String MAVEN_PROJECT_NOT_FOUND = "Cannot find maven project at folder: %s";
    public static final String WARNING = "Customize artifact name in `spring-boot-maven-plugin` configuration part is no longer supported, please use the standard `finalName`. see https://docs.spring.io/spring-boot/docs/current/maven-plugin/reference/htmlsingle/#packaging.examples.custom-name";

    public static boolean isMavenProject(Project project) {
        return MavenProjectsManager.getInstance(project).isMavenizedProject();
    }

    @AzureOperation("boundary/common.list_maven_projects")
    public static List<MavenProject> getMavenProjects(Project project) {
        return MavenProjectsManager.getInstance(project).getProjects();
    }

    public static String getTargetFile(@NotNull MavenProject mavenProject) {
        return Paths.get(mavenProject.getBuildDirectory(),
                         mavenProject.getFinalName() + "." + mavenProject.getPackaging()).toString();
    }

    public static String getSpringBootFinalJarFilePath(@NotNull Project ideaProject, @NotNull MavenProject mavenProject) {
        final String finalName = mavenProject.getFinalName();
        // https://docs.spring.io/spring-boot/docs/current/maven-plugin/reference/htmlsingle/#packaging.examples.custom-name
        // customize artifact name in `spring-boot-maven-plugin` configuration is no longer recommended.
        final String springPluginFinaleName = mavenProject.getPlugins().stream().filter(p -> p.getArtifactId().equals(SPRING_BOOT_MAVEN_PLUGIN)).findFirst()
            .map(MavenPlugin::getConfigurationElement)
            .map(e -> e.getChild("finalName"))
            .map(org.jdom.Element::getTextTrim)
            .orElse(null);
        return Paths.get(mavenProject.getBuildDirectory(), StringUtils.firstNonBlank(springPluginFinaleName, finalName) + "." + mavenProject.getPackaging()).toString();
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
    private static String getPluginConfiguration(String effectivePomXml, String groupId, String artifactId, String configurationName) throws DocumentException {
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

    @Nullable
    public static MavenProject getRootMavenProject(final Project project, final MavenProject mavenProject) {
        if (mavenProject == null) {
            return null;
        }
        MavenProject result = mavenProject;
        MavenId parentId = mavenProject.getParentId();
        while (parentId != null) {
            result = getMavenProjectById(project, parentId);
            if (result == null) {
                return null;
            }
            parentId = result.getParentId();
        }
        return result;
    }

    @Nullable
    public static MavenProject getMavenProjectById(final Project project, final MavenId mavenId) {
        return MavenProjectsManager.getInstance(project).getProjects().stream()
                .filter(pro -> Objects.equals(pro.getMavenId(), mavenId)).findFirst().orElse(null);
    }

    public static MavenProject getMavenProjectByDirectory(final Project project, final String directory) {
        return MavenProjectsManager.getInstance(project).getProjects().stream()
                .filter(pro -> StringUtils.equals(pro.getDirectory(), directory)).findFirst().orElse(null);
    }

}
