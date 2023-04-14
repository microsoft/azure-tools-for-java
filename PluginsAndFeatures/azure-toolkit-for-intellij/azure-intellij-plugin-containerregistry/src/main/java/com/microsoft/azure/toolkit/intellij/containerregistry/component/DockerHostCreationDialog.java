/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.component;

import com.github.dockerjava.api.DockerClient;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.components.JBLabel;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.component.AzureFileInput;
import com.microsoft.azure.toolkit.intellij.container.DockerUtil;
import com.microsoft.azure.toolkit.intellij.container.model.DockerHost;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;

public class DockerHostCreationDialog extends AzureDialog<DockerHost>
        implements AzureForm<DockerHost> {
    public static final String CONNECTION_SUCCEEDED = "Connection Succeeded";
    private JPanel pnlRoot;
    private JCheckBox chkEnableTLS;
    private JLabel lblCertPath;
    private AzureFileInput txtCertPath;
    private JLabel lblDockerHost;
    private AzureTextInput txtDockerHost;
    private JButton btnTestConnection;
    private JBLabel lblValidation;

    private Project project;

    public DockerHostCreationDialog(final Project project) {
        super(project);
        $$$setupUI$$$();
        this.init();
    }

    @Override
    protected void init() {
        super.init();
        txtDockerHost.setRequired(true);
        chkEnableTLS.addItemListener(this::onSelectTLS);
        btnTestConnection.addActionListener(this::onTestConnection);
        txtDockerHost.addValueChangedListener(ignore -> resetValidationMessage());
        txtCertPath.addValueChangedListener(ignore -> resetValidationMessage());
    }

    private void resetValidationMessage() {
        lblValidation.setText(StringUtils.EMPTY);
        lblValidation.setIcon(null);
    }

    private void onTestConnection(ActionEvent actionEvent) {
        lblValidation.setText("Connecting...");
        lblValidation.setIconWithAlignment(AnimatedIcon.Default.INSTANCE, SwingConstants.LEFT, SwingConstants.CENTER);
        Mono.fromRunnable(() -> {
                    final DockerClient dockerClient = DockerUtil.getDockerClient(getValue());
                    DockerUtil.ping(dockerClient);
                }).subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> AzureTaskManager.getInstance().runLater(() -> {
                    lblValidation.setText(ExceptionUtils.getRootCauseMessage(e));
                    lblValidation.setIconWithAlignment(AllIcons.General.Error, SwingConstants.LEFT, SwingConstants.CENTER);
                }, AzureTask.Modality.ANY))
                .doOnSuccess(e -> AzureTaskManager.getInstance().runLater(() -> {
                    lblValidation.setText(CONNECTION_SUCCEEDED);
                    lblValidation.setIconWithAlignment(AllIcons.General.InspectionsOK, SwingConstants.LEFT, SwingConstants.CENTER);
                }, AzureTask.Modality.ANY)).subscribe();
    }

    private void onSelectTLS(ItemEvent itemEvent) {
        lblCertPath.setEnabled(chkEnableTLS.isSelected());
        txtCertPath.setEnabled(chkEnableTLS.isSelected());
        txtCertPath.setRequired(chkEnableTLS.isSelected());
        txtCertPath.revalidate();
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


    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
