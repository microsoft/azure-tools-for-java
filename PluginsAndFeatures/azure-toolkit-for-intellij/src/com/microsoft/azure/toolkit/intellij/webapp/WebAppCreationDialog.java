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
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.toolkit.intellij.appservice.AppConfigDialog;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceInfoBasicPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.lib.webapp.WebAppConfig;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class WebAppCreationDialog extends AppConfigDialog<WebAppConfig> {
    public static final String TITLE_CREATE_WEBAPP_DIALOG = "Create Web App";
    private static final PricingTier DEFAULT_PRICING_TIER = PricingTier.BASIC_B2;
    private JPanel panel;
    private WebAppConfigFormPanelAdvance advancedForm;
    private AppServiceInfoBasicPanel<WebAppConfig> basicForm;

    public WebAppCreationDialog(Project project) {
        super(project);
        this.init();
        setFrontPanel(basicForm);
    }

    public void setDeploymentVisible(boolean visible) {
        this.advancedForm.setDeploymentVisible(visible);
        this.basicForm.setDeploymentVisible(visible);
        this.pack();
    }

    @Override
    public WebAppConfig getData() {
        return super.getData();
    }

    @Override
    protected AzureFormPanel<WebAppConfig> getAdvancedFormPanel() {
        return advancedForm;
    }

    @Override
    protected AzureFormPanel<WebAppConfig> getBasicFormPanel() {
        return basicForm;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.panel;
    }

    protected String getDialogTitle() {
        return TITLE_CREATE_WEBAPP_DIALOG;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        advancedForm = new WebAppConfigFormPanelAdvance(project);

        basicForm = new AppServiceInfoBasicPanel(project, WebAppConfig::getWebAppDefaultConfig);
        basicForm.setDeploymentVisible(false);
    }
}
