/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.appservice;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azure.toolkit.eclipse.appservice.serviceplan.DraftServicePlan;
import com.microsoft.azure.toolkit.eclipse.common.component.AzureComboBox.ItemReference;
import com.microsoft.azure.toolkit.eclipse.common.component.AzureDialog;
import com.microsoft.azure.toolkit.eclipse.common.component.SubscriptionAndResourceGroupComposite;
import com.microsoft.azure.toolkit.eclipse.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.eclipse.common.component.resourcegroup.DraftResourceGroup;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

public class CreateWebAppDialog extends AzureDialog<AppServiceConfig> implements AzureForm<AppServiceConfig> {
    private CreateWebAppInstanceDetailComposite instanceDetailPanel;
    private SubscriptionAndResourceGroupComposite subsAndResourceGroupPanel;
    private CreateWebAppAppServicePlanComposite appServicePlanPanel;
    private AppServiceConfig config;

    /**
     * Create the dialog.
     *
     * @param parentShell
     */
    public CreateWebAppDialog(Shell parentShell, AppServiceConfig config) {
        super(parentShell);
        this.config = config;
        setShellStyle(SWT.SHELL_TRIM);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);

        Group grpProjectDetails = new Group(container, SWT.NONE);
        grpProjectDetails.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        grpProjectDetails.setText("Project Details");
        grpProjectDetails.setLayout(new FillLayout(SWT.HORIZONTAL));

        subsAndResourceGroupPanel = new SubscriptionAndResourceGroupComposite(grpProjectDetails, SWT.NONE);

        Group grpInstanceDetails = new Group(container, SWT.NONE);
        grpInstanceDetails.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        grpInstanceDetails.setText("Instance Details");
        grpInstanceDetails.setLayout(new FillLayout(SWT.HORIZONTAL));
        instanceDetailPanel = new CreateWebAppInstanceDetailComposite(grpInstanceDetails, SWT.NONE);

        Group grpAppServicePlan = new Group(container, SWT.NONE);
        grpAppServicePlan.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        grpAppServicePlan.setText("App Service Plan");
        grpAppServicePlan.setLayout(new FillLayout(SWT.HORIZONTAL));

        appServicePlanPanel = new CreateWebAppAppServicePlanComposite(grpAppServicePlan, SWT.NONE);

        SubscriptionComboBox subscriptionComboBox = subsAndResourceGroupPanel.getSubscriptionComboBox();
        subscriptionComboBox.addValueChangedListener(event -> {
            Subscription subs = subscriptionComboBox.getValue();
            instanceDetailPanel.setSubscription(subs);
            instanceDetailPanel.getRegionComboBox().setSubscription(subs);
            appServicePlanPanel.getServicePlanCombobox().setSubscription(subs);
        });
        instanceDetailPanel.getRegionComboBox().addValueChangedListener(event -> {
            Region region = instanceDetailPanel.getRegionComboBox().getValue();
            appServicePlanPanel.getServicePlanCombobox().setAzureRegion(region);
        });

        instanceDetailPanel.getRuntimeComboBox().addValueChangedListener(event -> {
            Runtime r = instanceDetailPanel.getRuntimeComboBox().getValue();
            if (r != null) {
                appServicePlanPanel.getServicePlanCombobox().setOs(r.getOperatingSystem());
            }

        });
        subscriptionComboBox.refreshItems();
        return container;
    }

    @Override
    public AppServiceConfig getValue() {
        final AppServicePlanEntity entity = appServicePlanPanel.getServicePlan();
        return new AppServiceConfig().subscriptionId(subsAndResourceGroupPanel.getSubscription().getId())
                .resourceGroup(subsAndResourceGroupPanel.getResourceGroup().getName())
                .appName(instanceDetailPanel.getAppName())
                .region(instanceDetailPanel.getResourceRegion())
                .runtime(instanceDetailPanel.getRuntime())
                .servicePlanName(entity.getName())
                .servicePlanResourceGroup(StringUtils.firstNonBlank(entity.getResourceGroup(),
                        subsAndResourceGroupPanel.getResourceGroup().getName()))
                .pricingTier(entity.getPricingTier());
    }

    @Override
    public void setValue(AppServiceConfig config) {
        Optional.ofNullable(config.subscriptionId()).ifPresent(
                subscription -> subsAndResourceGroupPanel.getSubscriptionComboBox().setValue(new ItemReference<>(
                        value -> StringUtils.equalsIgnoreCase(value.getId(), config.subscriptionId()))));
        // todo: refactor config to support draft value
        Optional.ofNullable(config.resourceGroup()).ifPresent(resourceGroup -> subsAndResourceGroupPanel
                .getResourceGroupComboBox().setValue(new DraftResourceGroup(resourceGroup)));
        Optional.ofNullable(config.appName())
                .ifPresent(name -> instanceDetailPanel.getWebAppNameInput().setValue(name));
        Optional.ofNullable(config.region())
                .ifPresent(region -> instanceDetailPanel.getRegionComboBox().setValue(region));
        Optional.ofNullable(config.runtime())
                .ifPresent(runtime -> instanceDetailPanel.getRuntimeComboBox()
                        .setValue(new ItemReference<>(value -> Objects.equals(runtime.os(), value.getOperatingSystem())
                                && Objects.equals(runtime.webContainer(), value.getWebContainer())
                                && Objects.equals(runtime.javaVersion(), value.getJavaVersion()))));
        Optional.ofNullable(config.getServicePlanConfig())
                .ifPresent(servicePlan -> appServicePlanPanel.getServicePlanCombobox()
                        .setValue(new DraftServicePlan(servicePlan.servicePlanName(), servicePlan.pricingTier())));
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Stream.of(subsAndResourceGroupPanel.getInputs(), instanceDetailPanel.getInputs(),
                appServicePlanPanel.getInputs()).flatMap(List::stream).collect(Collectors.toList());
    }

    @Override
    protected String getDialogTitle() {
        return "Create Azure Web App";
    }

    @Override
    public AzureForm<AppServiceConfig> getForm() {
        return this;
    }

    @Override
    public int open() {
        Optional.ofNullable(config).ifPresent(config -> AzureTaskManager.getInstance().runLater(() -> setValue(config)));
        return super.open();
    }

}
