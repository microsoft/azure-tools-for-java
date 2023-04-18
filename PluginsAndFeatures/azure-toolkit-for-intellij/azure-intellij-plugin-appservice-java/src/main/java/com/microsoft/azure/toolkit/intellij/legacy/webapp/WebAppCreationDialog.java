/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.ConfigDialog;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.AppServiceInfoBasicPanel;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.auth.IAccountActions;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;
import static com.microsoft.azure.toolkit.lib.Azure.az;

public class WebAppCreationDialog extends ConfigDialog<WebAppConfig> {
    private static final PricingTier DEFAULT_PRICING_TIER = PricingTier.BASIC_B2;
    private JPanel panel;
    protected WebAppConfigFormPanelAdvance advancedForm;
    protected AppServiceInfoBasicPanel<WebAppConfig> basicForm;

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

    @ExceptionNotification
    private void createUIComponents() {
        // TODO: place custom component creation code here
        advancedForm = new WebAppConfigFormPanelAdvance(project);
        final List<Subscription> selectedSubscriptions = az(AzureAccount.class).account().getSelectedSubscriptions();
        if (selectedSubscriptions.isEmpty()) {
            this.close();
            throw new AzureToolkitRuntimeException("there are no subscriptions selected in your account.", IAccountActions.SELECT_SUBS);
        }
        basicForm = new AppServiceInfoBasicPanel(project, selectedSubscriptions.get(0), () -> WebAppConfig.getWebAppDefaultConfig(project.getName()));
        basicForm.setDeploymentVisible(false);
    }
}
