/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice;

import com.intellij.openapi.project.Project;
import com.intellij.ui.TitledSeparator;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.ide.appservice.model.AppServiceConfig;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.platform.RuntimeComboBox;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.serviceplan.ServicePlanComboBox;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServicePlanConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupConfig;
import com.microsoft.azuretools.utils.WebAppUtils;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class AppServiceInfoAdvancedPanel<T extends AppServiceConfig> extends JPanel implements AzureFormPanel<T> {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyMMddHHmmss");
    private static final String NOT_APPLICABLE = "N/A";
    private final Project project;
    private final Supplier<? extends T> supplier;

    private JPanel contentPanel;

    private SubscriptionComboBox selectorSubscription;
    private ResourceGroupComboBox selectorGroup;

    private AppNameInput textName;
    private RuntimeComboBox selectorRuntime;
    private RegionComboBox selectorRegion;

    private JLabel textSku;
    private AzureArtifactComboBox selectorApplication;
    private ServicePlanComboBox selectorServicePlan;
    private TitledSeparator deploymentTitle;
    private JLabel lblArtifact;
    private JLabel lblSubscription;
    private JLabel lblResourceGroup;
    private JLabel lblName;
    private JLabel lblPlatform;
    private JLabel lblRegion;
    private JLabel lblAppServicePlan;
    private JLabel lblSku;

    public AppServiceInfoAdvancedPanel(final Project project, final Supplier<? extends T> supplier) {
        super();
        this.project = project;
        this.supplier = supplier;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    @Override
    public T getValue() {
        final Subscription subscription = this.selectorSubscription.getValue();
        final ResourceGroup resourceGroup = this.selectorGroup.getValue();
        final String name = this.textName.getValue();
        final Runtime runtime = this.selectorRuntime.getValue();
        final Region region = this.selectorRegion.getValue();
        final AppServicePlan servicePlan = this.selectorServicePlan.getValue();
        final AzureArtifact artifact = this.selectorApplication.getValue();

        final T config = supplier.get();
        config.setSubscription(subscription);
        config.setResourceGroup(ResourceGroupConfig.fromResource(resourceGroup));
        config.setName(name);
        config.setRuntime(runtime);
        config.setRegion(region);
        final AppServicePlanConfig planConfig = AppServicePlanConfig.fromResource(servicePlan);
        if (Objects.nonNull(planConfig) && servicePlan.isDraftForCreating()) {
            planConfig.setResourceGroupName(config.getResourceGroupName());
            planConfig.setRegion(region);
            planConfig.setOs(Objects.requireNonNull(runtime).getOperatingSystem());
        }
        config.setServicePlan(planConfig);
        if (Objects.nonNull(artifact)) {
            final AzureArtifactManager manager = AzureArtifactManager.getInstance(this.project);
            final String path = manager.getFileForDeployment(this.selectorApplication.getValue());
            config.setApplication(Paths.get(path));
        }
        return config;
    }

    @Override
    public void setValue(final T config) {
        this.selectorSubscription.setValue(config.getSubscription());
        this.textName.setValue(config.getName());
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            this.selectorGroup.setValue(Optional.ofNullable(config.getResourceGroup()).map(ResourceGroupConfig::toResource).orElse(null));
            this.selectorServicePlan.setValue(Optional.ofNullable(config.getServicePlan()).map(AppServicePlanConfig::toResource).orElse(null));
            this.selectorRuntime.setValue(config.getRuntime());
            this.selectorRegion.setValue(config.getRegion());
        });
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            this.textName,
            this.selectorSubscription,
            this.selectorGroup,
            this.selectorRuntime,
            this.selectorRegion,
            this.selectorApplication,
            this.selectorServicePlan
        };
        return Arrays.asList(inputs);
    }

    @Override
    public void setVisible(final boolean visible) {
        this.contentPanel.setVisible(visible);
        super.setVisible(visible);
    }

    public void setDeploymentVisible(boolean visible) {
        this.deploymentTitle.setVisible(visible);
        this.lblArtifact.setVisible(visible);
        this.selectorApplication.setVisible(visible);
    }

    public SubscriptionComboBox getSelectorSubscription() {
        return selectorSubscription;
    }

    public RuntimeComboBox getSelectorRuntime() {
        return selectorRuntime;
    }

    public ServicePlanComboBox getSelectorServicePlan() {
        return selectorServicePlan;
    }

    public AppNameInput getTextName() {
        return textName;
    }

    private void init() {
        final String date = DATE_FORMAT.format(new Date());
        final String defaultWebAppName = String.format("app-%s-%s", this.project.getName(), date);
        this.textName.setValue(defaultWebAppName);
        this.textSku.setBorder(JBUI.Borders.emptyLeft(5));
        this.textSku.setText(NOT_APPLICABLE);
        this.selectorServicePlan.addItemListener(this::onServicePlanChanged);
        this.selectorSubscription.addItemListener(this::onSubscriptionChanged);
        this.selectorRuntime.addItemListener(this::onRuntimeChanged);
        this.selectorRegion.addItemListener(this::onRegionChanged);
        this.textName.setRequired(true);
        this.selectorServicePlan.setRequired(true);
        this.selectorSubscription.setRequired(true);
        this.selectorGroup.setRequired(true);
        this.selectorRuntime.setRequired(true);
        this.selectorRegion.setRequired(true);

        this.lblSubscription.setLabelFor(selectorSubscription);
        this.lblResourceGroup.setLabelFor(selectorGroup);
        this.lblName.setLabelFor(textName);
        this.lblPlatform.setLabelFor(selectorRuntime);
        this.lblRegion.setLabelFor(selectorRegion);
        this.lblAppServicePlan.setLabelFor(selectorServicePlan);
        this.lblArtifact.setLabelFor(selectorApplication);
        this.selectorApplication.setFileFilter(virtualFile -> {
            final String ext = FileNameUtils.getExtension(virtualFile.getPath());
            final Runtime runtime = this.selectorRuntime.getValue();
            return StringUtils.isNotBlank(ext) && WebAppUtils.isSupportedArtifactType(runtime, ext);
        });
    }

    private void onRegionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Region region = (Region) e.getItem();
            this.selectorServicePlan.setRegion(region);
        }
    }

    private void onRuntimeChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Runtime runtime = (Runtime) e.getItem();
            final OperatingSystem operatingSystem = Objects.isNull(runtime) ? null : runtime.getOperatingSystem();
            this.selectorServicePlan.setOperatingSystem(operatingSystem);
        }
    }

    private void onSubscriptionChanged(final ItemEvent e) {
        //TODO: @wangmi try subscription mechanism? e.g. this.selectorGroup.subscribe(this.selectSubscription)
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Subscription subscription = (Subscription) e.getItem();
            this.selectorGroup.setSubscription(subscription);
            this.textName.setSubscription(subscription);
            this.selectorRegion.setSubscription(subscription);
            this.selectorServicePlan.setSubscription(subscription);
        }
    }

    private void onServicePlanChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final AppServicePlan plan = (AppServicePlan) e.getItem();
            if (plan == null || plan.getPricingTier() == null) {
                return;
            }
            final String pricing = Objects.equals(plan.getPricingTier(), PricingTier.CONSUMPTION) ?
                    "Consumption" : String.format("%s_%s", plan.getPricingTier().getTier(), plan.getPricingTier().getSize());
            this.textSku.setText(pricing);
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
            this.textSku.setText(NOT_APPLICABLE);
        }
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.selectorApplication = new AzureArtifactComboBox(project, true);
        this.selectorApplication.reloadItems();
    }

    public void setValidPricingTier(List<PricingTier> pricingTier, PricingTier defaultPricingTier) {
        selectorServicePlan.setValidPricingTierList(pricingTier, defaultPricingTier);
    }

    public void setValidRuntime(List<Runtime> runtimes) {
        selectorRuntime.setPlatformList(runtimes);
    }
}
