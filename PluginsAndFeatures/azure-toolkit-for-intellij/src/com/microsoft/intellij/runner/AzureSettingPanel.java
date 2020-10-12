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

package com.microsoft.intellij.runner;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.externalSystem.model.project.ExternalProjectPojo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import com.intellij.ui.SimpleListCellRenderer;
import com.microsoft.azuretools.securestore.SecureStore;
import com.microsoft.azuretools.service.ServiceManager;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.runner.functions.core.FunctionUtils;
import com.microsoft.intellij.ui.components.AzureArtifact;
import com.microsoft.intellij.ui.components.AzureArtifactManager;
import com.microsoft.intellij.util.BeforeRunTaskUtils;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import com.microsoft.intellij.util.MavenUtils;
import com.microsoft.intellij.util.PluginUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.swing.*;
import java.nio.file.Paths;
import java.util.*;

public abstract class AzureSettingPanel<T extends AzureRunConfigurationBase> {
    private boolean legacyMode = true;
    protected final Project project;
    private boolean isCbArtifactInited;
    private boolean isArtifact;
    private boolean telemetrySent;
    private Artifact lastSelectedArtifact;
    private AzureArtifact lastSelectedAzureArtifact;
    protected SecureStore secureStore;

    public AzureSettingPanel(@NotNull Project project) {
        this.project = project;
        this.isCbArtifactInited = false;
        this.secureStore = ServiceManager.getServiceProvider(SecureStore.class);
    }

    public AzureSettingPanel(@NotNull Project project,
                             boolean legacyMode) {
        this(project);
        this.legacyMode = legacyMode;
    }

    public void reset(@NotNull T configuration) {
        // legacy initialize, will be removed later
        if (legacyMode) {
            if (configuration.isFirstTimeCreated()) {
                if (FunctionUtils.isFunctionProject(configuration.getProject())) {
                    // Todo: Add before run build job
                } else if (MavenUtils.isMavenProject(project)) {
                    MavenRunTaskUtil.addMavenPackageBeforeRunTask(configuration);
                } else {
                    final List<Artifact> artifacts = MavenRunTaskUtil.collectProjectArtifact(project);
                    if (artifacts.size() > 0) {
                        BuildArtifactsBeforeRunTaskProvider.setBuildArtifactBeforeRun(project, configuration, artifacts.get(0));
                    }
                }
            }
            configuration.setFirstTimeCreated(false);
            if (!isMavenProject()) {
                List<Artifact> artifacts = MavenRunTaskUtil.collectProjectArtifact(project);
                setupArtifactCombo(artifacts, configuration.getTargetPath());
            }  else {
                List<MavenProject> mavenProjects = MavenProjectsManager.getInstance(project).getProjects();
                setupMavenProjectCombo(mavenProjects, configuration.getTargetPath());
            }
        } else {
            setupAzureArtifactCombo(configuration.getArtifactIdentifier(), configuration);
        }

        resetFromConfig(configuration);
        sendTelemetry(configuration.getSubscriptionId(), configuration.getTargetName());
    }

    protected void savePassword(String serviceName,
                                String userName,
                                String password) {
        if (StringUtils.isEmpty(serviceName) || StringUtils.isEmpty(userName) || StringUtils.isEmpty(password)) {
            return;
        }
        this.secureStore.savePassword(serviceName, userName, password);
    }

    protected String loadPassword(String serviceName,
                                  String userName) {
        if (StringUtils.isEmpty(serviceName) || StringUtils.isEmpty(userName)) {
            return StringUtils.EMPTY;
        }
        try {
            return this.secureStore.loadPassword(serviceName, userName);
        } catch (java.lang.IllegalArgumentException e) {
            // Return empty string if no such password
            return StringUtils.EMPTY;
        }
    }

    protected boolean isMavenProject() {
        return MavenUtils.isMavenProject(project);
    }

    protected String getProjectBasePath() {
        return project.getBasePath();
    }

    protected String getTargetPath() {
        String targetPath = "";
        if (isArtifact && lastSelectedArtifact != null) {
            targetPath = lastSelectedArtifact.getOutputFilePath();
        } else {
            MavenProject mavenProject = (MavenProject) (getCbMavenProject().getSelectedItem());
            if (mavenProject != null) {
                targetPath = MavenRunTaskUtil.getTargetPath(mavenProject);
            }
        }
        return targetPath;
    }

    protected String getTargetName() {
        String targetName = "";
        if (isArtifact && lastSelectedArtifact != null) {
            String targetPath = lastSelectedArtifact.getOutputFilePath();
            targetName = Paths.get(targetPath).getFileName().toString();
        } else {
            MavenProject mavenProject = (MavenProject) (getCbMavenProject().getSelectedItem());
            if (mavenProject != null) {
                targetName = MavenRunTaskUtil.getTargetName(mavenProject);
            }
        }
        return targetName;
    }

    protected void artifactActionPerformed(Artifact selectArtifact) {
        if (!Comparing.equal(lastSelectedArtifact, selectArtifact)) {
            JPanel pnlRoot = getMainPanel();
            if (lastSelectedArtifact != null && isCbArtifactInited) {
                BuildArtifactsBeforeRunTaskProvider
                        .setBuildArtifactBeforeRunOption(pnlRoot, project, lastSelectedArtifact, false);
            }
            if (selectArtifact != null && isCbArtifactInited) {
                BuildArtifactsBeforeRunTaskProvider
                        .setBuildArtifactBeforeRunOption(pnlRoot, project, selectArtifact, true);
            }
            lastSelectedArtifact = selectArtifact;
        }
    }

    protected void syncBeforeRunTasks(AzureArtifact azureArtifact,
                                               final RunConfiguration configuration) {
        if (!Comparing.equal(lastSelectedAzureArtifact, azureArtifact)) {
            JPanel pnlRoot = getMainPanel();
            if (Objects.nonNull(lastSelectedAzureArtifact)) {
                try {
                    addOrRemoveBeforeRunTask(pnlRoot, lastSelectedAzureArtifact, configuration, false);
                } catch (IllegalAccessException e) {
                    PluginUtil.showErrorNotificationProject(configuration.getProject(), "Remove Before Run Task error",
                                                            e.getMessage());
                }
            }

            lastSelectedAzureArtifact = azureArtifact;
            if (Objects.nonNull(lastSelectedAzureArtifact)) {
                try {
                    addOrRemoveBeforeRunTask(pnlRoot, lastSelectedAzureArtifact, configuration, true);
                } catch (IllegalAccessException e) {
                    PluginUtil.showErrorNotificationProject(configuration.getProject(), "Add Before Run Task error",
                                                            e.getMessage());
                }
            }
        }
    }

    @NotNull
    public abstract String getPanelName();

    public abstract void disposeEditor();

    protected abstract void resetFromConfig(@NotNull T configuration);

    protected abstract void apply(@NotNull T configuration);

    @NotNull
    public abstract JPanel getMainPanel();

    @NotNull
    protected JLabel getLblArtifact() {
        return new JLabel();
    }

    @NotNull
    protected JComboBox<Artifact> getCbArtifact() {
        return new JComboBox<>();
    }

    @NotNull
    protected JComboBox<MavenProject> getCbMavenProject() {
        return new JComboBox<>();
    }

    @NotNull
    protected JLabel getLblMavenProject() {
        return new JLabel();
    }

    @NotNull
    protected JLabel getLblAzureArtifact() {
        return new JLabel();
    }

    @NotNull
    protected JComboBox<AzureArtifact> getCbAzureArtifact() {
        return new JComboBox<>();
    }

    protected void setupAzureArtifactCombo(String artifactIdentifier, RunConfiguration configuration) {
        List<AzureArtifact> azureArtifacts = AzureArtifactManager.getInstance(project).getAllSupportedAzureArtifacts();
        if (!azureArtifacts.isEmpty()) {
            for (AzureArtifact azureArtifact : azureArtifacts) {
                getCbAzureArtifact().addItem(azureArtifact);
                if (StringUtils.equals(AzureArtifactManager.getInstance(project).getArtifactIdentifier(azureArtifact)
                        , artifactIdentifier)) {
                    getCbAzureArtifact().setSelectedItem(azureArtifact);
                }
            }
        }

        getLblAzureArtifact().setVisible(true);
        getCbAzureArtifact().setVisible(true);

        getCbAzureArtifact().setRenderer(new SimpleListCellRenderer<AzureArtifact>() {
            @Override
            public void customize(JList list,
                                  AzureArtifact artifact,
                                  int index,
                                  boolean isSelected,
                                  boolean cellHasFocus) {
                if (Objects.nonNull(artifact)) {
                    setIcon(artifact.getIcon());
                    setText(artifact.getName());
                }
            }
        });
        getCbAzureArtifact().addActionListener(e -> {
            AzureArtifact artifact = (AzureArtifact) getCbAzureArtifact().getSelectedItem();
            syncBeforeRunTasks(artifact, configuration);
        });

        if (getCbAzureArtifact().getSelectedItem() != null) {
            syncBeforeRunTasks((AzureArtifact) getCbAzureArtifact().getSelectedItem() , configuration);
        }

        isCbArtifactInited = true;
    }

    private void setupArtifactCombo(List<Artifact> artifacts,
                                    String targetPath) {
        isCbArtifactInited = false;
        JComboBox<Artifact> cbArtifact = getCbArtifact();
        cbArtifact.removeAllItems();
        if (null != artifacts) {
            for (Artifact artifact : artifacts) {
                cbArtifact.addItem(artifact);
                if (Comparing.equal(artifact.getOutputFilePath(), targetPath)) {
                    cbArtifact.setSelectedItem(artifact);
                }
            }
        }
        cbArtifact.setVisible(true);
        getLblArtifact().setVisible(true);
        isArtifact = true;
        isCbArtifactInited = true;
    }

    private void setupMavenProjectCombo(List<MavenProject> mvnprjs,
                                        String targetPath) {
        JComboBox<MavenProject> cbMavenProject = getCbMavenProject();
        cbMavenProject.removeAllItems();
        if (null != mvnprjs) {
            for (MavenProject prj : mvnprjs) {
                cbMavenProject.addItem(prj);
                if (MavenRunTaskUtil.getTargetPath(prj).equals(targetPath)) {
                    cbMavenProject.setSelectedItem(prj);
                }
            }
        }
        cbMavenProject.setVisible(true);
        getLblMavenProject().setVisible(true);
    }

    private void sendTelemetry(String subId, String targetName) {
        if (telemetrySent) {
            return;
        }
        Observable.fromCallable(() -> {
            Map<String, String> map = new HashMap<>();
            map.put("SubscriptionId", subId != null ? subId : "");
            if (targetName != null) {
                map.put("FileType", MavenRunTaskUtil.getFileType(targetName));
            }
            AppInsightsClient.createByType(AppInsightsClient.EventType.Dialog,
                getPanelName(),
                "Open" /*action*/,
                map
            );
            return true;
        }).subscribeOn(Schedulers.io()).subscribe(
            (res) -> telemetrySent = true,
            (err) -> telemetrySent = true
        );
    }

    private static void addOrRemoveBeforeRunTask(JComponent runConfigurationEditorComponent,
                                                 AzureArtifact azureArtifact,
                                                 RunConfiguration configuration,
                                                 final boolean add) throws IllegalAccessException {
        switch (azureArtifact.getType()) {
            case Maven:
                BeforeRunTaskUtils.addOrRemoveBuildMavenProjectBeforeRunOption(runConfigurationEditorComponent,
                                                                               (MavenProject) azureArtifact.getReferencedObject(),
                                                                               configuration, add);
                break;
            case Gradle:
                BeforeRunTaskUtils.addOrRemoveBuildGradleProjectBeforeRunOption(runConfigurationEditorComponent,
                                                                                (ExternalProjectPojo) azureArtifact.getReferencedObject(),
                                                                                configuration, add);
                break;
            case Artifact:
                BeforeRunTaskUtils.addOrRemoveBuildArtifactBeforeRunOption(runConfigurationEditorComponent,
                                                                           (Artifact) azureArtifact.getReferencedObject(),
                                                                           configuration, add);
                break;
            default:
                // do nothing

        }
    }
}
