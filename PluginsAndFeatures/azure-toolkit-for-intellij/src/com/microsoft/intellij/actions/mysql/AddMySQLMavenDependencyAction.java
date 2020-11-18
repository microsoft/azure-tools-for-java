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

package com.microsoft.intellij.actions.mysql;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.MavenDependencyUtils;
import com.microsoft.azuretools.ijidea.utility.AzureMavenDependencyActionBase;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.intellij.maven.DependencyArtifact;
import com.microsoft.intellij.maven.MavenDependencyManager;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AddMySQLMavenDependencyAction extends AzureMavenDependencyActionBase {
    private static final String GROUP_ID = "mysql";
    private static final String ARTIFACT_ID = "mysql-connector-java";

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

        AzureTaskManager.getInstance().runInBackground(new AzureTask(project,
                                                                     "Add MySQL dependency",
                                                                     false, () -> {
            ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
            progressIndicator.setText("Syncing maven project " + project.getName());
            if (!syncMavenDependency(projectsManager, project, mavenProject)) {
                return;
            }
            progressIndicator.setText("Check existing dependencies");
            try {
                final String evaluateEffectivePom = MavenUtils.evaluateEffectivePom(project, mavenProject);
                ProgressManager.checkCanceled();

                if (StringUtils.isEmpty(evaluateEffectivePom)) {
                    PluginUtil.showErrorNotificationProject(project, "Error", "Failed to evaluate effective pom.");
                    return;
                }
                progressIndicator.setText("Get latest versions ...");
                MavenDependencyManager dependencyManager = new MavenDependencyManager(evaluateEffectivePom);
                final Map<String, DependencyArtifact> versionMaps = dependencyManager.getDependencyVersions();
                final Map<String, DependencyArtifact> managerDependencyVersionsMaps = dependencyManager.getDependencyManagementVersions();
                ProgressManager.checkCanceled();
                final List<DependencyArtifact> versionChanges = calculateVersionChanges(versionMaps);
                if (versionChanges.isEmpty()) {
                    PluginUtil.showInfoNotificationProject(project, "Your project is update-to-date.",
                                                           "No updates are needed.");
                    return;
                }

                progressIndicator.setText("Applying versions ...");
                final File pomFile = new File(mavenProject.getFile().getCanonicalPath());
                ProgressManager.checkCanceled();
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

    private static List<DependencyArtifact> calculateVersionChanges(Map<String, DependencyArtifact> versionMaps)
            throws AzureExecutionException, DocumentException, IOException {
        DependencyArtifact dependency = getDependencyArtifact(GROUP_ID, ARTIFACT_ID, versionMaps);

        if (StringUtils.isBlank(dependency.getCurrentVersion())) {
            List<String> latestVersions = MavenDependencyUtils.getMavenCentralVersions(GROUP_ID, ARTIFACT_ID);
            if (latestVersions.isEmpty()) {
                PluginUtil.showErrorNotification("Error",
                                                 String.format("Artifact '%s:%s' cannot be found from maven central.", GROUP_ID, ARTIFACT_ID));
            }
            dependency.setCompatibleVersion(latestVersions.get(latestVersions.size() - 1));

        }
        return StringUtils.equals(dependency.getCurrentVersion(), dependency.getCompatibleVersion()) ? Collections.emptyList() :
               Arrays.asList(dependency);
    }

    @Override
    protected String getServiceName(AnActionEvent event) {
        return TelemetryConstants.MYSQL;
    }

    @Override
    protected String getOperationName(AnActionEvent event) {
        return TelemetryConstants.ADD_MY_SQL_DEPENDENCY;
    }

}
