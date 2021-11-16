/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.appservice.serviceplan;

import com.microsoft.azure.toolkit.eclipse.appservice.PricingTierCombobox;
import com.microsoft.azure.toolkit.eclipse.common.component.AzureTextInput;
import com.microsoft.azure.toolkit.eclipse.common.component.AzureDialog;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ServicePlanCreationDialog extends AzureDialog<DraftServicePlan> implements AzureForm<DraftServicePlan> {
    private static final String APP_SERVICE_PLAN_NAME_PATTERN = "[a-zA-Z0-9\\-]{1,40}";
    private AzureTextInput text;
    private PricingTierCombobox pricingTierCombobox;
    private DraftServicePlan data;
    private List<PricingTier> pricingTiers = null;

    public ServicePlanCreationDialog(Shell parentShell) {
        super(parentShell);
        setShellStyle(SWT.CLOSE | SWT.MIN | SWT.MAX | SWT.RESIZE);
    }

    /**
     * Create contents of the dialog.
     *
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        GridLayout gridLayout = (GridLayout) container.getLayout();
        gridLayout.numColumns = 2;
        gridLayout.marginWidth = 5;

        Label lblNewLabel = new Label(container, SWT.WRAP);
        GridData lblNewLabelGrid = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
        lblNewLabelGrid.widthHint = 160;
        lblNewLabel.setLayoutData(lblNewLabelGrid);
        lblNewLabel.setText("App Service plan pricing tier determines the location, features, cost and compute resources associated with your app.");

        Label lblName = new Label(container, SWT.NONE);
        lblName.setText("Name:");

        text = new AzureTextInput(container, SWT.BORDER);
        text.setRequired(true);
        text.addValidator(() -> validateAppServicePlanName());
        GridData textGrid = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        textGrid.widthHint = 257;
        text.setLabeledBy(lblName);
        text.setLayoutData(textGrid);

        Label lblPricingTier = new Label(container, SWT.NONE);
        lblPricingTier.setText("Pricing tier:");

        pricingTierCombobox = new PricingTierCombobox(container, this.pricingTiers);
        pricingTierCombobox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        pricingTierCombobox.setLabeledBy(lblPricingTier);

        return container;
    }

    public void setPricingTier(List<PricingTier> pricingTiers) {
        this.pricingTiers = pricingTiers;
        if (pricingTierCombobox != null && pricingTierCombobox.isEnabled()) {
            pricingTierCombobox.refreshItems();
        }

    }

    public DraftServicePlan getData() {
        return data;
    }

    protected void buttonPressed(int buttonId) {
        if (buttonId == IDialogConstants.OK_ID) {
            this.data = new DraftServicePlan(text.getText(), pricingTierCombobox.getValue());
        }
        super.buttonPressed(buttonId);
    }

    @Override
    protected String getDialogTitle() {
        return "New App Service Plan";
    }

    @Override
    public AzureForm<DraftServicePlan> getForm() {
        return this;
    }

    @Override
    public DraftServicePlan getValue() {
        return data;
    }

    @Override
    public void setValue(DraftServicePlan draft) {
        Optional.ofNullable(draft).ifPresent(value -> {
            text.setValue(value.getName());
            pricingTierCombobox.setValue(value.getPricingTier());
        });
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(text, pricingTierCombobox);
    }

    private AzureValidationInfo validateAppServicePlanName() {
        final String appServicePlan = text.getValue();
        if (!appServicePlan.matches(APP_SERVICE_PLAN_NAME_PATTERN)) {
            return AzureValidationInfo.error(AzureMessageBundle.message("appService.servicePlan.validate.invalidName",
                    APP_SERVICE_PLAN_NAME_PATTERN).toString(), text);
        }
        return AzureValidationInfo.success(text);
    }
}
