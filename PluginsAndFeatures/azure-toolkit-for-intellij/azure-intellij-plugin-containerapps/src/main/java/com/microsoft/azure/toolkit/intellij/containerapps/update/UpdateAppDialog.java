/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.update;

import com.azure.resourcemanager.appcontainers.models.EnvironmentVar;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBIntSpinner;
import com.microsoft.azure.toolkit.intellij.common.AzureCommentLabel;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureHideableTitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.EnvironmentVariablesTextFieldWithBrowseButton;
import com.microsoft.azure.toolkit.intellij.container.AzureDockerClient;
import com.microsoft.azure.toolkit.intellij.containerapps.component.*;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerapps.model.IngressConfig;
import com.microsoft.azure.toolkit.lib.containerapps.model.RevisionMode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class UpdateAppDialog extends AzureDialog<UpdateAppDialog.UpdateAppConfig> implements AzureForm<UpdateAppDialog.UpdateAppConfig> {
    private static final Pattern CONTAINER_APP_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9\\-]{0,30}[a-z0-9]$");
    private static final String CONTAINER_APP_NAME_VALIDATION_MESSAGE = "A name must consist of lower case alphanumeric characters or '-', start with an alphabetic character, and end with an alphanumeric character and cannot have '--'. The length must not be more than 32 characters.";
    private JPanel pnlRoot;

    private AzureHideableTitledSeparator titleApp;
    private JPanel pnlApp;
    private ContainerAppComboBox selectorApp;
    private AzureCommentLabel commentApp;

    private AzureHideableTitledSeparator titleDeployment;
    private JPanel pnlDeployment;
    private JRadioButton btnDeployCode;
    private JRadioButton btnDeployArtifact;
    private JRadioButton btnDeployImage;
    private ImageForm formImage;
    private CodeForm formCode;
    private ArtifactForm formArtifact;

    private AzureHideableTitledSeparator titleOther;
    private EnvironmentVariablesTextFieldWithBrowseButton inputEnv;
    private JPanel pnlOther;

    private AzureHideableTitledSeparator titleIngress;
    private IngressConfigurationPanel pnlIngress;
    private JBIntSpinner intMinReplicas;
    private JBIntSpinner intMaxReplicas;

    private DeploymentSourceForm formDeploymentSource;

    public static final IngressConfig QUICK_START_INGRESS = IngressConfig.builder().enableIngress(true).external(true).targetPort(80).build();

    public UpdateAppDialog(final Project project) {
        super(project);
        $$$setupUI$$$();
        init();
    }

    @Override
    protected void init() {
        super.init();
        this.selectorApp.addItemListener(this::onAppChanged);
        this.selectorApp.setRequired(true);
        this.commentApp.setVisible(false);
        this.commentApp.setIconWithAlignment(AllIcons.General.Warning, SwingConstants.LEFT, SwingConstants.CENTER);

        this.btnDeployCode.addItemListener(this::onDeploymentSourceChanged);
        this.btnDeployArtifact.addItemListener(this::onDeploymentSourceChanged);
        this.btnDeployImage.addItemListener(this::onDeploymentSourceChanged);
        this.btnDeployCode.setSelected(true);
        this.formCode.setVisible(true);
        this.formImage.setVisible(false);
        this.formArtifact.setVisible(false);
        this.formImage.setOnImageFormChanged(this::onImageFormChanged);
        this.formCode.setOnFolderChanged(this::onSelectedFolderChanged);
        this.formDeploymentSource = this.formCode;

        this.titleApp.addContentComponent(pnlApp);
        this.titleDeployment.addContentComponent(pnlDeployment);
        this.titleIngress.addContentComponent(pnlIngress.getPnlRoot());
        this.titleOther.addContentComponent(pnlOther);

        this.titleApp.expand();
        this.titleDeployment.expand();
        this.titleIngress.collapse();
        this.titleOther.expand();
    }

    private void onAppChanged(final ItemEvent e) {
        this.commentApp.setVisible(false);
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final ContainerApp app = (ContainerApp) e.getItem();
            if (Objects.isNull(app)) {
                return;
            }
            this.setImageConfig(app.getImageConfig());
            this.formDeploymentSource.setContainerApp(app);
            this.pnlIngress.setValue(app.getIngressConfig());
            if (app.hasUnsupportedFeatures()) {
                final RevisionMode revisionsMode = app.revisionModel();
                final String message = revisionsMode == RevisionMode.SINGLE ?
                    "This will overwrite the active revision and unsupported features in IntelliJ will be lost." :
                    "Unsupported features in IntelliJ will be lost in the new revision.";
                this.commentApp.setText(message);
                this.commentApp.setVisible(true);
            }
        }
    }

    private void onImageFormChanged(final String type) {
        final boolean useQuickStartImage = this.formDeploymentSource == this.formImage && Objects.equals(type, ImageSourceTypeComboBox.QUICK_START);
        if (useQuickStartImage) {
            pnlIngress.setValue(QUICK_START_INGRESS);
        }
        this.titleIngress.toggle(!useQuickStartImage);
        this.titleIngress.setEnabled(!useQuickStartImage);
        this.pnlIngress.setEnabled(!useQuickStartImage);

        this.titleOther.toggle(!useQuickStartImage);
        this.inputEnv.setEnabled(!useQuickStartImage);
        this.intMaxReplicas.setEnabled(!useQuickStartImage);
        this.intMinReplicas.setEnabled(!useQuickStartImage);

        this.titleOther.setVisible(!useQuickStartImage);
        this.titleIngress.setVisible(!useQuickStartImage);
    }

    private void onSelectedFolderChanged(final Path folder) {
        if (this.formDeploymentSource != this.formCode && Objects.nonNull(folder) && Files.isDirectory(folder)) {
            return;
        }
        final Path dockerfile = folder.resolve("Dockerfile");
        if (Files.isRegularFile(dockerfile)) {
            try {
                final List<Integer> ports = AzureDockerClient.getExposedPortsOfDockerfile(dockerfile.toFile());
                if (!ports.isEmpty()) {
                    final IngressConfig ingressConfig = IngressConfig.builder().enableIngress(true).external(true).targetPort(ports.get(0)).build();
                    this.pnlIngress.setValue(ingressConfig);
                }
            } catch (final IOException e) {
                log.error("Failed to parse Dockerfile", e);
            }
        }
    }

    private void mergeContainerConfiguration(final ImageForm target, final ContainerAppDraft.ImageConfig value) {
        try {
            final ContainerAppDraft.ImageConfig targetValue = target.getValue();
            if (ObjectUtils.allNotNull(targetValue, value)) {
                if (!Objects.equals(Objects.requireNonNull(targetValue).getContainerRegistry(), value.getContainerRegistry()) ||
                    !Objects.equals(targetValue.getFullImageName(), value.getFullImageName())) {
                    target.setValue(value);
                }
            }
        } catch (final RuntimeException e) {
            // swallow exception as required parameters may be null
            target.setValue(value);
        }
    }

    private void createUIComponents() {
        this.formCode = new CodeForm(this.project);
        this.formArtifact = new ArtifactForm(this.project);
        this.intMaxReplicas = new JBIntSpinner(10, 1, 300);
        this.intMinReplicas = new JBIntSpinner(0, 0, 300);
    }

    private void onDeploymentSourceChanged(ItemEvent event) {
        this.formImage.setVisible(this.btnDeployImage.isSelected());
        this.formArtifact.setVisible(this.btnDeployArtifact.isSelected());
        this.formCode.setVisible(this.btnDeployCode.isSelected());

        this.formDeploymentSource = this.btnDeployImage.isSelected() ? this.formImage :
            this.btnDeployArtifact.isSelected() ? this.formArtifact : this.formCode;
        this.onImageFormChanged(this.formImage.getRegistryType());
        this.onSelectedFolderChanged(this.formCode.getSourceFolder());
    }

    @Override
    public AzureForm<UpdateAppConfig> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Update Container App";
    }

    @Override
    @Nullable
    protected JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public synchronized UpdateAppConfig getValue() {
        this.formDeploymentSource.setContainerApp(this.selectorApp.getValue());

        final ContainerAppDraft.ImageConfig imageConfig = this.formDeploymentSource.getValue();
        final List<EnvironmentVar> vars = inputEnv.getEnvironmentVariables().entrySet().stream()
            .map(e -> new EnvironmentVar().withName(e.getKey()).withValue(e.getValue()))
            .collect(Collectors.toList());
        Optional.ofNullable(imageConfig).ifPresent(config -> config.setEnvironmentVariables(vars));

        final ContainerAppDraft.ScaleConfig scaleConfig = ContainerAppDraft.ScaleConfig.builder()
            .maxReplicas(this.intMaxReplicas.getNumber())
            .minReplicas(this.intMinReplicas.getNumber())
            .build();

        final UpdateAppConfig config = new UpdateAppConfig();
        config.setApp(this.selectorApp.getValue());
        config.setImageConfig(imageConfig);
        config.setIngressConfig(pnlIngress.getValue());
        config.setScaleConfig(scaleConfig);
        return config;
    }

    @Override
    public void setValue(UpdateAppConfig config) {
        final ContainerAppDraft.ImageConfig imageConfig = config.getImageConfig();
        Optional.ofNullable(config.getApp()).ifPresent(a -> this.selectorApp.setValue(a));
        Optional.ofNullable(config.getIngressConfig()).ifPresent(pnlIngress::setValue);
        Optional.ofNullable(config.getScaleConfig()).ifPresent(c -> {
            // https://learn.microsoft.com/en-us/azure/container-apps/scale-app?pivots=azure-cli
            this.intMaxReplicas.setNumber(Optional.ofNullable(c.getMaxReplicas()).orElse(10));
            this.intMinReplicas.setNumber(Optional.ofNullable(c.getMinReplicas()).orElse(0));
        });
        this.setImageConfig(imageConfig);
    }

    private void setImageConfig(final ContainerAppDraft.ImageConfig imageConfig) {
        if (Objects.nonNull(imageConfig)) {
            final Optional<Path> source = Optional.ofNullable(imageConfig.getBuildImageConfig()).map(ContainerAppDraft.BuildImageConfig::getSource);
            if (source.isEmpty()) {
                this.btnDeployImage.setSelected(true);
                this.formImage.setValue(imageConfig);
            } else if (source.map(Files::isDirectory).orElse(false)) {
                this.btnDeployCode.setSelected(true);
                this.formCode.setValue(imageConfig);
            } else {
                this.btnDeployArtifact.setSelected(true);
                this.formArtifact.setValue(imageConfig);
            }
            this.inputEnv.setEnvironmentVariables(imageConfig.getEnvironmentVariables().stream()
                .collect(Collectors.toMap(EnvironmentVar::name, EnvironmentVar::value)));
        }
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(this.formDeploymentSource, this.selectorApp);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }

    @Data
    public static class UpdateAppConfig {
        private ContainerApp app;
        private ContainerAppDraft.ImageConfig imageConfig;
        private IngressConfig ingressConfig;
        private ContainerAppDraft.ScaleConfig scaleConfig;
    }

}
