/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp.docker.pushimage.ui;

import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.common.store.ISecureStore;
import com.microsoft.azure.toolkit.intellij.common.AzureSettingPanel;
import com.microsoft.azure.toolkit.intellij.webapp.docker.ContainerSettingPanel;
import com.microsoft.azure.toolkit.intellij.webapp.docker.pushimage.PushImageRunConfiguration;
import com.microsoft.azure.toolkit.intellij.webapp.docker.utils.DockerUtil;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import icons.MavenIcons;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.swing.*;

public class SettingPanel extends AzureSettingPanel<PushImageRunConfiguration> {
    public static final String PRIVATE_DOCKER_REGISTRY = "Private Docker Registry";
    private JPanel rootPanel;
    private JComboBox<Artifact> cbArtifact;
    private JLabel lblArtifact;
    private JPanel pnlArtifact;
    private ContainerSettingPanel containerSettingPanel;
    private JPanel pnlMavenProject;
    private JLabel lblMavenProject;
    private JComboBox cbMavenProject;

    /**
     * Constructor.
     */

    public SettingPanel(Project project) {
        super(project);
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.

        cbArtifact.addActionListener(e -> {
            artifactActionPerformed((Artifact) cbArtifact.getSelectedItem());
        });

        cbArtifact.setRenderer(new ListCellRendererWrapper<Artifact>() {
            @Override
            public void customize(JList jlist, Artifact artifact, int i, boolean b, boolean b1) {
                if (artifact != null) {
                    setIcon(artifact.getArtifactType().getIcon());
                    setText(artifact.getName());
                }
            }
        });

        cbMavenProject.addActionListener(e -> {
            MavenProject selectedMavenProject = (MavenProject) cbMavenProject.getSelectedItem();
            if (selectedMavenProject != null) {
                containerSettingPanel.setDockerPath(
                    DockerUtil.getDefaultDockerFilePathIfExist(selectedMavenProject.getDirectory())
                );
            }
        });

        cbMavenProject.setRenderer(new ListCellRendererWrapper<MavenProject>() {
            @Override
            public void customize(JList list, MavenProject mavenProject, int i, boolean b, boolean b1) {
                if (mavenProject != null) {
                    setIcon(MavenIcons.MavenProject);
                    setText(mavenProject.toString());
                }
            }
        });
    }

    @Override
    @NotNull
    public String getPanelName() {
        return "Push Image";
    }

    @Override
    @NotNull
    public JPanel getMainPanel() {
        return rootPanel;
    }

    @Override
    @NotNull
    protected JComboBox<Artifact> getCbArtifact() {
        return cbArtifact;
    }

    @Override
    @NotNull
    protected JLabel getLblArtifact() {
        return lblArtifact;
    }

    @Override
    @NotNull
    protected JComboBox<MavenProject> getCbMavenProject() {
        return cbMavenProject;
    }

    @Override
    @NotNull
    protected JLabel getLblMavenProject() {
        return lblMavenProject;
    }

    /**
     * Function triggered by any content change events.
     */
    @Override
    public void apply(PushImageRunConfiguration pushImageRunConfiguration) {
        pushImageRunConfiguration.setDockerFilePath(containerSettingPanel.getDockerPath());

        // set ACR info
        pushImageRunConfiguration.setPrivateRegistryImageSetting(new PrivateRegistryImageSetting(
            containerSettingPanel.getServerUrl().replaceFirst("^https?://", "").replaceFirst("/$", ""),
            containerSettingPanel.getUserName(),
            containerSettingPanel.getPassword(),
            containerSettingPanel.getImageTag(),
            ""
        ));
        final ISecureStore secureStore = AzureStoreManager.getInstance().getSecureStore();
        secureStore.savePassword(PRIVATE_DOCKER_REGISTRY, containerSettingPanel.getServerUrl(), containerSettingPanel.getUserName(),
            containerSettingPanel.getPassword());

        // set target
        pushImageRunConfiguration.setTargetPath(getTargetPath());
        pushImageRunConfiguration.setTargetName(getTargetName());
    }

    /**
     * Function triggered in constructing the panel.
     *
     * @param conf configuration instance
     */
    @Override
    public void resetFromConfig(PushImageRunConfiguration conf) {
        if (!isMavenProject()) {
            containerSettingPanel.setDockerPath(DockerUtil.getDefaultDockerFilePathIfExist(getProjectBasePath()));
        }

        PrivateRegistryImageSetting acrInfo = conf.getPrivateRegistryImageSetting();
        if (StringUtils.isEmpty(acrInfo.getPassword())) {
            final ISecureStore secureStore = AzureStoreManager.getInstance().getSecureStore();
            secureStore.migratePassword(containerSettingPanel.getServerUrl(), containerSettingPanel.getUserName(),
                PRIVATE_DOCKER_REGISTRY, containerSettingPanel.getServerUrl(), containerSettingPanel.getUserName());

            acrInfo.setPassword(secureStore.loadPassword(PRIVATE_DOCKER_REGISTRY, acrInfo.getServerUrl(), acrInfo.getUsername()));
        }
        containerSettingPanel.setTxtFields(acrInfo);

        // load dockerFile path from existing configuration.
        if (!Utils.isEmptyString(conf.getDockerFilePath())) {
            containerSettingPanel.setDockerPath(conf.getDockerFilePath());
        }
        containerSettingPanel.onListRegistries();
    }

    @Override
    public void disposeEditor() {
        containerSettingPanel.disposeEditor();
    }

    private void createUIComponents() {
        containerSettingPanel = new ContainerSettingPanel(this.project);
    }

    private void $$$setupUI$$$() {
    }
}
