/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceInfoBasicPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.ConfigDialog;
import com.microsoft.azure.toolkit.lib.appservice.ApplicationInsightsConfig;
import com.microsoft.azure.toolkit.lib.appservice.MonitorConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.function.FunctionAppConfig;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import javax.swing.*;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class FunctionAppCreationDialog extends ConfigDialog<FunctionAppConfig> {

    private JPanel contentPane;
    private AppServiceInfoBasicPanel<FunctionAppConfig> basicPanel;
    private FunctionAppConfigFormPanelAdvance advancePanel;

    public FunctionAppCreationDialog(final Project project) {
        super(project);
        this.init();
        setFrontPanel(basicPanel);
    }

    @Override
    protected AzureFormPanel<FunctionAppConfig> getAdvancedFormPanel() {
        return advancePanel;
    }

    @Override
    protected AzureFormPanel<FunctionAppConfig> getBasicFormPanel() {
        return basicPanel;
    }

    @Override
    protected String getDialogTitle() {
        return message("function.create.dialog.title");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        basicPanel = new AppServiceInfoBasicPanel<>(project, FunctionAppConfig::getFunctionAppDefaultConfig) {
            @Override
            public FunctionAppConfig getData() {
                // Create AI instance with same name by default
                final FunctionAppConfig config = super.getData();
                final MonitorConfig monitorConfig = MonitorConfig.builder().build();
                monitorConfig.setApplicationInsightsConfig(ApplicationInsightsConfig.builder().name(config.getName()).newCreate(true).build());
                config.setMonitorConfig(monitorConfig);
                return config;
            }
        };
        basicPanel.getSelectorRuntime().setPlatformList(Runtime.FUNCTION_APP_RUNTIME);
        advancePanel = new FunctionAppConfigFormPanelAdvance(project);
    }
}
