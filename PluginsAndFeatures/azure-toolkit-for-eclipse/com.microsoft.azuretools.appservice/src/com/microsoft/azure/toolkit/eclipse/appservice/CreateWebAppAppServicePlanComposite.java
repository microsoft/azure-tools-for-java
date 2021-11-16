/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.appservice;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.azure.toolkit.eclipse.appservice.serviceplan.ServicePlanCombobox;
import com.microsoft.azure.toolkit.lib.appservice.entity.AppServicePlanEntity;
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;

public class CreateWebAppAppServicePlanComposite extends Composite {

    private final ServicePlanCombobox cbServicePlan;
    private final Label textSku;
    private static final String NOT_APPLICABLE = "N/A";

    public ServicePlanCombobox getServicePlanCombobox() {
        return cbServicePlan;
    }

    /**
     * Create the composite.
     *
     * @param parent
     * @param style
     */
    public CreateWebAppAppServicePlanComposite(Composite parent, int style) {
        super(parent, style);
        setLayout(new GridLayout(2, false));

        Label lblNewLabel = new Label(this, SWT.NONE);
        lblNewLabel.setText("Plan:");

        cbServicePlan = new ServicePlanCombobox(this);
        cbServicePlan.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        cbServicePlan.setRequired(true);
        cbServicePlan.setLabeledBy(lblNewLabel);

        Label lblSkuAndSize = new Label(this, SWT.NONE);
        GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gridData.minimumWidth = 95;
        gridData.widthHint = 95;
        lblSkuAndSize.setLayoutData(gridData);
        lblSkuAndSize.setText("SKU and size:");

        textSku = new Label(this, SWT.NONE);
        textSku.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));

        cbServicePlan.addValueChangedListener(plan -> {
            if (plan == null || plan.getPricingTier() == null) {
                return;
            }

            final String pricing = Objects.equals(plan.getPricingTier(), PricingTier.CONSUMPTION) ? "Consumption"
                    : String.format("%s_%s", plan.getPricingTier().getTier(), plan.getPricingTier().getSize());
            this.textSku.setText(pricing);
        });

    }

    public AppServicePlanEntity getServicePlan() {
        return cbServicePlan.getValue();
    }

    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(cbServicePlan);
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

}
