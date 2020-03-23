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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import com.microsoft.azuretools.securestore.SecureStore;
import com.microsoft.azuretools.service.ServiceManager;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import rx.Observable;
import rx.schedulers.Schedulers;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AzureSettingPanel<T extends AzureRunConfigurationBase> {
    protected final Project project;
    private boolean isCbArtifactInited;
    private boolean isArtifact;
    private boolean telemetrySent;
    private Artifact lastSelectedArtifact;
    protected SecureStore secureStore;

    public AzureSettingPanel(@NotNull Project project) {
        this.project = project;
        this.isCbArtifactInited = false;
        this.secureStore = ServiceManager.getServiceProvider(SecureStore.class);
    }

    public void reset(@NotNull T configuration) {
        if (!isMavenProject()) {
            List<Artifact> artifacts = MavenRunTaskUtil.collectProjectArtifact(project);
            setupArtifactCombo(artifacts, configuration.getTargetPath());
        } else {
            List<MavenProject> mavenProjects = MavenProjectsManager.getInstance(project).getProjects();
            setupMavenProjectCombo(mavenProjects, configuration.getTargetPath());
        }

        resetFromConfig(configuration);
        sendTelemetry(configuration.getSubscriptionId(), configuration.getTargetName());
    }

    protected void savePassword(String serviceName, String userName, String password) {
        if (StringUtils.isEmpty(serviceName) || StringUtils.isEmpty(userName) || StringUtils.isEmpty(password)) {
            return;
        }
        this.secureStore.savePassword(serviceName, userName, password);
    }

    protected String loadPassword(String serviceName, String userName) {
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
        return MavenRunTaskUtil.isMavenProject(project);
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

    protected void artifactActionPeformed(Artifact selectArtifact) {
        JPanel pnlRoot = getMainPanel();
        if (!Comparing.equal(lastSelectedArtifact, selectArtifact)) {
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
    protected abstract JLabel getLblMavenProject();

    private void setupArtifactCombo(List<Artifact> artifacts, String targetPath) {
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
}
