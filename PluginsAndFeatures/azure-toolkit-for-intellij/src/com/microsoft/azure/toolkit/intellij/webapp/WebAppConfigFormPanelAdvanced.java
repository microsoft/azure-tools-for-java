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

package com.microsoft.azure.toolkit.intellij.webapp;

import com.intellij.openapi.project.Project;
import com.intellij.ui.TitledSeparator;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.toolkit.intellij.appservice.AppNameInput;
import com.microsoft.azure.toolkit.intellij.appservice.platform.PlatformComboBox;
import com.microsoft.azure.toolkit.intellij.appservice.region.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.appservice.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.intellij.appservice.serviceplan.ServicePlanComboBox;
import com.microsoft.azure.toolkit.intellij.appservice.subscription.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.lib.appservice.Platform;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.webapp.WebAppConfig;
import com.microsoft.intellij.ui.components.AzureArtifact;
import com.microsoft.intellij.ui.components.AzureArtifactManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.event.ItemEvent;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class WebAppConfigFormPanelAdvanced extends JPanel implements AzureFormPanel<WebAppConfig> {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyMMddHHmmss");
    private static final String NOT_APPLICABLE = "N/A";
    private final Project project;

    private JPanel contentPanel;

    private SubscriptionComboBox selectorSubscription;
    private ResourceGroupComboBox selectorGroup;

    private AppNameInput textName;
    private PlatformComboBox selectorPlatform;
    private RegionComboBox selectorRegion;

    private JLabel textSku;
    private AzureArtifactComboBox selectorApplication;
    private ServicePlanComboBox selectorServicePlan;
    private TitledSeparator deploymentTitle;
    private JLabel deploymentLabel;

    public WebAppConfigFormPanelAdvanced(final Project project) {
        super();
        this.project = project;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    @Override
    public WebAppConfig getData() {
        final Subscription subscription = this.selectorSubscription.getValue();
        final ResourceGroup resourceGroup = this.selectorGroup.getValue();
        final String name = this.textName.getValue();
        final Platform platform = this.selectorPlatform.getValue();
        final Region region = this.selectorRegion.getValue();
        final AppServicePlan servicePlan = this.selectorServicePlan.getValue();
        final AzureArtifact artifact = this.selectorApplication.getValue();

        final WebAppConfig config = WebAppConfig.builder().build();
        config.setSubscription(subscription);
        config.setResourceGroup(resourceGroup);
        config.setName(name);
        config.setPlatform(platform);
        config.setRegion(region);
        config.setServicePlan(servicePlan);
        if (Objects.nonNull(artifact)) {
            final AzureArtifactManager manager = AzureArtifactManager.getInstance(this.project);
            final String path = manager.getFileForDeployment(this.selectorApplication.getValue());
            config.setApplication(Paths.get(path));
        }
        return config;
    }

    @Override
    public void setData(final WebAppConfig config) {
        this.selectorSubscription.setValue(config.getSubscription());
        this.selectorGroup.setValue(config.getResourceGroup());
        this.textName.setValue(config.getName());
        this.selectorPlatform.setValue(config.getPlatform());
        this.selectorRegion.setValue(config.getRegion());
        this.selectorServicePlan.setValue(config.getServicePlan());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            this.selectorSubscription,
            this.selectorGroup,
            this.textName,
            this.selectorPlatform,
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
        this.deploymentLabel.setVisible(visible);
        this.selectorApplication.setVisible(visible);
    }

    private void init() {
        final String date = DATE_FORMAT.format(new Date());
        final String defaultWebAppName = String.format("app-%s-%s", this.project.getName(), date);
        this.textName.setValue(defaultWebAppName);
        this.textSku.setBorder(new EmptyBorder(0, 5, 0, 0));
        this.textSku.setText(NOT_APPLICABLE);
        this.selectorServicePlan.addItemListener(this::onServicePlanChanged);
        this.selectorSubscription.addItemListener(this::onSubscriptionChanged);
        this.selectorPlatform.addItemListener(this::onPlatformChanged);
        this.selectorRegion.addItemListener(this::onRegionChanged);
        this.textName.setRequired(true);
        this.selectorServicePlan.setRequired(true);
        this.selectorSubscription.setRequired(true);
        this.selectorPlatform.setRequired(true);
        this.selectorRegion.setRequired(true);
    }

    private void onRegionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Region region = e.getStateChange() == ItemEvent.SELECTED ? (Region) e.getItem() : null;
            this.selectorServicePlan.setRegion(region);
        }
    }

    private void onPlatformChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final OperatingSystem operatingSystem =
                e.getStateChange() == ItemEvent.SELECTED ? ((Platform) e.getItem()).getOs() : null;
            this.selectorServicePlan.setOperatingSystem(operatingSystem);
        }
    }

    private void onSubscriptionChanged(final ItemEvent e) {
        //TODO: @wangmi try subscription mechanism? e.g. this.selectorGroup.subscribe(this.selectSubscription)
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Subscription subscription =
                e.getStateChange() == ItemEvent.SELECTED ? (Subscription) e.getItem() : null;
            this.selectorGroup.setSubscription(subscription);
            this.textName.setSubscription(subscription);
            this.selectorRegion.setSubscription(subscription);
            this.selectorServicePlan.setSubscription(subscription);
        }
    }

    private void onServicePlanChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final AppServicePlan plan = (AppServicePlan) e.getItem();
            this.textSku.setText(plan.pricingTier().toString());
        } else if (e.getStateChange() == ItemEvent.DESELECTED) {
            this.textSku.setText(NOT_APPLICABLE);
        }
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.selectorApplication = new AzureArtifactComboBox(project, true);
        this.selectorApplication.refreshItems();
    }
}
