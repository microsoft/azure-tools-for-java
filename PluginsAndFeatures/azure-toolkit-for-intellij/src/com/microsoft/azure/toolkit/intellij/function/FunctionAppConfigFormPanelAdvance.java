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

package com.microsoft.azure.toolkit.intellij.function;

import com.intellij.openapi.project.Project;
import com.intellij.ui.DocumentAdapter;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceInfoAdvancedPanel;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceMonitorPanel;
import com.microsoft.azure.toolkit.intellij.appservice.insights.ApplicationInsightsComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.lib.appservice.ApplicationInsightsConfig;
import com.microsoft.azure.toolkit.lib.appservice.MonitorConfig;
import com.microsoft.azure.toolkit.lib.appservice.Platform;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.function.FunctionAppConfig;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import org.apache.commons.collections.ListUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.Arrays;
import java.util.List;

public class FunctionAppConfigFormPanelAdvance extends JPanel implements AzureFormPanel<FunctionAppConfig> {
    private JTabbedPane tabPane;
    private JPanel pnlRoot;
    private Project project;
    private AppServiceInfoAdvancedPanel<FunctionAppConfig> appServiceConfigPanelAdvanced;
    private AppServiceMonitorPanel appServiceMonitorPanel;
    private JPanel pnlMonitoring;
    private JPanel pnlAppService;

    private ApplicationInsightsConfig insightsConfig;

    public FunctionAppConfigFormPanelAdvance(final Project project) {
        this.project = project;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    @Override
    public void setVisible(final boolean visible) {
        pnlRoot.setVisible(visible);
    }

    @Override
    public FunctionAppConfig getData() {
        final FunctionAppConfig data = appServiceConfigPanelAdvanced.getData();
        data.setMonitorConfig(appServiceMonitorPanel.getData());
        return data;
    }

    @Override
    public void setData(final FunctionAppConfig data) {
        appServiceConfigPanelAdvanced.setData(data);
        appServiceMonitorPanel.setData(data.getMonitorConfig());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return ListUtils.union(appServiceConfigPanelAdvanced.getInputs(), appServiceMonitorPanel.getInputs());
    }

    private void init() {
        appServiceConfigPanelAdvanced.getTextName().getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull final DocumentEvent documentEvent) {
                // ai name pattern is the subset of function name pattern, so no need to validate the ai instance name
                insightsConfig.setName(appServiceConfigPanelAdvanced.getTextName().getValue());
                ApplicationInsightsComboBox insightsComboBox = appServiceMonitorPanel.getApplicationInsightsComboBox();
                insightsComboBox.removeItem(insightsConfig);
                insightsComboBox.setValue(insightsConfig);
            }
        });
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        appServiceConfigPanelAdvanced = new AppServiceInfoAdvancedPanel(project, () -> FunctionAppConfig.builder().build());
        appServiceConfigPanelAdvanced.setValidPlatform(Arrays.asList(Platform.AzureFunction.values()));
        try {
            final List<PricingTier> validPricing = AzureFunctionMvpModel.getInstance().listFunctionPricingTier();
            appServiceConfigPanelAdvanced.setValidPricingTier(validPricing, AzureFunctionMvpModel.CONSUMPTION_PRICING_TIER);
        } catch (IllegalAccessException e) {
            // swallow exceptions while load pricing tier
        }
        // Function does not support file deployment
        appServiceConfigPanelAdvanced.setDeploymentVisible(false);
        insightsConfig = ApplicationInsightsConfig.builder().newCreate(true)
                                                  .name(appServiceConfigPanelAdvanced.getTextName().getValue())
                                                  .build();

        appServiceMonitorPanel = new AppServiceMonitorPanel(project);
        appServiceMonitorPanel.setWebServerLogVisible(false);
        appServiceMonitorPanel.setData(MonitorConfig.builder().applicationInsightsConfig(insightsConfig).build());

        appServiceConfigPanelAdvanced.getSelectorSubscription().addActionListener(event -> {
            appServiceMonitorPanel.getApplicationInsightsComboBox().setSubscription(appServiceConfigPanelAdvanced.getSelectorSubscription().getValue());
        });

        appServiceConfigPanelAdvanced.getSelectorPlatform().addActionListener(event -> {
            final Platform platform = appServiceConfigPanelAdvanced.getSelectorPlatform().getValue();
            appServiceMonitorPanel.setApplicationLogVisible(platform.getOs() == OperatingSystem.WINDOWS);
        });
    }
}
