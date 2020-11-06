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
import com.microsoft.azure.toolkit.intellij.appservice.AppConfigDialog;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceInfoBasicPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.lib.appservice.*;
import com.microsoft.azure.toolkit.lib.function.FunctionAppConfig;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import javax.swing.*;
import java.util.Arrays;

public class FunctionAppCreationDialog extends AppConfigDialog<FunctionAppConfig> {

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
        return "Create Function";
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        basicPanel = new AppServiceInfoBasicPanel<FunctionAppConfig>(project, FunctionAppConfig::getFunctionAppDefaultConfig) {
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
        basicPanel.getSelectorPlatform().setPlatformList(Arrays.asList(Platform.AzureFunction.values()));
        advancePanel = new FunctionAppConfigFormPanelAdvance(project);
    }
}
