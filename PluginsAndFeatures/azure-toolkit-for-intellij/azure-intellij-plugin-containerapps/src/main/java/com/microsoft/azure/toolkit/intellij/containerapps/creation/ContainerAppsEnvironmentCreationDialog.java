/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.creation;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.TitledSeparator;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.labels.LinkLabel;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.intellij.containerapps.component.WorkloadProfileCreationDialog;
import com.microsoft.azure.toolkit.intellij.containerapps.component.WorkloadProfilesTable;
import com.microsoft.azure.toolkit.intellij.monitor.view.left.WorkspaceComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironmentDraft;
import com.microsoft.azure.toolkit.lib.containerapps.model.EnvironmentType;
import com.microsoft.azure.toolkit.lib.containerapps.model.WorkloadProfile;
import com.microsoft.azure.toolkit.lib.monitor.*;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class ContainerAppsEnvironmentCreationDialog extends AzureDialog<ContainerAppsEnvironmentDraft.Config> implements AzureForm<ContainerAppsEnvironmentDraft.Config> {
    private static final Pattern CONTAINER_APPS_ENVIRONMENT_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9\\-]{0,30}[a-z0-9]$");
    private static final String CONTAINER_APPS_ENVIRONMENT_NAME_VALIDATION_MESSAGE = "A name must consist of lower case alphanumeric characters or '-', start with an alphabetic character, and end with an alphanumeric character and cannot have '--'. The length must not be more than 32 characters.";
    public static final String WORKLOAD_PROFILE_DESCRIPTION = "Dedicated workload profiles enable you to run your apps on custom hardware. You can control costs by adjusting the minimum and maximum workload profile instance count. <a href=\"https://go.microsoft.com/fwlink/?linkid=2226081\">Learn More about workload profiles \u2197</a>";

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
    private JPanel pnlWorkloadProfiles;
    private TitledSeparator titleWorkloadProfiles;
    private JRadioButton rdoWorkloadProfile;
    private JRadioButton rdoConsumptionOnly;
    private JPanel pnlProfiles;
    private JBLabel lblWorkloadProfiles;

    private WorkloadProfilesTable workloadProfilesTable;

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
        result.setEnvironmentType(rdoConsumptionOnly.isSelected() ? EnvironmentType.ConsumptionOnly : EnvironmentType.WorkloadProfiles);
        if (result.getEnvironmentType() == EnvironmentType.WorkloadProfiles) {
            result.setWorkloadProfiles(workloadProfilesTable.getValue());
        }
        final LogAnalyticsWorkspaceModule workspaceModule = Azure.az(AzureLogAnalyticsWorkspace.class).logAnalyticsWorkspaces(result.getSubscription().getId());
        final LogAnalyticsWorkspaceConfig workspaceConfig = cbWorkspace.getValue();
        final LogAnalyticsWorkspace workspace;
        if (workspaceConfig.isNewCreate()) {
            workspace = workspaceModule.create(workspaceConfig.getName(), result.getResourceGroup().getResourceGroupName());
            ((LogAnalyticsWorkspaceDraft) workspace).setRegion(result.getRegion());
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
        rdoConsumptionOnly.setSelected(data.getEnvironmentType() == EnvironmentType.ConsumptionOnly);
        rdoWorkloadProfile.setSelected(data.getEnvironmentType() == EnvironmentType.WorkloadProfiles);
        if (data.getEnvironmentType() == EnvironmentType.WorkloadProfiles) {
            workloadProfilesTable.setValue(data.getWorkloadProfiles());
        }
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
        final ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(rdoWorkloadProfile);
        buttonGroup.add(rdoConsumptionOnly);
        rdoWorkloadProfile.addItemListener(ignore -> toggleEnvironmentType());
        rdoConsumptionOnly.addItemListener(ignore -> toggleEnvironmentType());

        this.cbWorkspace.setUsePreferredSizeAsMinimum(false);
        this.cbSubscription.setRequired(true);
        this.cbResourceGroup.setRequired(true);
        this.cbResourceGroup.setUsePreferredSizeAsMinimum(false);
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

        this.lblWorkloadProfiles.setAllowAutoWrapping(true);
        this.lblWorkloadProfiles.setCopyable(true);
        this.lblWorkloadProfiles.setText(WORKLOAD_PROFILE_DESCRIPTION);
    }

    private void toggleEnvironmentType() {
        this.titleWorkloadProfiles.setVisible(rdoWorkloadProfile.isSelected());
        this.pnlProfiles.setVisible(rdoWorkloadProfile.isSelected());
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

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.workloadProfilesTable = new WorkloadProfilesTable();
        final AnAction btnAdd = new AnActionButton(message("common.add"), AllIcons.General.Add) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                final Subscription subs = cbSubscription.getValue();
                final Region region = cbRegion.getValue();
                if (Objects.nonNull(subs) && Objects.nonNull(region)) {
                    final WorkloadProfileCreationDialog creationDialog = new WorkloadProfileCreationDialog(subs.getId(), region);
                    creationDialog.setValue(WorkloadProfileCreationDialog.DEFAULT_VALUE);
                    if (creationDialog.showAndGet()) {
                        final WorkloadProfile value = creationDialog.getValue();
                        workloadProfilesTable.addWorkloadProfile(value);
                    }
                }
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }
        };
        btnAdd.registerCustomShortcutSet(KeyEvent.VK_ADD, InputEvent.ALT_DOWN_MASK, workloadProfilesTable);

        final AnAction btnRemove = new AnActionButton(message("common.remove"), AllIcons.General.Remove) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                workloadProfilesTable.removeSelectedProfile();
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.BGT;
            }

            @Override
            public boolean isEnabled() {
                return workloadProfilesTable.getSelectedRow() > 0;
            }
        };
        btnRemove.registerCustomShortcutSet(KeyEvent.VK_SUBTRACT, InputEvent.ALT_DOWN_MASK, workloadProfilesTable);

        final ToolbarDecorator tableToolbarDecorator = ToolbarDecorator.createDecorator(workloadProfilesTable)
                .addExtraActions(btnAdd, btnRemove)
                .setMinimumSize(new Dimension(-1, 120))
                .setToolbarPosition(ActionToolbarPosition.TOP);
        this.pnlWorkloadProfiles = tableToolbarDecorator.createPanel();
    }
}
