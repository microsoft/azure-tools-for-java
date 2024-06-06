/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.HyperlinkLabel;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.EnvironmentVariablesTextFieldWithBrowseButton;
import com.microsoft.azure.toolkit.intellij.common.component.AzureFileInput;
import com.microsoft.azure.toolkit.intellij.containerregistry.component.ACRRegistryComboBox;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistryDraft;
import com.microsoft.azure.toolkit.lib.containerregistry.model.Sku;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.*;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class CodeForm implements AzureFormJPanel<ContainerAppDraft.ImageConfig>, DeploymentSourceForm {
    private static final String LINK_SUPPORTED_JAVA_BUILD_ENV = "https://learn.microsoft.com/en-us/azure/container-apps/java-build-environment-variables?source=recommendations#supported-java-build-environment-variables";
    private final Project project;
    @Getter
    private JPanel contentPanel;
    private JLabel lblCode;
    private AzureFileInput fileCode;
    private EnvironmentVariablesTextFieldWithBrowseButton inputEnv;
    private HyperlinkLabel buildEnvLink;
    @Setter
    private Consumer<Path> onFolderChanged = type -> {
    };
    private ACRRegistryComboBox selectorRegistry;
    private JLabel lblRegistry;

    @Getter
    private ContainerApp containerApp;
    @Setter
    private Module module;

    public CodeForm(final Project project) {
        super();
        this.project = project;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    public void setContainerApp(final ContainerApp containerApp) {
        this.containerApp = containerApp;
        if (Objects.nonNull(containerApp)) {
            // update the draft value
            this.selectorRegistry.setSubscription(containerApp.getSubscription());
            final Object rawValue = this.selectorRegistry.getRawValue();
            final ContainerRegistryDraft draft = getDraftRegistry();
            final List<ContainerRegistry> draftItems = selectorRegistry.getDraftItems();
            draftItems.clear();
            draftItems.add(draft);
            if (Objects.isNull(rawValue)) {
                selectorRegistry.setValue(val -> val.isAdminUserEnabled() &&
                        StringUtils.equalsIgnoreCase(val.getResourceGroupName(), containerApp.getResourceGroupName()));
            } else if (rawValue instanceof ContainerRegistryDraft) {
                selectorRegistry.setValue(draft);
            }
            selectorRegistry.reloadItems();
        }
    }

    @Nullable
    private ContainerRegistryDraft getDraftRegistry() {
        if (Objects.isNull(this.containerApp)) {
            return null;
        }
        final String fullImageName = this.getDefaultFullImageName();
        final ContainerAppDraft.ImageConfig config = new ContainerAppDraft.ImageConfig(fullImageName);
        final String acrRegistryName = Objects.requireNonNull(config.getAcrRegistryName());
        final ContainerRegistryDraft draft = az(AzureContainerRegistry.class)
                .registry(containerApp.getSubscriptionId()).create(acrRegistryName, containerApp.getResourceGroupName());
        draft.setSku(Sku.Standard);
        draft.setAdminUserEnabled(true);
        draft.setRegion(Optional.ofNullable(containerApp.getRegion()).orElse(Region.US_EAST));
        return draft;
    }

    private void init() {
        this.fileCode.setLabel("Code");
        this.fileCode.setRequired(true);
        this.fileCode.addValidator(this::validateCodePath);
        this.lblCode.setLabelFor(fileCode);
        this.lblCode.setIcon(AllIcons.General.ContextHelp);
        buildEnvLink.setHtmlText(String.format("<html><a href=\"%s\">Supported Java build environment variables</a></html>", LINK_SUPPORTED_JAVA_BUILD_ENV));
        buildEnvLink.addHyperlinkListener((e) -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse(LINK_SUPPORTED_JAVA_BUILD_ENV);
            }
        });
        buildEnvLink.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    public Path getSourceFolder() {
        return Optional.ofNullable(fileCode.getValue())
            .map(Path::of)
            .filter(Files::exists)
            .orElse(null);
    }

    @Override
    public ContainerAppDraft.ImageConfig getValue() {
        final String fullImageName = this.getDefaultFullImageName();
        final ContainerAppDraft.ImageConfig config = new ContainerAppDraft.ImageConfig(fullImageName);
        config.setBuildImageConfig(getImageConfig());
        Optional.ofNullable(this.selectorRegistry.getValue())
                .filter(ignore -> this.selectorRegistry.isVisible())
                .ifPresent(config::setContainerRegistry);
        return config;
    }

    private ContainerAppDraft.BuildImageConfig getImageConfig() {
        final ContainerAppDraft.BuildImageConfig buildConfig = new ContainerAppDraft.BuildImageConfig();
        Optional.ofNullable(fileCode.getValue())
                .map(Path::of)
                .filter(Files::exists)
                .ifPresent(buildConfig::setSource);
        final Map<String, String> envVarsMap = this.inputEnv.getEnvironmentVariables();
        buildConfig.setSourceBuildEnv(envVarsMap);
        return buildConfig;
    }

    @Override
    public void setValue(final ContainerAppDraft.ImageConfig config) {
        Optional.ofNullable(config.getBuildImageConfig()).ifPresent(buildConfig -> {
            Optional.ofNullable(buildConfig.getSource()).map(Path::toString).ifPresent(fileCode::setValue);
            Optional.ofNullable(config.getContainerRegistry()).ifPresent(selectorRegistry::setValue);
            Optional.ofNullable(buildConfig.getSourceBuildEnv()).ifPresent(this.inputEnv::setEnvironmentVariables);
        });
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.fileCode);
    }

    @Override
    public void setVisible(final boolean visible) {
        this.contentPanel.setVisible(visible);
    }

    private void createUIComponents() {
        this.fileCode = new AzureFileInput();
        this.fileCode.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener<>("Select Path of Source Code", null, fileCode,
            this.project, FileChooserDescriptorFactory.createSingleFolderDescriptor(), TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT));
        this.fileCode.addValueChangedListener(s -> onFolderChanged.accept(Path.of(s)));
        this.fileCode.addValueChangedListener(this::onSelectFilePath);
    }

    private void onSelectFilePath(String s) {
        final ContainerAppDraft.BuildImageConfig imageConfig = getImageConfig();
        this.lblRegistry.setVisible(imageConfig.sourceHasDockerFile());
        this.selectorRegistry.setVisible(imageConfig.sourceHasDockerFile());
    }

    public void setCodeSource(final String path) {
        this.fileCode.setValue(path);
    }

    public void setCodeSourceByModule(final Module module) {
        if (Objects.equals(module, this.module)) {
            return;
        }
        final String source = Optional.ofNullable(ProjectUtil.guessModuleDir(module))
                .map(VirtualFile::getPath)
                .orElseGet(() -> ModuleUtil.getModuleDirPath(module));
        this.fileCode.setValue(source);
    }

    private AzureValidationInfo validateCodePath() {
        final String path = this.fileCode.getValue();
        if (!FileUtil.exists(path)) {
            return AzureValidationInfo.error("Directory does not exist", this.fileCode);
        }
        if (!FileUtils.isDirectory(new File(path))) {
            return AzureValidationInfo.error("Not a directory", this.fileCode);
        }
        return AzureValidationInfo.ok(this.fileCode);
    }
}
