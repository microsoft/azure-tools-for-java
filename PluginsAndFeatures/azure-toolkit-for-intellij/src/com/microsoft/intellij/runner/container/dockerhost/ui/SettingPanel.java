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

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.intellij.runner.container.dockerhost.DockerHostRunConfiguration;
import com.microsoft.intellij.util.MavenRunTaskUtil;

import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.project.MavenProject;

import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SettingPanel {
    private final Project project;
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

    private Artifact lastSelectedArtifact;
    private boolean isCbArtifactInited;

    /**
     * Constructor.
     */
    public SettingPanel(Project project) {
        this.project = project;

        dockerCertPathTextField.addActionListener(this::onDockerCertPathBrowseButtonClick);
        comboTlsEnabled.addActionListener(event -> updateComponentEnabledState());


        // Artifact to build
        isCbArtifactInited = false;
        cbArtifact.addActionListener(e -> {
            final Artifact selectedArtifact = (Artifact) cbArtifact.getSelectedItem();
            if (!Comparing.equal(lastSelectedArtifact, selectedArtifact)) {
                if (isCbArtifactInited) {
                    if (lastSelectedArtifact != null) {
                        BuildArtifactsBeforeRunTaskProvider
                                .setBuildArtifactBeforeRunOption(rootPanel, project, lastSelectedArtifact, false);
                    }
                    if (selectedArtifact != null) {
                        BuildArtifactsBeforeRunTaskProvider
                                .setBuildArtifactBeforeRunOption(rootPanel, project, selectedArtifact, true);
                    }
                }
                lastSelectedArtifact = selectedArtifact;

            }
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
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }


    /**
     * Function triggered in constructing the panel.
     *
     * @param conf configuration instance
     */
    public void reset(DockerHostRunConfiguration conf) {
        textDockerHost.setText(conf.getDockerHost());
        comboTlsEnabled.setSelected(conf.isTlsEnabled());
        dockerCertPathTextField.setText(conf.getDockerCertPath());
        textImageName.setText(conf.getImageName());
        textTagName.setText(conf.getTagName());
        updateComponentEnabledState();

        if (!MavenRunTaskUtil.isMavenProject(project)) {
            List<Artifact> artifacts = MavenRunTaskUtil.collectProjectArtifact(project);
            setupArtifactCombo(artifacts, conf.getDockerHostRunModel().getTargetPath());
        }

    }

    /**
     * Function triggered by any content change events.
     *
     * @param conf configuration instance
     */
    @SuppressWarnings("Duplicates")
    public void apply(DockerHostRunConfiguration conf) {
        conf.setDockerHost(textDockerHost.getText());
        conf.setTlsEnabled(comboTlsEnabled.isSelected());
        conf.setDockerCertPath(dockerCertPathTextField.getText());
        conf.setImageName(textImageName.getText());
        if (Utils.isEmptyString(textTagName.getText())) {
            conf.setTagName("latest");
        } else {
            conf.setTagName(textTagName.getText());
        }
        // set target
        if (lastSelectedArtifact != null) {
            conf.setTargetPath(lastSelectedArtifact.getOutputFilePath());
            Path p = Paths.get(conf.getTargetPath());
            if (null != p) {
                conf.setTargetName(p.getFileName().toString());
            } else {
                // TODO: get package type according to artifact
                conf.setTargetName(lastSelectedArtifact.getName() + "." + MavenConstants.TYPE_WAR);
            }
        } else {
            MavenProject mavenProject = MavenRunTaskUtil.getMavenProject(project);
            if (mavenProject != null) {
                conf.setTargetPath(MavenRunTaskUtil.getTargetPath(mavenProject));
                conf.setTargetName(MavenRunTaskUtil.getTargetName(mavenProject));
            }
        }
    }

    @SuppressWarnings("Duplicates")
    private void setupArtifactCombo(List<Artifact> artifacts, String targetPath) {
        isCbArtifactInited = false;
        cbArtifact.removeAllItems();
        if (null != artifacts) {
            for (Artifact artifact : artifacts) {
                cbArtifact.addItem(artifact);
                if (null != targetPath && Comparing.equal(artifact.getOutputFilePath(), targetPath)) {
                    cbArtifact.setSelectedItem(artifact);
                }
            }
        }
        cbArtifact.setVisible(true);
        lblArtifact.setVisible(true);
        isCbArtifactInited = true;
    }

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
