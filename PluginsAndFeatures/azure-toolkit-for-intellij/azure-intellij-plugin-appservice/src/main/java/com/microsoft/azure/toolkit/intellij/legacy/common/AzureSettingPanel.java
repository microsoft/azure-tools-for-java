/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.common;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.impl.ConfigurationSettingsEditorWrapper;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import com.intellij.ui.SimpleListCellRenderer;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.common.store.ISecureStore;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import com.microsoft.intellij.util.BuildArtifactBeforeRunTaskUtils;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import com.microsoft.intellij.util.MavenUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.swing.*;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class AzureSettingPanel<T extends AzureRunConfigurationBase> {

    private static final JLabel EMPTY_LABEL = new JLabel();
    private static final JComboBox EMPTY_COMBO_BOX = new JComboBox();

    private boolean oldMode = true;
    protected final Project project;
    private boolean isCbArtifactInited;
    private boolean isArtifact;
    private boolean telemetrySent;
    private Artifact lastSelectedArtifact;
    protected AzureArtifact currentArtifact;
    protected ISecureStore secureStore;

    public AzureSettingPanel(@NotNull Project project) {
        this.project = project;
        this.isCbArtifactInited = false;
        this.secureStore = AzureStoreManager.getInstance().getSecureStore();
    }

    public AzureSettingPanel(@NotNull Project project,
                             boolean oldMode) {
        this(project);
        this.oldMode = oldMode;
    }

    public void reset(@NotNull T configuration) {
        // legacy initialize, will be removed later
        if (oldMode) {
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
            } else {
                List<MavenProject> mavenProjects = MavenProjectsManager.getInstance(project).getProjects();
                setupMavenProjectCombo(mavenProjects, configuration.getTargetPath());
            }
        } else {
            setupAzureArtifactCombo(configuration.getArtifactIdentifier(), configuration);
        }
        this.currentArtifact = AzureArtifactManager.getInstance(project).getAzureArtifactById(configuration.getArtifactIdentifier());

        resetFromConfig(configuration);
        sendTelemetry(configuration.getSubscriptionId(), configuration.getTargetName());
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

    protected void syncBeforeRunTasks(AzureArtifact newArtifact, @NotNull final RunConfiguration configuration) {
        if (!AzureArtifactManager.getInstance(configuration.getProject()).equalsAzureArtifact(currentArtifact, newArtifact)) {
            final AzureArtifact oldArtifact = currentArtifact;
            currentArtifact = newArtifact;
            final JPanel pnlRoot = getMainPanel();
            final DataContext context = DataManager.getInstance().getDataContext(pnlRoot);
            final ConfigurationSettingsEditorWrapper editor = ConfigurationSettingsEditorWrapper.CONFIGURATION_EDITOR_KEY.getData(context);
            if (editor == null) {
                return;
            }
            if (Objects.nonNull(oldArtifact)) {
                BuildArtifactBeforeRunTaskUtils.removeBeforeRunTask(editor, oldArtifact, configuration);
            }
            if (Objects.nonNull(newArtifact)) {
                BuildArtifactBeforeRunTaskUtils.addBeforeRunTask(editor, newArtifact, configuration);
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
    protected abstract JComboBox<Artifact> getCbArtifact();

    @NotNull
    protected abstract JLabel getLblArtifact();

    @NotNull
    protected abstract JComboBox<MavenProject> getCbMavenProject();

    @NotNull
    protected JLabel getLblMavenProject() {
        return EMPTY_LABEL;
    }

    @NotNull
    protected JLabel getLblAzureArtifact() {
        return EMPTY_LABEL;
    }

    @NotNull
    protected JComboBox<AzureArtifact> getCbAzureArtifact() {
        return EMPTY_COMBO_BOX;
    }

    protected void setupAzureArtifactCombo(String artifactIdentifier, RunConfiguration configuration) {
        if (isCbArtifactInited || getCbAzureArtifact() == EMPTY_COMBO_BOX) {
            return;
        }
        List<AzureArtifact> azureArtifacts = AzureArtifactManager.getInstance(project).getAllSupportedAzureArtifacts();
        getCbAzureArtifact().removeAllItems();
        if (!azureArtifacts.isEmpty()) {
            for (AzureArtifact azureArtifact : azureArtifacts) {
                getCbAzureArtifact().addItem(azureArtifact);
                if (StringUtils.equals(AzureArtifactManager.getInstance(project).getArtifactIdentifier(azureArtifact)
                        , artifactIdentifier)) {
                    getCbAzureArtifact().setSelectedItem(azureArtifact);
                }
            }
            final AzureArtifact defaultArtifact = (AzureArtifact) getCbAzureArtifact().getSelectedItem();
            if (defaultArtifact != null) {
                syncBeforeRunTasks(defaultArtifact, configuration);
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

    private void setupMavenProjectCombo(List<MavenProject> mvnprjs, String targetPath) {
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
            map.put("eventType", "Dialog");
            map.put("panelName", getPanelName());
            map.put("operationName", "Open");
            AzureTelemeter.log(AzureTelemetry.Type.INFO, map);
            return true;
        }).subscribeOn(Schedulers.io()).subscribe(
            (res) -> telemetrySent = true,
            (err) -> telemetrySent = true
        );
    }
}
