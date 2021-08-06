/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.webapp;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceInfoBasicPanel;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.ConfigDialog;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.webapp.WebAppConfig;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class WebAppCreationDialog extends ConfigDialog<WebAppConfig> {
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
        return message("webapp.create.dialog.title");
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        advancedForm = new WebAppConfigFormPanelAdvance(project);

        basicForm = new AppServiceInfoBasicPanel(project, WebAppConfig::getWebAppDefaultConfig);
        basicForm.setDeploymentVisible(false);
    }
}
