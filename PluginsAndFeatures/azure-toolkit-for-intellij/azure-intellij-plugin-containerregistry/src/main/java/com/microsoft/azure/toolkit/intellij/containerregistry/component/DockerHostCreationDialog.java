/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.component;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComponentWithBrowseButton;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.ui.AnimatedIcon;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.component.AzureFileInput;
import com.microsoft.azure.toolkit.intellij.container.AzureDockerClient;
import com.microsoft.azure.toolkit.intellij.container.model.DockerHost;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class DockerHostCreationDialog extends AzureDialog<DockerHost>
    implements AzureForm<DockerHost> {
    public static final String CONNECTION_SUCCEEDED = "Connection Succeeded";
    private JPanel pnlRoot;
    private JCheckBox chkEnableTLS;
    private AzureFileInput txtCertPath;
    private JLabel lblDockerHost;
    private AzureTextInput txtDockerHost;
    private JLabel lblCertPath;
    private JPanel outputContainer;
    private JTextPane outputPanel;
    private JLabel outputStatusIcon;

    private Project project;

    public DockerHostCreationDialog(final Project project) {
        super(project);
        $$$setupUI$$$();
        this.init();
    }

    @Override
    protected void init() {
        super.init();
        chkEnableTLS.addItemListener(this::onSelectTLS);
        lblDockerHost.setLabelFor(txtDockerHost);
        txtDockerHost.setRequired(true);
        txtDockerHost.addValidator(this::validateDockerHost);
        txtDockerHost.addValueChangedListener(ignore -> resetValidationMessage());
        lblCertPath.setLabelFor(txtCertPath);
        txtCertPath.addValueChangedListener(ignore -> resetValidationMessage());
        txtCertPath.addValidator(this::validateCertPath);
        txtCertPath.addActionListener(new ComponentWithBrowseButton.BrowseFolderActionListener<>("Select Cert for Docker Host", null, txtCertPath,
            project, FileChooserDescriptorFactory.createSingleFolderDescriptor(), TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT));
    }

    private AzureValidationInfo validateCertPath() {
        final String value = txtCertPath.getValue();
        if (StringUtils.isNotEmpty(value) && !FileUtil.exists(value)) {
            return AzureValidationInfo.error(String.format("File %s does not exists", value), txtCertPath);
        }
        return AzureValidationInfo.success(txtCertPath);
    }

    private AzureValidationInfo validateDockerHost() {
        final String value = txtDockerHost.getValue();
        try {
            final URI uri = URI.create(value);
            if (StringUtils.isBlank(uri.getScheme())) {
                return AzureValidationInfo.error("Invalid Docker host URI, schema of uri could not be empty", txtDockerHost);
            }
        } catch (final Exception e) {
            return AzureValidationInfo.error(String.format("Invalid Docker host URI, %s", e.getMessage()), txtDockerHost);
        }
        return AzureValidationInfo.success(txtDockerHost);
    }

    private void resetValidationMessage() {
        outputPanel.setText(StringUtils.EMPTY);
        outputStatusIcon.setIcon(null);
        outputContainer.setVisible(false);
    }

    private void onSelectTLS(ItemEvent itemEvent) {
        lblCertPath.setVisible(chkEnableTLS.isSelected());
        txtCertPath.setVisible(chkEnableTLS.isSelected());
        txtCertPath.setRequired(chkEnableTLS.isSelected());
        txtCertPath.validateValueAsync();
    }

    @Override
    public AzureForm<DockerHost> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "New Docker Host Configuration";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public DockerHost getValue() {
        final String dockerHost = txtDockerHost.getValue();
        final String certPath = chkEnableTLS.isSelected() ? txtCertPath.getValue() : null;
        return new DockerHost(dockerHost, certPath);
    }

    @Override
    public void setValue(DockerHost data) {
        txtDockerHost.setValue(data.getDockerHost());
        chkEnableTLS.setSelected(StringUtils.isEmpty(data.getDockerCertPath()));
        txtCertPath.setValue(data.getDockerCertPath());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtDockerHost, txtCertPath);
    }

    @Override
    protected void doOKAction() {
        outputContainer.setVisible(true);
        outputPanel.setText("Connecting...");
        outputStatusIcon.setIcon(AnimatedIcon.Default.INSTANCE);
        Mono.fromRunnable(() -> {
                final AzureDockerClient dockerClient = AzureDockerClient.from(getValue());
                dockerClient.ping();
            }).subscribeOn(Schedulers.boundedElastic())
            .doOnError(e -> AzureTaskManager.getInstance().runLater(() -> {
                outputContainer.setVisible(true);
                outputPanel.setText(ExceptionUtils.getRootCauseMessage(e));
                outputStatusIcon.setIcon(AllIcons.General.Error);
            }, AzureTask.Modality.ANY))
            .doOnSuccess(e -> AzureTaskManager.getInstance().runLater(() -> {
                outputContainer.setVisible(true);
                outputPanel.setText(CONNECTION_SUCCEEDED);
                outputStatusIcon.setIcon(AllIcons.General.InspectionsOK);
            }, AzureTask.Modality.ANY)).subscribe();
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
