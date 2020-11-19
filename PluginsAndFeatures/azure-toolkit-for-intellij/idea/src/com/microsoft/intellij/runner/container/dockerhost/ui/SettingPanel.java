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

package com.microsoft.intellij.runner.container.dockerhost.ui;

import com.microsoft.intellij.runner.AzureSettingPanel;
import icons.MavenIcons;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.intellij.runner.container.dockerhost.DockerHostRunConfiguration;
import com.microsoft.intellij.runner.container.utils.DockerUtil;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;

import java.awt.event.ActionEvent;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SettingPanel extends AzureSettingPanel<DockerHostRunConfiguration> {
    private static final String IMAGE_NAME_PREFIX = "localimage";
    private static final String DEFAULT_TAG_NAME = "latest";

    private JTextField textDockerHost;
    private JCheckBox comboTlsEnabled;
    private TextFieldWithBrowseButton dockerCertPathTextField;
    private JTextField textImageName;
    private JTextField textTagName;
    private JPanel pnlArtifact;
    private JLabel lblArtifact;
    private JComboBox<Artifact> cbArtifact;
    private JPanel rootPanel;
    private JPanel pnlDockerCertPath;
    private TextFieldWithBrowseButton dockerFilePathTextField;
    private JPanel pnlMavenProject;
    private JLabel lblMavenProject;
    private JComboBox cbMavenProject;

    /**
     * Constructor.
     */
    public SettingPanel(Project project) {
        super(project);

        dockerCertPathTextField.addActionListener(this::onDockerCertPathBrowseButtonClick);
        comboTlsEnabled.addActionListener(event -> updateComponentEnabledState());

        dockerFilePathTextField.addActionListener(e -> {
            String path = dockerFilePathTextField.getText();
            final VirtualFile file = FileChooser.chooseFile(
                    new FileChooserDescriptor(
                            true /*chooseFiles*/,
                            false /*chooseFolders*/,
                            false /*chooseJars*/,
                            false /*chooseJarsAsFiles*/,
                            false /*chooseJarContents*/,
                            false /*chooseMultiple*/
                    ),
                    project,
                    Utils.isEmptyString(path) ? null : LocalFileSystem.getInstance().findFileByPath(path)
            );
            if (file != null) {
                dockerFilePathTextField.setText(file.getPath());
            }
        });

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
                dockerFilePathTextField.setText(
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
        return "Docker Run";
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
     * Function triggered in constructing the panel.
     *
     * @param conf configuration instance
     */
    @Override
    public void resetFromConfig(DockerHostRunConfiguration conf) {
        if (!isMavenProject()) {
            dockerFilePathTextField.setText(DockerUtil.getDefaultDockerFilePathIfExist(getProjectBasePath()));
        }

        textDockerHost.setText(conf.getDockerHost());
        comboTlsEnabled.setSelected(conf.isTlsEnabled());
        dockerCertPathTextField.setText(conf.getDockerCertPath());
        textImageName.setText(conf.getImageName());
        textTagName.setText(conf.getTagName());
        updateComponentEnabledState();

        // load dockerFile path from existing configuration.
        if (!Utils.isEmptyString(conf.getDockerFilePath())) {
            dockerFilePathTextField.setText(conf.getDockerFilePath());
        }

        // default value for new resources
        DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        String date = df.format(new Date());
        if (Utils.isEmptyString(textImageName.getText())) {
            textImageName.setText(String.format("%s-%s", IMAGE_NAME_PREFIX, date));
        }
        if (Utils.isEmptyString(textTagName.getText())) {
            textTagName.setText(DEFAULT_TAG_NAME);
        }
        if (Utils.isEmptyString(textDockerHost.getText())) {
            try {
                textDockerHost.setText(DefaultDockerClient.fromEnv().uri().toString());
            } catch (DockerCertificateException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Function triggered by any content change events.
     *
     * @param conf configuration instance
     */
    @Override
    public void apply(DockerHostRunConfiguration conf) {
        conf.setDockerHost(textDockerHost.getText());
        conf.setTlsEnabled(comboTlsEnabled.isSelected());
        conf.setDockerCertPath(dockerCertPathTextField.getText());
        conf.setDockerFilePath(dockerFilePathTextField.getText());
        conf.setImageName(textImageName.getText());
        if (Utils.isEmptyString(textTagName.getText())) {
            conf.setTagName("latest");
        } else {
            conf.setTagName(textTagName.getText());
        }

        conf.setTargetPath(getTargetPath());
        conf.setTargetName(getTargetName());
    }

    @Override
    public void disposeEditor() {}

    private void updateComponentEnabledState() {
        pnlDockerCertPath.setVisible(comboTlsEnabled.isSelected());
    }

    private void onDockerCertPathBrowseButtonClick(ActionEvent event) {
        String path = dockerCertPathTextField.getText();
        final VirtualFile[] files = FileChooser.chooseFiles(
                new FileChooserDescriptor(false, true, true, false, false, false),
                dockerCertPathTextField,
                null,
                Utils.isEmptyString(path) ? null : LocalFileSystem.getInstance().findFileByPath(path));
        if (files.length > 0) {
            final StringBuilder builder = new StringBuilder();
            for (VirtualFile file : files) {
                if (builder.length() > 0) {
                    builder.append(File.pathSeparator);
                }
                builder.append(FileUtil.toSystemDependentName(file.getPath()));
            }
            path = builder.toString();
            dockerCertPathTextField.setText(path);
        }
    }
}
