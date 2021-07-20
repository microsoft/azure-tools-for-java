/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.HyperlinkLabel;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentMode;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments.DeploymentNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.FileReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.BROWSE_TEMPLATE_SAMPLES;
import static com.microsoft.azure.toolkit.intellij.arm.action.UpdateDeploymentAction.NOTIFY_UPDATE_DEPLOYMENT_FAIL;
import static com.microsoft.azure.toolkit.intellij.arm.action.UpdateDeploymentAction.NOTIFY_UPDATE_DEPLOYMENT_SUCCESS;

public class UpdateDeploymentForm extends DeploymentBaseForm {

    private JPanel contentPane;
    private JLabel subsNameLabel;
    private JLabel rgNameLabel;
    private JLabel deploymentNameLabel;
    private HyperlinkLabel lblTemplateHover;
    private Project project;
    private final DeploymentNode deploymentNode;
    private TextFieldWithBrowseButton templateTextField;
    private JLabel lblResourceParameters;
    private TextFieldWithBrowseButton parametersTextField;
    private StatusBar statusBar;

    public UpdateDeploymentForm(Project project, DeploymentNode deploymentNode) {
        super(project, false);
        setModal(true);
        setTitle("Update Deployment");
        this.project = project;
        statusBar = WindowManager.getInstance().getStatusBar(project);
        this.deploymentNode = deploymentNode;
        lblTemplateHover.setHyperlinkText("Browse for samples");
        lblTemplateHover.setHyperlinkTarget(ARM_DOC);
        lblTemplateHover.addHyperlinkListener((e) -> {
            EventUtil.logEvent(EventType.info, TelemetryConstants.ARM, BROWSE_TEMPLATE_SAMPLES, null);
        });
        initTemplateComponent();
        fill();
        init();
    }

    @Override
    protected void doOKAction() {
        String deploymentName = deploymentNode.getDeployment().name();
        final AzureString title = AzureOperationBundle.title("arm|deployment.update", deploymentName);
        AzureTaskManager.getInstance().runInBackground(new AzureTask(project, title, false, () -> {
            EventUtil.executeWithLog(TelemetryConstants.ARM, TelemetryConstants.UPDATE_DEPLOYMENT, (operation -> {
                Deployment.Update update = deploymentNode.getDeployment().update();

                String templatePath = templateTextField.getText();
                update = update.withTemplate(IOUtils.toString(new FileReader(templatePath)));

                String parametersPath = parametersTextField.getText();
                if (!StringUtils.isEmpty(parametersPath)) {
                    String parameters = IOUtils.toString(new FileReader(parametersPath));
                    update = update.withParameters(DeploymentUtils.parseParameters(parameters));
                }
                update.withMode(DeploymentMode.INCREMENTAL).apply();
                UIUtils.showNotification(statusBar, NOTIFY_UPDATE_DEPLOYMENT_SUCCESS, MessageType.INFO);
            }), (e) -> {
                UIUtils.showNotification(statusBar, NOTIFY_UPDATE_DEPLOYMENT_FAIL + ", " + e.getMessage(),
                                         MessageType.ERROR);
            });
        }));
        close(OK_EXIT_CODE, true);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    private void fill() {
        final List<Subscription> subscriptions = az(AzureAccount.class).account().getSelectedSubscriptions();
        final Map<String, Subscription> sidMap = subscriptions.stream().collect(Collectors.toMap(Subscription::getId, s -> s));
        if (sidMap.containsKey(deploymentNode.getSubscriptionId())) {
            subsNameLabel.setText(sidMap.get(deploymentNode.getSubscriptionId()).getName());
        }
        rgNameLabel.setText(deploymentNode.getDeployment().resourceGroupName());
        deploymentNameLabel.setText(deploymentNode.getDeployment().name());
    }

    protected void initTemplateComponent() {
        templateTextField.addActionListener(
                UIUtils.createFileChooserListener(templateTextField, project,
                        FileChooserDescriptorFactory.createSingleLocalFileDescriptor()));
        parametersTextField.addActionListener(
                UIUtils.createFileChooserListener(parametersTextField, project,
                        FileChooserDescriptorFactory.createSingleLocalFileDescriptor()));
    }

}
