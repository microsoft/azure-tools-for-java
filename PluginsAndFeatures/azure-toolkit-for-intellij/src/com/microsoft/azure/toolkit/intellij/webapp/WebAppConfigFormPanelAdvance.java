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
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceInfoAdvancedPanel;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceMonitorPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.lib.appservice.MonitorConfig;
import com.microsoft.azure.toolkit.lib.appservice.Platform;
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
    public WebAppConfig getData() {
        final WebAppConfig data = appServiceConfigPanelAdvanced.getData();
        data.setMonitorConfig(appServiceMonitorPanel.getData());
        return data;
    }

    @Override
    public void setData(final WebAppConfig data) {
        appServiceConfigPanelAdvanced.setData(data);
        appServiceMonitorPanel.setData(data.getMonitorConfig());
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
        try {
            final List<PricingTier> validPricing = AzureMvpModel.getInstance().listPricingTier();
            appServiceConfigPanelAdvanced.setValidPricingTier(validPricing, WebAppConfig.DEFAULT_PRICING_TIER);
        } catch (IllegalAccessException e) {
            // swallow exceptions while load pricing tier
        }

        appServiceMonitorPanel = new AppServiceMonitorPanel(project);
        // Application Insights is not supported in Web App
        appServiceMonitorPanel.setApplicationInsightsVisible(false);
        appServiceMonitorPanel.setData(MonitorConfig.builder().build());

        appServiceConfigPanelAdvanced.getSelectorPlatform().addActionListener(event -> {
            final Platform platform = appServiceConfigPanelAdvanced.getSelectorPlatform().getValue();
            appServiceMonitorPanel.setApplicationLogVisible(platform != null && platform.getOs() == OperatingSystem.WINDOWS);
        });
    }
}
