/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice.resourcegroup;

import com.intellij.ui.components.JBLabel;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.SwingUtils;
import com.microsoft.azure.toolkit.intellij.common.ValidationDebouncedTextInput;
import com.microsoft.azure.toolkit.lib.common.DraftResourceGroup;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo.AzureValidationInfoBuilder;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.util.ValidationUtils;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class ResourceGroupCreationDialog extends AzureDialog<DraftResourceGroup>
        implements AzureForm<DraftResourceGroup> {
    private Subscription subscription;
    private JBLabel labelDescription;
    private JPanel contentPanel;
    private ValidationDebouncedTextInput textName;

    public ResourceGroupCreationDialog(Subscription subscription) {
        super();
        this.init();
        this.subscription = subscription;
        this.textName.setValidator(this::validateName);
        SwingUtils.setTextAndEnableAutoWrap(this.labelDescription, message("appService.resourceGroup.description"));
        this.pack();
    }

    private AzureValidationInfo validateName() {
        try {
            ValidationUtils.validateResourceGroupName(this.subscription.getId(), this.textName.getValue());
        } catch (final IllegalArgumentException e) {
            final AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            return builder.input(this.textName).type(AzureValidationInfo.Type.ERROR).message(e.getMessage()).build();
        }
        return AzureValidationInfo.OK;
    }

    @Override
    public AzureForm<DraftResourceGroup> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return message("appService.resourceGroup.create.title");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.contentPanel;
    }

    @Override
    public DraftResourceGroup getData() {
        return new DraftResourceGroup(this.subscription, this.textName.getValue());
    }

    @Override
    public void setData(final DraftResourceGroup data) {
        this.subscription = data.getSubscription();
        this.textName.setValue(data.getName());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.textName);
    }
}
