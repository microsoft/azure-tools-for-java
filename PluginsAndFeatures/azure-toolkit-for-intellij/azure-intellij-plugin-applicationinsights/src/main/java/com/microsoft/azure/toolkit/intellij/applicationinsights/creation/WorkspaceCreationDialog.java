/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.applicationinsights.creation;

import com.intellij.ui.components.JBLabel;
import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.ObjectUtils;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.SwingUtils;
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.workspace.AzureLogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.applicationinsights.workspace.LogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.applicationinsights.workspace.LogAnalyticsWorkspaceConfig;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Collections;
import java.util.List;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class WorkspaceCreationDialog extends AzureDialog<LogAnalyticsWorkspaceConfig>
    implements AzureForm<LogAnalyticsWorkspaceConfig> {
    private String subscriptionId;
    // todo support change resource group
    private ResourceGroup resourceGroup;
    private JPanel contentPanel;
    private JBLabel labelDescription;
    private AzureTextInput textName;
    // todo not support change region
    private RegionComboBox regionComboBox;

    public WorkspaceCreationDialog(final Subscription subscription,
                                   final ResourceGroup resourceGroup, final Region region) {
        super();
        this.subscriptionId = subscription.getId();
        this.resourceGroup = resourceGroup;
        this.init();
        this.textName.addValidator(this::validateName);
        this.regionComboBox.setValue(region);
        SwingUtils.setTextAndEnableAutoWrap(this.labelDescription, message("workspace.create.description"));
        this.pack();
    }

    private AzureValidationInfo validateName() {
        final String workspaceName = this.textName.getValue();
        if (workspaceName.length() > 63 || workspaceName.length() < 4) {
            return AzureValidationInfo.error(message("workspace.name.validate.length"), this);
        }
        if (workspaceName.startsWith("-") || workspaceName.endsWith("-")) {
            return AzureValidationInfo.error(message("workspace.name.validate.beginEndSymbol"), this);
        }
        if (workspaceName.replaceAll("[0-9-a-zA-Z]", "").length() > 0) {
            return AzureValidationInfo.error(message("workspace.name.validate.symbol"), this);
        }
        if (ObjectUtils.allNotNull(subscriptionId, resourceGroup)) {
            final LogAnalyticsWorkspace workspace = Azure.az(AzureLogAnalyticsWorkspace.class)
                    .logAnalyticsWorkspaces(subscriptionId).get(workspaceName, resourceGroup.getName());
            if (workspace != null && workspace.exists()) {
                return AzureValidationInfo.error(message("workspace.name.validate.unique"), this);
            }
        }
        return AzureValidationInfo.success(this);
    }

    @Override
    public AzureForm<LogAnalyticsWorkspaceConfig> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return message("workspace.create.title");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.contentPanel;
    }

    @Override
    public LogAnalyticsWorkspaceConfig getValue() {
        return LogAnalyticsWorkspaceConfig.builder()
                .newCreate(true).subscriptionId(this.subscriptionId).name(textName.getValue()).build();
    }

    @Override
    public void setValue(final LogAnalyticsWorkspaceConfig data) {
        this.subscriptionId = data.getSubscriptionId();
        this.textName.setValue(data.getName());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.textName);
    }

}
