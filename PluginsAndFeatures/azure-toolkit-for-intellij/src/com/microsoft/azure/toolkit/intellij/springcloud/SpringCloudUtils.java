/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppPlatformManager;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.springcloud.dependency.SpringCloudDependencyManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.intellij.util.PluginUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class SpringCloudUtils {
    private static final String[] SPRING_ARTIFACTS = {
            "spring-boot-starter-actuator",
            "spring-cloud-config-client",
            "spring-cloud-starter-netflix-eureka-client",
            "spring-cloud-starter-zipkin",
            "spring-cloud-starter-sleuth"
    };
    private static final String JAR = "jar";
    private static final String MAIN_CLASS = "Main-Class";
    private static final String SPRING_BOOT_LIB = "Spring-Boot-Lib";
    private static final String SPRING_BOOT_AUTOCONFIGURE = "spring-boot-autoconfigure";
    private static final String NOT_SPRING_BOOT_Artifact = "Artifact %s is not a spring-boot artifact.";
    private static final String DEPENDENCIES_IS_NOT_UPDATED = "Azure Spring Cloud dependencies are not updated.";
    private static final String MAIN_CLASS_NOT_FOUND =
            "Main class cannot be found in %s, which is required for spring cloud app.";
    private static final String AZURE_DEPENDENCIES_WARNING_TITLE =
            "Azure dependencies are missing or incompatible";
    private static final String DEPENDENCY_WARNING = "Azure dependencies are missing or incompatible, you "
            + "may update the dependencies by Azure -> Add Azure Spring Cloud dependency on project context menu.\n";

    @NotNull
    public static File getArtifactFile(@NotNull final String artifactId, final Project project) throws AzureExecutionException, IOException {
        if (StringUtils.isEmpty(artifactId)) {
            throw new AzureExecutionException("You must specify an artifact");
        }
        final AzureArtifact artifact = AzureArtifactManager.getInstance(project).getAzureArtifactById(artifactId);
        if (Objects.isNull(artifact)) {
            throw new AzureExecutionException(String.format("The artifact '%s' you selected doesn't exists", artifactId));
        }
        return getArtifactFile(artifact, project);
    }

    @NotNull
    public static File getArtifactFile(@NotNull final AzureArtifact artifact, final Project project) throws AzureExecutionException, IOException {
        final String path = AzureArtifactManager.getInstance(project).getFileForDeployment(artifact);
        if (!Files.exists(Paths.get(path))) {
            throw new AzureExecutionException(String.format("File '%s' cannot be found.", path));
        }
        validateSpringCloudAppArtifact(path, project);
        return new File(path);
    }

    private static void validateSpringCloudAppArtifact(String finalJar, final Project project) throws AzureExecutionException, IOException {
        final JarFile jarFile = new JarFile(finalJar);
        final Attributes manifestAttributes = jarFile.getManifest().getMainAttributes();
        final String mainClass = manifestAttributes.getValue(MAIN_CLASS);
        if (StringUtils.isEmpty(mainClass)) {
            throw new AzureExecutionException(String.format(MAIN_CLASS_NOT_FOUND, finalJar));
        }
        final String library = manifestAttributes.getValue(SPRING_BOOT_LIB);
        if (StringUtils.isEmpty(library)) {
            return;
        }
        final Map<String, String> dependencies = getSpringAppDependencies(jarFile.entries(), library);
        if (!dependencies.containsKey(SPRING_BOOT_AUTOCONFIGURE)) {
            throw new AzureExecutionException(String.format(NOT_SPRING_BOOT_Artifact, finalJar));
        }
        final String springVersion = dependencies.get(SPRING_BOOT_AUTOCONFIGURE);
        final List<String> missingDependencies = new ArrayList<>();
        final Map<String, String> inCompatibleDependencies = new HashMap<>();
        for (final String artifact : SPRING_ARTIFACTS) {
            if (!dependencies.containsKey(artifact)) {
                missingDependencies.add(artifact);
            } else if (!SpringCloudDependencyManager.isCompatibleVersion(dependencies.get(artifact), springVersion)) {
                inCompatibleDependencies.put(artifact, dependencies.get(artifact));
            }
        }
        final String dependencyPrompt = getDependenciesValidationPrompt(
                missingDependencies, inCompatibleDependencies, springVersion);
        if (!inCompatibleDependencies.isEmpty()) {
            PluginUtil.showWarningNotificationProject(project, AZURE_DEPENDENCIES_WARNING_TITLE, dependencyPrompt);
        } else if (!missingDependencies.isEmpty()) {
            PluginUtil.showInfoNotificationProject(project, AZURE_DEPENDENCIES_WARNING_TITLE, dependencyPrompt);
        }
    }

    private static String getDependenciesValidationPrompt(List<String> missingDependencies,
                                                          Map<String, String> inCompatibleDependencies, String springVersion) {
        final StringBuilder result = new StringBuilder();
        result.append(DEPENDENCY_WARNING);
        for (final String dependency : missingDependencies) {
            result.append(String.format("%s : Missing \n", dependency));
        }
        for (final String dependency : inCompatibleDependencies.keySet()) {
            result.append(String.format("%s : Incompatible, current version %s, spring boot version %s \n",
                    dependency, inCompatibleDependencies.get(dependency), springVersion));
        }
        return result.toString();
    }

    private static Map<String, String> getSpringAppDependencies(Enumeration<JarEntry> jarEntryEnumeration,
                                                                String libraryPath) {
        final String[] springArtifacts = ArrayUtils.add(SPRING_ARTIFACTS, SPRING_BOOT_AUTOCONFIGURE);
        final List<JarEntry> jarEntries = Collections.list(jarEntryEnumeration);
        return jarEntries.stream()
                .filter(jarEntry -> StringUtils.startsWith(jarEntry.getName(), libraryPath)
                        && StringUtils.equalsIgnoreCase(FilenameUtils.getExtension(jarEntry.getName()), JAR))
                .map(jarEntry -> {
                    final String fileName = FilenameUtils.getBaseName(jarEntry.getName());
                    final int i = StringUtils.lastIndexOf(fileName, "-");
                    return (i > 0 && i < fileName.length() - 1) ?
                            new String[]{
                                    StringUtils.substring(fileName, 0, i),
                                    StringUtils.substring(fileName, i + 1)
                            } :
                            new String[]{fileName, ""};
                })
                .filter(entry -> ArrayUtils.contains(springArtifacts, entry[0]))
                .collect(Collectors.toMap(entry -> entry[0], entry -> entry[1]));
    }

    public static AppPlatformManager getSpringManager(String sid) {
        return AuthMethodManager.getInstance().getAzureSpringCloudClient(sid);
    }
}
