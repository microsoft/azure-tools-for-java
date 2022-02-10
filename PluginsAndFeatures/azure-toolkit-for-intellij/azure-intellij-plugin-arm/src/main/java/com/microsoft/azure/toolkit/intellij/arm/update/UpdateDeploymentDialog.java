/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm.update;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.HyperlinkLabel;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.resource.ResourceDeployment;
import com.microsoft.azure.toolkit.lib.resource.ResourceDeploymentDraft;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.ui.util.UIUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.BROWSE_TEMPLATE_SAMPLES;

public class UpdateDeploymentDialog extends AzureDialogWrapper {
    public static final String ARM_DOC = "https://azure.microsoft.com/en-us/resources/templates/";

    private JPanel contentPane;
    private JLabel subsNameLabel;
    private JLabel rgNameLabel;
    private JLabel deploymentNameLabel;
    private HyperlinkLabel lblTemplateHover;
    private JLabel lblResourceParameters;
    private TextFieldWithBrowseButton templateTextField;
    private TextFieldWithBrowseButton parametersTextField;

    private final Project project;
    private final ResourceDeployment deployment;

    public UpdateDeploymentDialog(@Nonnull Project project, ResourceDeployment deployment) {
        super(project, false);
        setModal(true);
        setTitle("Update Deployment");
        this.project = project;
        this.deployment = deployment;
        lblTemplateHover.setHyperlinkText("Browse for samples");
        lblTemplateHover.setHyperlinkTarget(ARM_DOC);
        lblTemplateHover.addHyperlinkListener((e) -> {
            EventUtil.logEvent(EventType.info, TelemetryConstants.ARM, BROWSE_TEMPLATE_SAMPLES, null);
        });
        initListeners();
        setData(this.deployment);
        init();
    }

    protected void initListeners() {
        templateTextField.addActionListener(
            UIUtils.createFileChooserListener(templateTextField, project,
                FileChooserDescriptorFactory.createSingleLocalFileDescriptor()));
        parametersTextField.addActionListener(
            UIUtils.createFileChooserListener(parametersTextField, project,
                FileChooserDescriptorFactory.createSingleLocalFileDescriptor()));
    }

    private void setData(@Nonnull final ResourceDeployment deployment) {
        final List<Subscription> subscriptions = az(AzureAccount.class).account().getSelectedSubscriptions();
        final Map<String, Subscription> sidMap = subscriptions.stream().collect(Collectors.toMap(Subscription::getId, s -> s));
        if (sidMap.containsKey(deployment.getSubscriptionId())) {
            subsNameLabel.setText(sidMap.get(deployment.getSubscriptionId()).getName());
        }
        rgNameLabel.setText(deployment.getResourceGroupName());
        deploymentNameLabel.setText(deployment.getName());
    }

    @Override
    protected void doOKAction() {
        final StatusBar statusBar = WindowManager.getInstance().getStatusBar(this.project);
        final String deploymentName = this.deployment.getName();
        final AzureString title = AzureOperationBundle.title("arm.update_deployment.deployment", deploymentName);
        AzureTaskManager.getInstance().runInBackground(title, false, () -> {
            final ResourceDeploymentDraft draft = (ResourceDeploymentDraft) this.deployment.update();
            final String templatePath = templateTextField.getText();
            final String parametersPath = parametersTextField.getText();
            try {
                if (!StringUtils.isEmpty(templatePath)) {
                    final String templateAsJson = IOUtils.toString(new FileReader(templatePath));
                    draft.setTemplateAsJson(templateAsJson);
                }
                if (!StringUtils.isEmpty(parametersPath)) {
                    final String parametersAsJson = IOUtils.toString(new FileReader(parametersPath));
                    draft.setParametersAsJson(parametersAsJson);
                }
                draft.commit();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        close(OK_EXIT_CODE, true);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }
}
