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

package com.microsoft.azure.springcloud.dependency.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azuretools.ijidea.utility.AzureMavenDependencyActionBase;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.maven.DependencyArtifact;
import com.microsoft.intellij.maven.SpringCloudDependencyManager;
import com.microsoft.intellij.util.MavenUtils;
import com.microsoft.intellij.util.PluginUtil;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.DocumentException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.utils.MavenProcessCanceledException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddSpringCloudMavenDependencyAction extends AzureMavenDependencyActionBase {
    public static final String SPRING_CLOUD_GROUP_ID = "org.springframework.cloud";
    public static final String SPRING_BOOT_GROUP_ID = "org.springframework.boot";
    private static final String GROUP_ID = "com.microsoft.azure";
    private static final String ARTIFACT_ID = "spring-cloud-starter-azure-spring-cloud-client";
    private static final String SPRING_CLOUD_COMMONS_KEY = "org.springframework.cloud:spring-cloud-commons";

    @Override
    public boolean onActionPerformed(@NotNull AnActionEvent event, @Nullable Operation operation) {
        final Module module = event.getData(LangDataKeys.MODULE);
        final Project project = module.getProject();
        final MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(project);
        final MavenProject mavenProject = projectsManager.findProject(module);
        if (mavenProject == null) {
            PluginUtil.showErrorNotificationProject(project, "Error",
                    String.format("Project '%s' is not a maven project.",
                            project.getName()));
            return true;
        }

        AzureTaskManager.getInstance().runInBackground(new AzureTask(project, "Update Azure Spring Cloud dependencies", false, () -> {
            ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
            progressIndicator.setText("Syncing maven project " + project.getName());
            if (!syncMavenDependency(projectsManager, project, mavenProject)) {
                return;
            }
            try {
                progressIndicator.setText("Check existing dependencies");
                final String evaluateEffectivePom = MavenUtils.evaluateEffectivePom(project, mavenProject);
                ProgressManager.checkCanceled();
                if (StringUtils.isEmpty(evaluateEffectivePom)) {
                    PluginUtil.showErrorNotificationProject(project, "Error", "Failed to evaluate effective pom.");
                    return;
                }
                final String springBootVer = getMavenLibraryVersion(mavenProject, SPRING_BOOT_GROUP_ID, "spring-boot-autoconfigure");
                if (StringUtils.isEmpty(springBootVer)) {
                    throw new AzureExecutionException(String.format("Module %s is not a spring-boot application.", module.getName()));
                }
                progressIndicator.setText("Get latest versions ...");
                SpringCloudDependencyManager dependencyManager = new SpringCloudDependencyManager(evaluateEffectivePom);
                Map<String, DependencyArtifact> versionMaps = dependencyManager.getDependencyVersions();
                Map<String, DependencyArtifact> managerDependencyVersionsMaps = dependencyManager.getDependencyManagementVersions();

                // given the spring-cloud-commons is greater or equal to 2.2.5.RELEASE, we should not add spring-cloud-starter-azure-spring-cloud-client
                // because the code is already merged into spring repo: https://github.com/spring-cloud/spring-cloud-commons/pull/803
                boolean noAzureSpringCloudClientDependency = shouldNotAddAzureSpringCloudClientDependency(versionMaps) ||
                    shouldNotAddAzureSpringCloudClientDependency(managerDependencyVersionsMaps);

                ProgressManager.checkCanceled();
                final List<DependencyArtifact> versionChanges = calculateVersionChanges(springBootVer, noAzureSpringCloudClientDependency, versionMaps);
                if (versionChanges.isEmpty()) {
                    PluginUtil.showInfoNotificationProject(project, "Your project is update-to-date.",
                                                           "No updates are needed.");
                    return;
                }
                progressIndicator.setText("Applying versions ...");
                final File pomFile = new File(mavenProject.getFile().getCanonicalPath());
                ProgressManager.checkCanceled();

                versionChanges.stream().filter(change -> managerDependencyVersionsMaps.containsKey(change.getKey())).forEach(change -> {
                    String managementVersion = managerDependencyVersionsMaps.get(change.getKey()).getCurrentVersion();
                    if (SpringCloudDependencyManager.isCompatibleVersion(managementVersion, springBootVer)) {
                        change.setCompatibleVersion("");
                        change.setManagementVersion(managementVersion);
                    }
                });

                if (applyVersionChanges(dependencyManager, pomFile, managerDependencyVersionsMaps, versionChanges)) {
                    noticeUserVersionChanges(project, pomFile, versionChanges);
                } else {
                    PluginUtil.showInfoNotificationProject(project, "Your project is update-to-date.", "No updates are needed.");
                }
            } catch (DocumentException | IOException | AzureExecutionException | MavenProcessCanceledException e) {
                PluginUtil.showErrorNotification("Error",
                                                 "Failed to update Azure Spring Cloud dependencies due to error: "
                                                     + e.getMessage());
            }
        }));

        return false;
    }

    @Override
    protected String getServiceName(AnActionEvent event) {
        return TelemetryConstants.SPRING_CLOUD;
    }

    @Override
    protected String getOperationName(AnActionEvent event) {
        return TelemetryConstants.ADD_DEPENDENCY_SPRING_CLOUD_APP;
    }

    private static boolean shouldNotAddAzureSpringCloudClientDependency(Map<String, DependencyArtifact> versionMaps) {
        if (versionMaps.containsKey(SPRING_CLOUD_COMMONS_KEY)) {
            String version = versionMaps.get(SPRING_CLOUD_COMMONS_KEY).getCurrentVersion();
            return SpringCloudDependencyManager.isGreaterOrEqualVersion(version, "2.2.5.RELEASE");
        }
        return false;
    }

    private static List<DependencyArtifact> calculateVersionChanges(String springBootVer,
                                                                    boolean noAzureSpringCloudClientDependency,
                                                                    Map<String, DependencyArtifact> versionMaps)
            throws AzureExecutionException, DocumentException, IOException {
        List<DependencyArtifact> dep = new ArrayList<>();
        if (!noAzureSpringCloudClientDependency) {
            dep.add(getDependencyArtifact(GROUP_ID, ARTIFACT_ID, versionMaps));
        }
        dep.add(getDependencyArtifact(SPRING_BOOT_GROUP_ID, "spring-boot-starter-actuator", versionMaps));
        dep.add(getDependencyArtifact(SPRING_CLOUD_GROUP_ID, "spring-cloud-config-client", versionMaps));
        dep.add(getDependencyArtifact(SPRING_CLOUD_GROUP_ID,
                "spring-cloud-starter-netflix-eureka-client",
                versionMaps));
        dep.add(getDependencyArtifact(SPRING_CLOUD_GROUP_ID, "spring-cloud-starter-zipkin", versionMaps));
        dep.add(getDependencyArtifact(SPRING_CLOUD_GROUP_ID, "spring-cloud-starter-sleuth", versionMaps));

        return SpringCloudDependencyManager.getCompatibleVersions(dep, springBootVer);
    }
}
