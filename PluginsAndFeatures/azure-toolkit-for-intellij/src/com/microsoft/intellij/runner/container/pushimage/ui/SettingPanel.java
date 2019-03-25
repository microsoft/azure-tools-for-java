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

package com.microsoft.intellij.runner.container.pushimage.ui;

import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.intellij.runner.AzureSettingPanel;
import com.microsoft.intellij.runner.container.common.ContainerSettingPanel;
import com.microsoft.intellij.runner.container.pushimage.PushImageRunConfiguration;
import com.microsoft.intellij.runner.container.utils.DockerUtil;
import icons.MavenIcons;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

public class SettingPanel extends AzureSettingPanel<PushImageRunConfiguration> {
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
            artifactActionPeformed((Artifact) cbArtifact.getSelectedItem());
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
            public void customize(JList jList, MavenProject mavenProject, int i, boolean b, boolean b1) {
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
        PrivateRegistryImageSetting privateRegistryImageSetting = new PrivateRegistryImageSetting(
            containerSettingPanel.getServerUrl().replaceFirst("^https?://", "").replaceFirst("/$", ""),
            containerSettingPanel.getUserName(),
            containerSettingPanel.getPassword(),
            containerSettingPanel.getImageTag(),
            ""
        );
        pushImageRunConfiguration.setPrivateRegistryImageSetting(privateRegistryImageSetting);
        savePassword(containerSettingPanel.getServerUrl(), containerSettingPanel.getUserName(),
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
        acrInfo.setPassword(loadPassword(acrInfo.getServerUrl(), acrInfo.getUsername()));
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
