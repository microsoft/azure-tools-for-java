/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.SimpleListCellRenderer;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.Deployment.DefinitionStages.WithTemplate;
import com.microsoft.azure.management.resources.DeploymentMode;
import com.microsoft.azure.toolkit.intellij.appservice.region.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.tooling.msservices.serviceexplorer.azure.arm.ResourceManagementNode;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.io.FileReader;
import java.util.List;

import static com.microsoft.azure.toolkit.intellij.arm.action.CreateDeploymentAction.NOTIFY_CREATE_DEPLOYMENT_FAIL;
import static com.microsoft.azure.toolkit.intellij.arm.action.CreateDeploymentAction.NOTIFY_CREATE_DEPLOYMENT_SUCCESS;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.BROWSE_TEMPLATE_SAMPLES;

public class CreateDeploymentForm extends DeploymentBaseForm {

    private static final String DUPLICATED_DEPLOYMENT_NAME = "A deployment with the same name already exists";

    private JPanel contentPane;
    private JTextField rgNameTextFiled;
    private JComboBox rgNameCb;
    private JRadioButton createNewRgButton;
    private JRadioButton useExistingRgButton;
    private JTextField deploymentNameTextField;
    private com.microsoft.azure.toolkit.intellij.appservice.region.RegionComboBox regionCb;
    private JLabel usingExistRgRegionLabel;
    private JLabel usingExistRgRegionDetailLabel;
    private JLabel createNewRgRegionLabel;
    private SubscriptionComboBox subscriptionCb;
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
                ResourceGroup rg = (ResourceGroup) rgNameCb.getSelectedItem();
                usingExistRgRegionDetailLabel.setText(Region.fromName(rg.getRegion()).getLabel());
            }
        });
        subscriptionCb.addActionListener((l) -> {
            fillResourceGroup();
        });

        this.rgNameCb.setRenderer(new SimpleListCellRenderer<ResourceGroup>() {
            @Override
            public void customize(JList list, ResourceGroup rg, int i, boolean b, boolean b1) {
                if (rg != null) {
                    setText(rg.getName());
                }
            }
        });

        lblTemplateHover.setHyperlinkText("Browse for samples");
        lblTemplateHover.setHyperlinkTarget(ARM_DOC);
        lblTemplateHover.addHyperlinkListener((e) -> {
            EventUtil.logEvent(EventType.info, TelemetryConstants.ARM, BROWSE_TEMPLATE_SAMPLES, null);
        });

        initTemplateComponent();
        radioRgLogic();
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
        final AzureString title = AzureOperationBundle.title("arm|deployment.deploy", deploymentName);
        AzureTaskManager.getInstance().runInBackground(new AzureTask(project, title, false, () -> {
            EventUtil.executeWithLog(TelemetryConstants.ARM, TelemetryConstants.CREATE_DEPLOYMENT, (operation -> {
                Subscription subs = (Subscription) subscriptionCb.getSelectedItem();
                com.microsoft.azure.management.Azure azure = AuthMethodManager.getInstance().getAzureClient(subs.getId());
                WithTemplate template;
                if (createNewRgButton.isSelected()) {
                    rgName = rgNameTextFiled.getText();
                    final Region region = (Region) regionCb.getSelectedItem();
                    template = azure
                        .deployments().define(deploymentName)
                        .withNewResourceGroup(rgNameTextFiled.getText(),
                            com.microsoft.azure.management.resources.fluentcore.arm.Region.fromName(region.getName()));
                } else {
                    ResourceGroup rg = (ResourceGroup) rgNameCb.getSelectedItem();
                    List<ResourceEx<Deployment>> deployments = AzureMvpModel.getInstance()
                                                                            .getDeploymentByRgName(subs.getId(), rg.getName());
                    boolean isExist = deployments.parallelStream()
                                                 .anyMatch(deployment -> deployment.getResource().name().equals(deploymentName));
                    if (isExist) {
                        throw new RuntimeException(DUPLICATED_DEPLOYMENT_NAME);
                    }
                    rgName = rg.getName();
                    template = azure.deployments().define(deploymentName).withExistingResourceGroup(rg.getName());
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
        }));
        close(DialogWrapper.OK_EXIT_CODE, true);
    }

    public void fillSubsAndRg(ResourceManagementNode node) {
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

    private void createUIComponents() {
        this.subscriptionCb = new SubscriptionComboBox();
        this.regionCb = new RegionComboBox();
    }

    private void fill() {
        deploymentNameTextField.setText("deployment" + System.currentTimeMillis());
        rgNameTextFiled.setText("resouregroup" + System.currentTimeMillis());
    }

    private void updateUI() {
        AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, rgName));
    }

    private void fillResourceGroup() {
        if (subscriptionCb.getSelectedItem() == null) {
            return;
        }
        String sid = ((Subscription) subscriptionCb.getSelectedItem()).getId();
        rgNameCb.removeAllItems();
        for (ResourceEx<ResourceGroup> rg : AzureMvpModel.getInstance().getResourceGroups(sid)) {
            rgNameCb.addItem(rg.getResource());
        }
    }

    private void radioRgLogic() {
        boolean isCreateNewRg = createNewRgButton.isSelected();
        rgNameTextFiled.setVisible(isCreateNewRg);
        regionCb.setVisible(isCreateNewRg);
        regionCb.setSubscription((Subscription) this.subscriptionCb.getSelectedItem());
        createNewRgRegionLabel.setVisible(isCreateNewRg);

        rgNameCb.setVisible(!isCreateNewRg);
        usingExistRgRegionLabel.setVisible(!isCreateNewRg);
        usingExistRgRegionDetailLabel.setVisible(!isCreateNewRg);
        pack();
    }

    private void selectSubs(String targetSid) {
        if (subscriptionCb.getItemCount() == 0) {
            subscriptionCb.setValue(Azure.az(AzureAccount.class).account().getSubscription(targetSid), true);
        }
        for (int i = 0; i < subscriptionCb.getItemCount(); i++) {
            if (subscriptionCb.getItemAt(i).getId().equals(targetSid)) {
                subscriptionCb.setSelectedIndex(i);
                break;
            }
        }
    }
}
