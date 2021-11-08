/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceInfoAdvancedPanel;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceMonitorPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.lib.appservice.MonitorConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.webapp.WebAppConfig;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import org.apache.commons.collections.ListUtils;

import javax.swing.*;
import java.util.List;

public class WebAppConfigFormPanelAdvance extends JPanel implements AzureFormPanel<WebAppConfig> {
    private JTabbedPane tabPane;
    private JPanel pnlRoot;
    private Project project;
    private AppServiceInfoAdvancedPanel<WebAppConfig> appServiceConfigPanelAdvanced;
    private AppServiceMonitorPanel appServiceMonitorPanel;
    private JPanel pnlMonitoring;
    private JPanel pnlAppService;

    public WebAppConfigFormPanelAdvance(final Project project) {
        this.project = project;
    }

    @Override
    public void setVisible(final boolean visible) {
        pnlRoot.setVisible(visible);
    }

    @Override
    public WebAppConfig getValue() {
        final WebAppConfig data = appServiceConfigPanelAdvanced.getValue();
        data.setMonitorConfig(appServiceMonitorPanel.getValue());
        return data;
    }

    @Override
    public void setValue(final WebAppConfig data) {
        appServiceConfigPanelAdvanced.setValue(data);
        appServiceMonitorPanel.setValue(data.getMonitorConfig());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return ListUtils.union(appServiceConfigPanelAdvanced.getInputs(), appServiceMonitorPanel.getInputs());
    }

    public void setDeploymentVisible(final boolean visible) {
        this.appServiceConfigPanelAdvanced.setDeploymentVisible(visible);
    }

    public void setValidPricingTier(final List<PricingTier> validPricingTiers, final PricingTier defaultPricingTier) {
        this.appServiceConfigPanelAdvanced.setValidPricingTier(validPricingTiers, defaultPricingTier);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        appServiceConfigPanelAdvanced = new AppServiceInfoAdvancedPanel(project, WebAppConfig::getWebAppDefaultConfig);
        final List<PricingTier> validPricing = AzureMvpModel.getInstance().listPricingTier();
        appServiceConfigPanelAdvanced.setValidPricingTier(validPricing, WebAppConfig.DEFAULT_PRICING_TIER);

        appServiceMonitorPanel = new AppServiceMonitorPanel(project);
        // Application Insights is not supported in Web App
        appServiceMonitorPanel.setApplicationInsightsVisible(false);
        appServiceMonitorPanel.setValue(MonitorConfig.builder().build());

        appServiceConfigPanelAdvanced.getSelectorRuntime().addActionListener(event -> {
            final Runtime runtime = appServiceConfigPanelAdvanced.getSelectorRuntime().getValue();
            appServiceMonitorPanel.setApplicationLogVisible(runtime != null && runtime.getOperatingSystem() == OperatingSystem.WINDOWS);
        });
    }
}
