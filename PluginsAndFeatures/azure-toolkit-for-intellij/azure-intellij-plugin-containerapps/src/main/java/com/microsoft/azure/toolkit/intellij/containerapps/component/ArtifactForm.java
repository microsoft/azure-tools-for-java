/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.ui.HyperlinkLabel;
import com.microsoft.azure.toolkit.intellij.common.*;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ItemListener;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class ArtifactForm implements AzureFormJPanel<ContainerAppDraft.ImageConfig>, DeploymentSourceForm {
    private static final String LINK_SUPPORTED_JAVA_BUILD_ENV = "https://learn.microsoft.com/en-us/azure/container-apps/java-build-environment-variables?source=recommendations#supported-java-build-environment-variables";
    private final Project project;
    @Getter
    private JPanel contentPanel;
    private JLabel lblArtifact;
    private AzureArtifactComboBox selectorArtifact;
    private EnvironmentVariablesTextFieldWithBrowseButton inputEnv;
    private HyperlinkLabel buildEnvLink;

    @Setter
    @Getter
    private ContainerApp containerApp;

    public ArtifactForm(final Project project) {
        super();
        this.project = project;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    private void init() {
        this.selectorArtifact.setLabel("Artifact");
        this.selectorArtifact.setRequired(true);
        this.selectorArtifact.setFileFilter(virtualFile -> {
            final String ext = FileNameUtils.getExtension(virtualFile.getPath());
            return StringUtils.equalsAnyIgnoreCase(ext, "jar", "war");
        });
        this.lblArtifact.setLabelFor(selectorArtifact);
        this.lblArtifact.setIcon(AllIcons.General.ContextHelp);
        buildEnvLink.setHtmlText(String.format("<html><a href=\"%s\">Supported Java build environment variables</a></html>", LINK_SUPPORTED_JAVA_BUILD_ENV));
        buildEnvLink.addHyperlinkListener((e) -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse(LINK_SUPPORTED_JAVA_BUILD_ENV);
            }
        });
        buildEnvLink.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    @Override
    public ContainerAppDraft.ImageConfig getValue() {
        final String fullImageName = this.getDefaultFullImageName();
        final ContainerAppDraft.ImageConfig config = new ContainerAppDraft.ImageConfig(fullImageName);
        final ContainerAppDraft.BuildImageConfig buildConfig = new ContainerAppDraft.BuildImageConfig();
        Optional.ofNullable(selectorArtifact.getValue()).map(AzureArtifact::getFileForDeployment)
            .map(Path::of)
            .filter(Files::exists)
            .ifPresent(buildConfig::setSource);
        final Map<String, String> envVarsMap = this.inputEnv.getEnvironmentVariables();
        buildConfig.setSourceBuildEnv(envVarsMap);
        config.setBuildImageConfig(buildConfig);
        return config;
    }

    @Override
    public void setValue(final ContainerAppDraft.ImageConfig config) {
        Optional.ofNullable(config.getBuildImageConfig()).ifPresent(buildConfig -> {
            final AzureArtifact artifact = AzureArtifactManager.getInstance(project).getAllSupportedAzureArtifacts().stream()
                    .filter(a -> StringUtils.equals(a.getFileForDeployment(), buildConfig.getSource().toAbsolutePath().toString()))
                    .findFirst()
                    .orElseGet(() -> AzureArtifact.createFromFile(buildConfig.getSource().toAbsolutePath().toString(), project));
            Optional.ofNullable(artifact).ifPresent(this.selectorArtifact::setArtifact);
            Optional.ofNullable(buildConfig.getSourceBuildEnv()).ifPresent(this.inputEnv::setEnvironmentVariables);
        });
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.selectorArtifact);
    }

    @Override
    public void setVisible(final boolean visible) {
        this.contentPanel.setVisible(visible);
    }

    public void setFixedArtifact(final AzureArtifact artifact){
        this.lblArtifact.setVisible(false);
        this.selectorArtifact.setVisible(false);
        this.selectorArtifact.setArtifact(artifact);
    }

    private void createUIComponents() {
        this.selectorArtifact = new AzureArtifactComboBox(project, true);
        this.selectorArtifact.reloadItems();
    }

    public void setModule(Module module) {
        selectorArtifact.setArtifactFilter(artifact -> artifact.getModule() == module &&
                StringUtils.equalsAnyIgnoreCase(artifact.getPackaging(), "jar", "war"));
        selectorArtifact.reloadItems();
        selectorArtifact.setFileArtifactOnly(false);
        selectorArtifact.addValidator(() -> {
            final AzureArtifact artifact = this.selectorArtifact.getValue();
            if (Objects.nonNull(artifact) && !StringUtils.equalsAnyIgnoreCase(artifact.getPackaging(), "jar", "war")) {
                return AzureValidationInfo.error("Invalid artifact, Azure Container app only supports 'jar' and 'war' artifact.", this.selectorArtifact);
            }
            return AzureValidationInfo.success(this.selectorArtifact);
        });
    }

    public void addArtifactListener(@Nonnull final ItemListener listener) {
        this.selectorArtifact.addItemListener(listener);
    }

    public AzureArtifact getArtifact() {
        return this.selectorArtifact.getValue();
    }
}
