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

package com.microsoft.intellij.forms.arm;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.HyperlinkLabel;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentMode;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskRunner;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.intellij.helpers.arm.DeploymentUtils;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.deployments.DeploymentNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.FileReader;
import java.util.Map;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.BROWSE_TEMPLATE_SAMPLES;
import static com.microsoft.intellij.serviceexplorer.azure.arm.UpdateDeploymentAction.NOTIFY_UPDATE_DEPLOYMENT_FAIL;
import static com.microsoft.intellij.serviceexplorer.azure.arm.UpdateDeploymentAction.NOTIFY_UPDATE_DEPLOYMENT_SUCCESS;

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
        AzureTaskRunner.getInstance().runInBackground(new AzureTask(project, "Update your azure resource " + deploymentName + "...", false, () -> {
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
        close(DialogWrapper.OK_EXIT_CODE, true);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    private void fill() {
        Map<String, Subscription> sidMap = AzureModel.getInstance().getSidToSubscriptionMap();
        if (sidMap.containsKey(deploymentNode.getSubscriptionId())) {
            subsNameLabel.setText(sidMap.get(deploymentNode.getSubscriptionId()).displayName());
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
