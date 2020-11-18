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

package com.microsoft.azure.toolkit.intellij.appservice.serviceplan;

import com.intellij.ui.components.JBLabel;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.SwingUtils;
import com.microsoft.azure.toolkit.intellij.common.ValidationDebouncedTextInput;
import com.microsoft.azure.toolkit.lib.appservice.DraftServicePlan;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo.AzureValidationInfoBuilder;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.intellij.util.ValidationUtils;

import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class ServicePlanCreationDialog extends AzureDialog<DraftServicePlan>
        implements AzureForm<DraftServicePlan> {
    public static final String DESCRIPTION =
            "App Service plan pricing tier determines the location, features, cost and compute resources associated with your app.";
    public static final String DIALOG_TITLE = "New App Service Plan";
    private Subscription subscription;
    private OperatingSystem os;
    private Region region;
    private JPanel contentPanel;
    private JBLabel labelDescription;
    private ValidationDebouncedTextInput textName;
    private PricingTierComboBox comboBoxPricingTier;

    public ServicePlanCreationDialog(final Subscription subscription,
                                     OperatingSystem os,
                                     Region region,
                                     final List<PricingTier> pricingTierList, final PricingTier defaultPricingTier) {
        super();
        this.subscription = subscription;
        this.os = os;
        this.region = region;
        this.init();
        this.textName.setValidator(this::validateName);
        this.comboBoxPricingTier.setPricingTierList(pricingTierList);
        this.comboBoxPricingTier.setDefaultPricingTier(defaultPricingTier);
        SwingUtils.setTextAndEnableAutoWrap(this.labelDescription, DESCRIPTION);
        this.pack();
    }

    private AzureValidationInfo validateName() {
        try {
            ValidationUtils.validateAppServicePlanName(this.textName.getValue());
        } catch (final IllegalArgumentException e) {
            final AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            return builder.input(this.textName).type(AzureValidationInfo.Type.ERROR).message(e.getMessage()).build();
        }
        return AzureValidationInfo.OK;
    }

    @Override
    public AzureForm<DraftServicePlan> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return DIALOG_TITLE;
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.contentPanel;
    }

    @Override
    public DraftServicePlan getData() {
        final DraftServicePlan.DraftServicePlanBuilder builder = DraftServicePlan.builder();
        builder.subscription(this.subscription)
               .name(this.textName.getValue())
               .os(this.os)
               .region(this.region)
               .tier(this.comboBoxPricingTier.getValue());
        return builder.build();
    }

    @Override
    public void setData(final DraftServicePlan data) {
        this.subscription = data.getSubscription();
        this.os = data.operatingSystem();
        this.region = data.region();
        this.textName.setValue(data.name());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.textName);
    }

}
