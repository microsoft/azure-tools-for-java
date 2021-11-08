/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function;

import com.intellij.openapi.project.Project;
import com.intellij.ui.DocumentAdapter;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceInfoAdvancedPanel;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceMonitorPanel;
import com.microsoft.azure.toolkit.intellij.appservice.insights.ApplicationInsightsComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.lib.appservice.ApplicationInsightsConfig;
import com.microsoft.azure.toolkit.lib.appservice.MonitorConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.function.FunctionAppConfig;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.collections.ListUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FunctionAppConfigFormPanelAdvance extends JPanel implements AzureFormPanel<FunctionAppConfig> {
    private final Project project;
    private JTabbedPane tabPane;
    private JPanel pnlRoot;
    private AppServiceInfoAdvancedPanel<FunctionAppConfig> appServiceConfigPanelAdvanced;
    private AppServiceMonitorPanel appServiceMonitorPanel;
    private JPanel pnlMonitoring;
    private JPanel pnlAppService;

    private ApplicationInsightsConfig insightsConfig;

    public FunctionAppConfigFormPanelAdvance(final Project project) {
        super();
        this.project = project;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    @Override
    public void setVisible(final boolean visible) {
        pnlRoot.setVisible(visible);
    }

    @Override
    public FunctionAppConfig getValue() {
        final FunctionAppConfig data = appServiceConfigPanelAdvanced.getValue();
        data.setMonitorConfig(appServiceMonitorPanel.getValue());
        return data;
    }

    @Override
    public void setValue(final FunctionAppConfig data) {
        appServiceConfigPanelAdvanced.setValue(data);
        appServiceMonitorPanel.setValue(data.getMonitorConfig());
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
                final ApplicationInsightsComboBox insightsComboBox = appServiceMonitorPanel.getApplicationInsightsComboBox();
                insightsComboBox.removeItem(insightsConfig);
                insightsComboBox.setValue(insightsConfig);
            }
        });
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        appServiceConfigPanelAdvanced = new AppServiceInfoAdvancedPanel<>(project, () -> FunctionAppConfig.builder().build());
        appServiceConfigPanelAdvanced.setValidRuntime(Runtime.FUNCTION_APP_RUNTIME);
        appServiceConfigPanelAdvanced.setValidPricingTier(new ArrayList<>(PricingTier.FUNCTION_PRICING), PricingTier.CONSUMPTION);
        // Function does not support file deployment
        appServiceConfigPanelAdvanced.setDeploymentVisible(false);
        insightsConfig = ApplicationInsightsConfig.builder().newCreate(true)
                                                  .name(appServiceConfigPanelAdvanced.getTextName().getValue())
                                                  .build();

        appServiceMonitorPanel = new AppServiceMonitorPanel(project);
        appServiceMonitorPanel.setWebServerLogVisible(false);
        appServiceMonitorPanel.setValue(MonitorConfig.builder().applicationInsightsConfig(insightsConfig).build());

        appServiceConfigPanelAdvanced.getSelectorSubscription().addActionListener(event ->
                appServiceMonitorPanel.getApplicationInsightsComboBox().setSubscription(appServiceConfigPanelAdvanced.getSelectorSubscription().getValue()));

        appServiceConfigPanelAdvanced.getSelectorRuntime().addActionListener(event -> {
            final OperatingSystem operatingSystem = Optional.ofNullable(appServiceConfigPanelAdvanced.getSelectorRuntime().getValue())
                    .map(Runtime::getOperatingSystem).orElse(null);
            appServiceMonitorPanel.setApplicationLogVisible(operatingSystem == OperatingSystem.WINDOWS);
        });
    }
}
