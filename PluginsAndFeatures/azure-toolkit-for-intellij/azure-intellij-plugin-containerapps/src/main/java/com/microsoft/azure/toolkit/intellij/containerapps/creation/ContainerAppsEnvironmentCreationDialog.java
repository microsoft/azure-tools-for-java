/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.creation;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.monitor.view.left.WorkspaceComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.monitor.AzureLogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspaceConfig;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspaceDraft;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspaceModule;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironmentDraft;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContainerAppsEnvironmentCreationDialog extends AzureDialog<ContainerAppsEnvironmentDraft.Config> implements AzureForm<ContainerAppsEnvironmentDraft.Config> {
    private static final Pattern CONTAINER_APPS_ENVIRONMENT_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9\\-]{0,30}[a-z0-9]$");
    private static final String CONTAINER_APPS_ENVIRONMENT_NAME_VALIDATION_MESSAGE = "A name must consist of lower case alphanumeric characters or '-', start with an alphabetic character, and end with an alphanumeric character and cannot have '--'. The length must not be more than 32 characters.";

    private JLabel lblSubscription;
    private SubscriptionComboBox cbSubscription;
    private JLabel lblResourceGroup;
    private ResourceGroupComboBox cbResourceGroup;
    private JLabel lblEnvironmentName;
    private AzureTextInput txtEnvironmentName;
    private JPanel pnlRoot;
    private WorkspaceComboBox cbWorkspace;
    private JLabel lblRegion;
    private RegionComboBox cbRegion;

    public ContainerAppsEnvironmentCreationDialog(final Project project) {
        super(project);
        $$$setupUI$$$();
        init();
    }

    @Override
    public AzureForm<ContainerAppsEnvironmentDraft.Config> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Container Apps Environment";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public ContainerAppsEnvironmentDraft.Config getValue() {
        final ContainerAppsEnvironmentDraft.Config result = new ContainerAppsEnvironmentDraft.Config();
        result.setSubscription(cbSubscription.getValue());
        result.setResourceGroup(cbResourceGroup.getValue());
        result.setRegion(cbRegion.getValue());
        result.setName(txtEnvironmentName.getValue());
        final LogAnalyticsWorkspaceModule workspaceModule = Azure.az(AzureLogAnalyticsWorkspace.class).logAnalyticsWorkspaces(result.getSubscription().getId());
        final LogAnalyticsWorkspaceConfig workspaceConfig = cbWorkspace.getValue();
        final LogAnalyticsWorkspace workspace;
        if (workspaceConfig.isNewCreate()) {
            workspace = workspaceModule.create(workspaceConfig.getName(), result.getResourceGroup().getResourceGroupName());
            ((LogAnalyticsWorkspaceDraft)workspace).setRegion(result.getRegion());
        } else {
            workspace = workspaceModule.get(workspaceConfig.getResourceId());
        }
        result.setLogAnalyticsWorkspace(workspace);
        return result;
    }

    @Override
    public void setValue(ContainerAppsEnvironmentDraft.Config data) {
        Optional.ofNullable(data.getSubscription()).ifPresent(cbSubscription::setValue);
        Optional.ofNullable(data.getResourceGroup()).ifPresent(cbResourceGroup::setValue);
        Optional.ofNullable(data.getName()).ifPresent(txtEnvironmentName::setValue);
        Optional.ofNullable(data.getRegion()).ifPresent(cbRegion::setValue);
        Optional.ofNullable(data.getLogAnalyticsWorkspace())
                .map(this::convertLogAnalyticsWorkspaceToConfig)
                .ifPresent(cbWorkspace::setValue);
    }

    public void setSubscription(final Subscription subscription, final boolean fixed) {
        if (Objects.nonNull(subscription)) {
            this.cbSubscription.setValue(subscription, fixed);
        }
    }

    public void setResourceGroup(final ResourceGroup resourceGroup, final boolean fixed) {
        if (Objects.nonNull(resourceGroup)) {
            this.cbResourceGroup.setValue(resourceGroup, fixed);
        }
    }

    public void setRegion(final Region region, final boolean fixed) {
        if (Objects.nonNull(region)) {
            this.cbRegion.setValue(region, fixed);
        }
    }

    private LogAnalyticsWorkspaceConfig convertLogAnalyticsWorkspaceToConfig(final LogAnalyticsWorkspace workspace) {
        return workspace.isDraftForCreating() ? LogAnalyticsWorkspaceConfig.builder().newCreate(true).name(workspace.getName())
                .subscriptionId(workspace.getSubscriptionId()).regionName(workspace.getRegion().getName()).build() :
                LogAnalyticsWorkspaceConfig.builder().newCreate(false).resourceId(workspace.getId()).build();
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtEnvironmentName, cbSubscription, cbResourceGroup, cbWorkspace);
    }

    public void init() {
        super.init();
        this.cbSubscription.setRequired(true);
        this.cbResourceGroup.setRequired(true);
        this.cbRegion.setRequired(true);
        this.txtEnvironmentName.setRequired(true);
        this.txtEnvironmentName.addValidator(this::validateContainerAppsEnvironmentName);

        this.cbSubscription.addItemListener(this::onSubscriptionChanged);
        this.cbRegion.addItemListener(this::onRegionChanged);

        this.lblSubscription.setLabelFor(cbSubscription);
        this.lblResourceGroup.setLabelFor(cbResourceGroup);
        this.lblEnvironmentName.setLabelFor(txtEnvironmentName);
        this.lblRegion.setLabelFor(cbRegion);
        this.lblSubscription.setIcon(AllIcons.General.ContextHelp);
        this.lblResourceGroup.setIcon(AllIcons.General.ContextHelp);
    }

    private AzureValidationInfo validateContainerAppsEnvironmentName() {
        final String name = txtEnvironmentName.getValue();
        final Matcher matcher = CONTAINER_APPS_ENVIRONMENT_NAME_PATTERN.matcher(name);
        return matcher.matches() && !StringUtils.contains(name, "--") ? AzureValidationInfo.success(txtEnvironmentName) :
                AzureValidationInfo.error(CONTAINER_APPS_ENVIRONMENT_NAME_VALIDATION_MESSAGE, txtEnvironmentName);

    }

    private void onRegionChanged(ItemEvent itemEvent) {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED && itemEvent.getItem() instanceof Region) {
            final Region region = (Region) itemEvent.getItem();
            this.cbWorkspace.setRegion(region);
        }
    }

    private void onSubscriptionChanged(ItemEvent itemEvent) {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED && itemEvent.getItem() instanceof Subscription) {
            final Subscription subscription = (Subscription) itemEvent.getItem();
            this.cbResourceGroup.setSubscription(subscription);
            this.cbRegion.setSubscription(subscription);
            this.cbWorkspace.setSubscription(subscription);
        }
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
