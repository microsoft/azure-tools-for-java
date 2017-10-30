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

import icons.MavenIcons;

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
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.runner.container.dockerhost.DockerHostRunConfiguration;
import com.microsoft.intellij.runner.container.utils.DockerUtil;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;

import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import rx.Observable;
import rx.schedulers.Schedulers;

import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SettingPanel {
    private static final String IMAGE_NAME_PREFIX = "localimage";
    private static final String DEFAULT_TAG_NAME = "latest";

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
    private TextFieldWithBrowseButton dockerFilePathTextField;
    private JPanel pnlMavenProject;
    private JLabel lblMavenProject;
    private JComboBox cbMavenProject;

    private Artifact lastSelectedArtifact;
    private boolean isCbArtifactInited;

    private boolean telemetrySent;

    /**
     * Constructor.
     */
    public SettingPanel(Project project) {
        this.project = project;

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
            public void customize(JList jList, MavenProject mavenProject, int i, boolean b, boolean b1) {
                if (mavenProject != null) {
                    setIcon(MavenIcons.MavenProject);
                    setText(mavenProject.toString());
                }
            }
        });

        telemetrySent = false;
    }

    // TODO: refactor later
    private void sendTelemetry(String targetName) {
        if (telemetrySent) {
            return;
        }
        Observable.fromCallable(() -> {
            Map<String, String> map = new HashMap<>();
            String fileType = "";
            if (targetName != null) {
                fileType = MavenRunTaskUtil.getFileType(targetName);
            }
            map.put("FileType", fileType);
            AppInsightsClient.createByType(AppInsightsClient.EventType.Dialog, "Docker Run",
                    "Open", map);
            return true;
        }).subscribeOn(Schedulers.io()).subscribe(
                (res) -> telemetrySent = true,
                (err) -> telemetrySent = true
        );
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
            setupArtifactCombo(artifacts, conf.getDataModel().getTargetPath());
            dockerFilePathTextField.setText(DockerUtil.getDefaultDockerFilePathIfExist(project.getBasePath()));
        } else {
            List<MavenProject> mavenProjects = MavenProjectsManager.getInstance(project).getProjects();
            setupMavenProjectCombo(mavenProjects, conf.getTargetPath());
        }
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
        sendTelemetry(conf.getTargetName());
    }

    @SuppressWarnings("Duplicates")
    private void setupMavenProjectCombo(List<MavenProject> mvnprjs, String targetPath) {
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
        lblMavenProject.setVisible(true);
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
        conf.setDockerFilePath(dockerFilePathTextField.getText());
        conf.setImageName(textImageName.getText());
        if (Utils.isEmptyString(textTagName.getText())) {
            conf.setTagName("latest");
        } else {
            conf.setTagName(textTagName.getText());
        }
        // set target
        if (lastSelectedArtifact != null) {
            String targetPath = lastSelectedArtifact.getOutputFilePath();
            conf.setTargetPath(targetPath);
            conf.setTargetName(Paths.get(targetPath).getFileName().toString());
        } else {
            MavenProject mavenProject = (MavenProject) cbMavenProject.getSelectedItem();
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
