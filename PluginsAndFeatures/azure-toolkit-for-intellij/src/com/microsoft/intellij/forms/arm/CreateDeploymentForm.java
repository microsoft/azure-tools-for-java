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
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.HyperlinkLabel;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.Deployment.DefinitionStages.WithTemplate;
import com.microsoft.azure.management.resources.DeploymentMode;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.intellij.helpers.arm.DeploymentUtils;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.intellij.ui.util.UIUtils.ElementWrapper;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.ResourceManagementNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.FileReader;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.BROWSE_TEMPLATE_SAMPLES;
import static com.microsoft.intellij.serviceexplorer.azure.arm.CreateDeploymentAction.NOTIFY_CREATE_DEPLOYMENT_FAIL;
import static com.microsoft.intellij.serviceexplorer.azure.arm.CreateDeploymentAction.NOTIFY_CREATE_DEPLOYMENT_SUCCESS;

public class CreateDeploymentForm extends DeploymentBaseForm {

    private static final String DUPLICATED_DEPLOYMENT_NAME = "A deployment with the same name already exists";

    private JPanel contentPane;
    private JTextField rgNameTextFiled;
    private JComboBox rgNameCb;
    private JRadioButton createNewRgButton;
    private JRadioButton useExistingRgButton;
    private JTextField deploymentNameTextField;
    private JComboBox regionCb;
    private JLabel usingExistRgRegionLabel;
    private JLabel usingExistRgRegionDetailLabel;
    private JLabel createNewRgRegionLabel;
    private JComboBox subscriptionCb;
    private TextFieldWithBrowseButton templateTextField;
    private HyperlinkLabel lblTemplateHover;
    private TextFieldWithBrowseButton parametersTextField;
    private Project project;
    private StatusBar statusBar;
    private String rgName;
    private String deploymentName;

    public CreateDeploymentForm(Project project) {
        super(project, false);
        this.project = project;
        statusBar = WindowManager.getInstance().getStatusBar(project);
        setModal(true);
        setTitle("Create Deployment");

        final ButtonGroup resourceGroup = new ButtonGroup();
        resourceGroup.add(createNewRgButton);
        resourceGroup.add(useExistingRgButton);
        useExistingRgButton.setSelected(true);
        createNewRgButton.addItemListener((e) -> radioRgLogic());
        useExistingRgButton.addItemListener((e) -> radioRgLogic());

        rgNameCb.addActionListener((l) -> {
            if (rgNameCb.getSelectedItem() != null) {
                ResourceGroup rg = ((ElementWrapper<ResourceGroup>) rgNameCb.getSelectedItem()).getValue();
                usingExistRgRegionDetailLabel.setText(rg.region().label());
            }
        });
        subscriptionCb.addActionListener((l) -> {
            fillResourceGroup();
            fillRegion();
        });

        lblTemplateHover.setHyperlinkText("Browse for samples");
        lblTemplateHover.setHyperlinkTarget(ARM_DOC);
        lblTemplateHover.addHyperlinkListener((e) -> {
            EventUtil.logEvent(EventType.info, TelemetryConstants.ARM, BROWSE_TEMPLATE_SAMPLES, null);
        });

        initTemplateComponent();
        radioRgLogic();
        initCache();
        fill();
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        deploymentName = deploymentNameTextField.getText();
        ProgressManager.getInstance().run(new Task.Backgroundable(project,
            "Deploying your azure resource " + deploymentName + "...", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                EventUtil.executeWithLog(TelemetryConstants.ARM, TelemetryConstants.CREATE_DEPLOYMENT, (operation -> {
                    SubscriptionDetail subs = (SubscriptionDetail) subscriptionCb.getSelectedItem();
                    Azure azure = AuthMethodManager.getInstance().getAzureClient(subs.getSubscriptionId());
                    WithTemplate template;
                    if (createNewRgButton.isSelected()) {
                        rgName = rgNameTextFiled.getText();
                        template = azure
                            .deployments().define(deploymentName)
                            .withNewResourceGroup(rgNameTextFiled.getText(),
                                ((ElementWrapper<Region>) regionCb.getSelectedItem()).getValue());
                    } else {
                        ResourceGroup rg = ((ElementWrapper<ResourceGroup>) rgNameCb.getSelectedItem()).getValue();
                        List<ResourceEx<Deployment>> deployments = AzureMvpModel.getInstance()
                                .getDeploymentByRgName(subs.getSubscriptionId(), rg.name());
                        boolean isExist = deployments.parallelStream()
                                .anyMatch(deployment -> deployment.getResource().name().equals(deploymentName));
                        if (isExist) {
                            throw new RuntimeException(DUPLICATED_DEPLOYMENT_NAME);
                        }
                        rgName = rg.name();
                        template = azure.deployments().define(deploymentName).withExistingResourceGroup(rg);
                    }

                    String fileText = templateTextField.getText();
                    String content = IOUtils.toString(new FileReader(fileText));
                    String parametersPath = parametersTextField.getText();
                    String parameters = StringUtils.isEmpty(parametersPath) ? "{}" :
                            IOUtils.toString(new FileReader(parametersPath));
                    parameters = DeploymentUtils.parseParameters(parameters);
                    template.withTemplate(content)
                            .withParameters(parameters)
                            .withMode(DeploymentMode.INCREMENTAL)
                            .create();

                    UIUtils.showNotification(statusBar, NOTIFY_CREATE_DEPLOYMENT_SUCCESS, MessageType.INFO);
                    updateUI();
                }), (ex) -> {
                        UIUtils.showNotification(statusBar, NOTIFY_CREATE_DEPLOYMENT_FAIL + ", " + ex.getMessage(),
                            MessageType.ERROR);
                        updateUI();
                    });
            }
        });
        close(DialogWrapper.OK_EXIT_CODE, true);
    }

    public void filleSubsAndRg(ResourceManagementNode node) {
        selectSubs(node.getSid());
        fillResourceGroup();
        UIUtils.selectByText(rgNameCb, node.getRgName());
        radioRgLogic();
    }

    protected void initTemplateComponent() {
        templateTextField.addActionListener(
            UIUtils.createFileChooserListener(templateTextField, project,
                FileChooserDescriptorFactory.createSingleLocalFileDescriptor()));
        parametersTextField.addActionListener(
                UIUtils.createFileChooserListener(parametersTextField, project,
                        FileChooserDescriptorFactory.createSingleLocalFileDescriptor()));
    }

    private void fill() {
        Map<SubscriptionDetail, List<ResourceGroup>> srgMap = AzureModel.
            getInstance().getSubscriptionToResourceGroupMap();

        for (SubscriptionDetail sd : srgMap.keySet()) {
            subscriptionCb.addItem(sd);
        }
        if (subscriptionCb.getItemCount() > 0) {
            subscriptionCb.setSelectedIndex(0);
        }

        deploymentNameTextField.setText("deployment" + System.currentTimeMillis());
        rgNameTextFiled.setText("resouregroup" + System.currentTimeMillis());
    }

    private void initCache() {
        Map<SubscriptionDetail, List<Location>> subscription2Location =
                AzureModel.getInstance().getSubscriptionToLocationMap();
        if (subscription2Location == null) {
            ProgressManager.getInstance().run(new Task.Modal(project, "Loading Available Locations...", false) {
                @Override
                public void run(ProgressIndicator indicator) {
                    try {
                        AzureModelController.updateSubscriptionMaps(null);
                    } catch (Exception ex) {
                        AzurePlugin.log("Error loading locations", ex);
                    }
                }
            });
        }
    }

    private void fillRegion() {
        List<Location> locations = AzureModel.getInstance().getSubscriptionToLocationMap()
            .get(subscriptionCb.getSelectedItem()).stream().sorted(Comparator.comparing(Location::displayName)).
                collect(Collectors.toList());
        regionCb.removeAllItems();
        for (Location location : locations) {
            Region region = location.region();
            ElementWrapper<Region> item = new ElementWrapper<>(region.label(), region);
            regionCb.addItem(item);
            if (region == Region.EUROPE_WEST) {
                regionCb.setSelectedItem(item);
            }
        }
    }

    private void updateUI() {
        AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, rgName));
    }

    private void fillResourceGroup() {
        Map<SubscriptionDetail, List<ResourceGroup>> srgMap = AzureModel
            .getInstance().getSubscriptionToResourceGroupMap();
        rgNameCb.removeAllItems();
        for (SubscriptionDetail sd : srgMap.keySet()) {
            if (sd == subscriptionCb.getSelectedItem()) {
                for (ResourceGroup rg : srgMap.get(sd)) {
                    rgNameCb.addItem(new ElementWrapper<>(rg.name(), rg));
                }
                break;
            }
        }
    }

    private void radioRgLogic() {
        boolean isCreateNewRg = createNewRgButton.isSelected();
        rgNameTextFiled.setVisible(isCreateNewRg);
        regionCb.setVisible(isCreateNewRg);
        createNewRgRegionLabel.setVisible(isCreateNewRg);

        rgNameCb.setVisible(!isCreateNewRg);
        usingExistRgRegionLabel.setVisible(!isCreateNewRg);
        usingExistRgRegionDetailLabel.setVisible(!isCreateNewRg);
        pack();
    }

    private void selectSubs(String targetSid) {
        for (int i = 0; i < subscriptionCb.getItemCount(); i++) {
            if (((SubscriptionDetail) subscriptionCb.getItemAt(i)).getSubscriptionId().equals(targetSid)) {
                subscriptionCb.setSelectedIndex(i);
                break;
            }
        }
    }
}
