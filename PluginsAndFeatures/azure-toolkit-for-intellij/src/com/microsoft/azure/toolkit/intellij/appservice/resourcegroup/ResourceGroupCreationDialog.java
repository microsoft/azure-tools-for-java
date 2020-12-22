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

package com.microsoft.azure.toolkit.intellij.appservice.resourcegroup;

import com.intellij.ui.components.JBLabel;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.SwingUtils;
import com.microsoft.azure.toolkit.intellij.common.ValidationDebouncedTextInput;
import com.microsoft.azure.toolkit.lib.appservice.DraftResourceGroup;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo.AzureValidationInfoBuilder;
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
            ValidationUtils.validateResourceGroupName(this.subscription.subscriptionId(), this.textName.getValue());
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
        final DraftResourceGroup.DraftResourceGroupBuilder builder = DraftResourceGroup.builder();
        builder.subscription(this.subscription)
               .name(this.textName.getValue());
        return builder.build();
    }

    @Override
    public void setData(final DraftResourceGroup data) {
        this.subscription = data.getSubscription();
        this.textName.setValue(data.name());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.textName);
    }
}
