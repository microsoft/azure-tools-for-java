/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.component.resourcegroup;

import com.intellij.ui.components.JBLabel;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.DraftResourceGroup;
import com.microsoft.azure.toolkit.intellij.common.SwingUtils;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Collections;
import java.util.List;


public class ResourceGroupCreationDialog extends AzureDialog<DraftResourceGroup>
        implements AzureForm<DraftResourceGroup> {
    private Subscription subscription;
    private JBLabel labelDescription;
    private JPanel contentPanel;
    private ResourceGroupNameTextField textName;

    public ResourceGroupCreationDialog(Subscription subscription) {
        super();
        this.init();
        this.subscription = subscription;
        this.textName.setSubscription(subscription);
        SwingUtils.setTextAndEnableAutoWrap(this.labelDescription, AzureMessageBundle.message("common.resourceGroup.description").toString());
        this.pack();
    }

    @Override
    public AzureForm<DraftResourceGroup> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return AzureMessageBundle.message("common.resourceGroup.create.title").toString();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.contentPanel;
    }

    @Override
    public DraftResourceGroup getValue() {
        return new DraftResourceGroup(this.subscription, this.textName.getValue());
    }

    @Override
    public void setValue(final DraftResourceGroup data) {
        this.subscription = data.getSubscription();
        this.textName.setValue(data.getName());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.textName);
    }
}
