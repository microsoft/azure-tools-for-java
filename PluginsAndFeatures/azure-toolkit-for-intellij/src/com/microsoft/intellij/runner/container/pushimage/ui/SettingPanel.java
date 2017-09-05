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
import com.intellij.openapi.util.Comparing;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.intellij.runner.container.pushimage.PushImageRunConfiguration;
import com.microsoft.intellij.util.MavenRunTaskUtil;

import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.project.MavenProject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import rx.Observable;
import rx.schedulers.Schedulers;

public class SettingPanel {
    private final Project project;
    private JPanel rootPanel;
    private JPanel pnlAcr;
    private JPanel pnlAcrHolder;
    private JTextField textServerUrl;
    private JTextField textUsername;
    private JPasswordField passwordField;
    private JTextField textImageTag;
    private JTextField textStartupFile;
    private JComboBox<Artifact> cbArtifact;
    private JLabel lblArtifact;
    private JPanel pnlArtifact;
    private Artifact lastSelectedArtifact;
    private boolean isCbArtifactInited;

    private boolean telemetrySent;

    /**
     * Constructor.
     */

    public SettingPanel(Project project) {
        this.project = project;

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
            AppInsightsClient.createByType(AppInsightsClient.EventType.Dialog, "Push Image",
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
     * Function triggered by any content change events.
     */
    @SuppressWarnings("Duplicates")
    public void apply(PushImageRunConfiguration pushImageRunConfiguration) {
        // set ACR info
        pushImageRunConfiguration.setPrivateRegistryImageSetting(new PrivateRegistryImageSetting(
                textServerUrl.getText().replaceFirst("^https?://", "").replaceFirst("/$", ""),
                textUsername.getText(),
                String.valueOf(passwordField.getPassword()),
                textImageTag.getText(),
                textStartupFile.getText()
        ));

        // set target
        if (lastSelectedArtifact != null) {
            pushImageRunConfiguration.setTargetPath(lastSelectedArtifact.getOutputFilePath());
            Path p = Paths.get(pushImageRunConfiguration.getTargetPath());
            if (null != p) {
                pushImageRunConfiguration.setTargetName(p.getFileName().toString());
            } else {
                pushImageRunConfiguration.setTargetName(lastSelectedArtifact.getName() + "."
                        + MavenConstants.TYPE_WAR);
            }
        } else {
            MavenProject mavenProject = MavenRunTaskUtil.getMavenProject(project);
            if (mavenProject != null) {
                pushImageRunConfiguration.setTargetPath(MavenRunTaskUtil.getTargetPath(mavenProject));
                pushImageRunConfiguration.setTargetName(MavenRunTaskUtil.getTargetName(mavenProject));
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

    /**
     * Function triggered in constructing the panel.
     *
     * @param conf configuration instance
     */
    public void reset(PushImageRunConfiguration conf) {
        if (!MavenRunTaskUtil.isMavenProject(project)) {
            List<Artifact> artifacts = MavenRunTaskUtil.collectProjectArtifact(project);
            setupArtifactCombo(artifacts, conf.getTargetPath());
        }

        PrivateRegistryImageSetting acrInfo = conf.getPrivateRegistryImageSetting();
        textServerUrl.setText(acrInfo.getServerUrl());
        textUsername.setText(acrInfo.getUsername());
        passwordField.setText(acrInfo.getPassword());
        textImageTag.setText(acrInfo.getImageNameWithTag());
        textStartupFile.setText(acrInfo.getStartupFile());

        sendTelemetry(conf.getTargetName());
    }

}
