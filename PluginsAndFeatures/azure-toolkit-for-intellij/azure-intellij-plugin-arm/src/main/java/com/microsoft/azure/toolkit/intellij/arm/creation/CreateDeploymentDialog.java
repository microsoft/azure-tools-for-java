/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm.creation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.SimpleListCellRenderer;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceDeploymentDraft;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupDraft;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.ui.util.UIUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;

public class CreateDeploymentDialog extends AzureDialogWrapper {
    public static final String ERROR_CREATING_DEPLOYMENT = "Error creating Deployment";
    public static final String NOTIFY_CREATE_DEPLOYMENT_SUCCESS = "Create deployment successfully";
    public static final String NOTIFY_CREATE_DEPLOYMENT_FAIL = "Create deployment failed";
    public static final String ARM_DOC = "https://azure.microsoft.com/en-us/resources/templates/";

    private static final String DUPLICATED_DEPLOYMENT_NAME = "A deployment with the same name already exists";
    private final Project project;

    private JPanel contentPane;
    private JTextField rgNameTextFiled;
    private JComboBox<ResourceGroup> resourceGroupCombobox;
    private JRadioButton createNewRgButton;
    private JRadioButton useExistingRgButton;
    private JTextField deploymentNameTextField;
    private RegionComboBox regionCb;
    private JLabel usingExistRgRegionLabel;
    private JLabel usingExistRgRegionDetailLabel;
    private JLabel createNewRgRegionLabel;
    private SubscriptionComboBox subscriptionCombobox;
    private TextFieldWithBrowseButton templateTextField;
    private HyperlinkLabel lblTemplateHover;
    private TextFieldWithBrowseButton parametersTextField;

    public CreateDeploymentDialog(@Nonnull final Project project, @Nullable ResourceGroup group) {
        super(project, false);
        this.project = project;
        setModal(true);
        setTitle("Create Deployment");

        useExistingRgButton.setSelected(true);
        this.resourceGroupCombobox.setRenderer(new SimpleListCellRenderer<>() {
            @Override
            public void customize(JList list, ResourceGroup rg, int i, boolean b, boolean b1) {
                if (rg != null) {
                    setText(rg.getName());
                }
            }
        });
        lblTemplateHover.setHyperlinkText("Browse for samples");
        lblTemplateHover.setHyperlinkTarget(ARM_DOC);
        deploymentNameTextField.setText("deployment" + System.currentTimeMillis());
        rgNameTextFiled.setText("resouregroup" + System.currentTimeMillis());
        initListeners();
        updateResourceGroupPanel();
        init();
        this.setResourceGroup(group);
    }

    private void initListeners() {
        createNewRgButton.addItemListener((e) -> updateResourceGroupPanel());
        useExistingRgButton.addItemListener((e) -> updateResourceGroupPanel());
        resourceGroupCombobox.addActionListener((l) -> {
            if (resourceGroupCombobox.getSelectedItem() != null) {
                final ResourceGroup rg = (ResourceGroup) resourceGroupCombobox.getSelectedItem();
                usingExistRgRegionDetailLabel.setText(rg.getRegion().getLabel());
            }
        });
        subscriptionCombobox.addItemListener((l) -> reloadResourceGroupItems());
        templateTextField.addActionListener(
            UIUtils.createFileChooserListener(templateTextField, project, FileChooserDescriptorFactory.createSingleLocalFileDescriptor()));
        parametersTextField.addActionListener(
            UIUtils.createFileChooserListener(parametersTextField, project, FileChooserDescriptorFactory.createSingleLocalFileDescriptor()));
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        final String deploymentName = deploymentNameTextField.getText();
        final StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
        final AzureString title = AzureOperationBundle.title("arm.create_deployment.deployment", deploymentName);
        final Subscription subs = (Subscription) subscriptionCombobox.getSelectedItem();
        final String parametersPath = parametersTextField.getText();
        final String templatePath = templateTextField.getText();
        final IAzureMessager messager = AzureMessager.getMessager();
        AzureTaskManager.getInstance().runInBackground(title, false, () -> {
            if (Objects.isNull(subs)) {
                // TODO: migrate to AzureFormInput.validate
                messager.error("\"Subscription\" is required to create a deployment.");
                return;
            }
            final ResourceGroup group;
            if (createNewRgButton.isSelected()) {
                final String rgName = rgNameTextFiled.getText();
                if (StringUtils.isEmpty(rgName)) {
                    // TODO: migrate to AzureFormInput.validate
                    messager.error("\"name\" is required to create a resource group.");
                    return;
                }
                final Region region = (Region) regionCb.getSelectedItem();
                group = Azure.az(AzureResources.class).groups(subs.getId()).create(rgName, rgName);
                ((ResourceGroupDraft) group).setRegion(region);
            } else {
                final Object selectedResourceGroup = resourceGroupCombobox.getSelectedItem();
                if (Objects.isNull(selectedResourceGroup)) {
                    // TODO: migrate to AzureFormInput.validate
                    messager.error("\"Resource group\" is required to create a deployment.");
                    return;
                }
                group = (ResourceGroup) selectedResourceGroup;
            }
            try {
                if (StringUtils.isBlank(templatePath)) {
                    // TODO: migrate to AzureFormInput.validate
                    messager.error("\"Resource Template\" is required to create a deployment.");
                    return;
                }
                createDeployment(deploymentName, parametersPath, templatePath, messager, group);
            } catch (final Throwable e) {
                throw new AzureToolkitRuntimeException(e);
            }
        });
    }

    @AzureOperation(name = "arm.create_deployment.deployment", params = {"deploymentName"}, type = AzureOperation.Type.ACTION)
    private void createDeployment(String deploymentName, String parametersPath, String templatePath, IAzureMessager messager, ResourceGroup group)
        throws IOException {
        final String template = IOUtils.toString(new FileReader(templatePath));
        String parameters = "{}";
        if (!StringUtils.isEmpty(parametersPath)) {
            final String origin = IOUtils.toString(new FileReader(parametersPath));
            final Gson gson = new Gson();
            final JsonElement parametersElement = gson.fromJson(origin, JsonElement.class).getAsJsonObject().get("parameters");
            parameters = parametersElement == null ? origin : parametersElement.toString();
        }
        AzureTaskManager.getInstance().runLater(() -> close(DialogWrapper.OK_EXIT_CODE, true));
        final ResourceDeploymentDraft draft = group.deployments().create(deploymentName, group.getName());
        draft.setTemplateAsJson(template);
        draft.setParametersAsJson(parameters);
        draft.commit();
        messager.success(NOTIFY_CREATE_DEPLOYMENT_SUCCESS);
    }

    private void setResourceGroup(@Nullable ResourceGroup group) {
        if (Objects.isNull(group)) {
            return;
        }
        subscriptionCombobox.setValue(new AzureComboBox.ItemReference<>(group.getSubscriptionId(), Subscription::getId));
        reloadResourceGroupItems();
        UIUtils.selectByText(resourceGroupCombobox, group.getName());
        updateResourceGroupPanel();
    }

    private void reloadResourceGroupItems() {
        if (subscriptionCombobox.getSelectedItem() == null) {
            return;
        }
        final String sid = ((Subscription) subscriptionCombobox.getSelectedItem()).getId();
        resourceGroupCombobox.removeAllItems();
        Azure.az(AzureResources.class).groups(sid).list().forEach(g -> resourceGroupCombobox.addItem(g));
    }

    private void fillSubscription(String targetSid) {
        if (subscriptionCombobox.getItemCount() == 0) {
            subscriptionCombobox.setValue(Azure.az(AzureAccount.class).account().getSubscription(targetSid), true);
        }
        for (int i = 0; i < subscriptionCombobox.getItemCount(); i++) {
            if (subscriptionCombobox.getItemAt(i).getId().equals(targetSid)) {
                subscriptionCombobox.setSelectedIndex(i);
                break;
            }
        }
    }

    private void updateResourceGroupPanel() {
        final boolean isCreateNewRg = createNewRgButton.isSelected();
        rgNameTextFiled.setVisible(isCreateNewRg);
        regionCb.setVisible(isCreateNewRg);
        regionCb.setSubscription((Subscription) this.subscriptionCombobox.getSelectedItem());
        createNewRgRegionLabel.setVisible(isCreateNewRg);

        resourceGroupCombobox.setVisible(!isCreateNewRg);
        usingExistRgRegionLabel.setVisible(!isCreateNewRg);
        usingExistRgRegionDetailLabel.setVisible(!isCreateNewRg);
        pack();
    }

    private void createUIComponents() {
        this.subscriptionCombobox = new SubscriptionComboBox();
        this.regionCb = new RegionComboBox();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return this.deploymentNameTextField;
    }
}
