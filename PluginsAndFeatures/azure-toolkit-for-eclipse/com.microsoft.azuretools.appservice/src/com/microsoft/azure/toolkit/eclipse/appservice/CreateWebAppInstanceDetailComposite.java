/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.appservice;

import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.azure.toolkit.eclipse.appservice.platform.RuntimeComboBox;
import com.microsoft.azure.toolkit.eclipse.common.component.AzureTextInput;
import com.microsoft.azure.toolkit.eclipse.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.entity.CheckNameAvailabilityResultEntity;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;

public class CreateWebAppInstanceDetailComposite extends Composite {
    private final RuntimeComboBox cbRuntime;
    private final RegionComboBox cbRegion;
    private AzureTextInput text;
    private Subscription subscription;

    public RegionComboBox getRegionComboBox() {
        return cbRegion;
    }

    public RuntimeComboBox getRuntimeComboBox() {
        return cbRuntime;
    }

    public AzureTextInput getWebAppNameInput() {
        return text;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    /**
     * Create the composite.
     *
     * @param parent
     * @param style
     */
    public CreateWebAppInstanceDetailComposite(Composite parent, int style) {
        super(parent, style);
        setLayout(new GridLayout(3, false));

        Label lblNewLabel = new Label(this, SWT.NONE);
        GridData newLabelGrid = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        newLabelGrid.minimumWidth = 95;
        newLabelGrid.widthHint = 95;
        lblNewLabel.setLayoutData(newLabelGrid);
        lblNewLabel.setText("Name:");

        text = new AzureTextInput(this, SWT.BORDER);
        text.setRequired(true);
        text.setLabeledBy(lblNewLabel);
        text.addValidator(this::validateAppName);
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label azureSuffixLabel = new Label(this, SWT.NONE);
        azureSuffixLabel.setText(".azurewebsites.net");
        azureSuffixLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));

        Label lblPlatform = new Label(this, SWT.NONE);
        lblPlatform.setText("Platform:");

        cbRuntime = new RuntimeComboBox(this);
        cbRuntime.setRequired(true);
        cbRuntime.setLabeledBy(lblPlatform);
        cbRuntime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        Label lblRegion = new Label(this, SWT.NONE);
        lblRegion.setText("Region:");

        cbRegion = new RegionComboBox(this);
        cbRegion.setRequired(true);
        cbRegion.setLabeledBy(lblRegion);
        cbRegion.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

    }

    public String getAppName() {
        return text.getText();
    }

    public RuntimeConfig getRuntime() {
        final Runtime runtime = cbRuntime.getValue();
        return new RuntimeConfig().os(runtime.getOperatingSystem()).javaVersion(runtime.getJavaVersion())
                .webContainer(runtime.getWebContainer());
    }

    public Region getResourceRegion() {
        return cbRegion.getValue();
    }

    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(text, cbRuntime, cbRegion);
    }

    private AzureValidationInfo validateAppName() {
        if (subscription == null) {
            return AzureValidationInfo.success(text);
        }
        final CheckNameAvailabilityResultEntity result = Azure.az(AzureAppService.class)
                .checkNameAvailability(subscription.getId(), text.getValue());
        return result.isAvailable() ? AzureValidationInfo.success(text)
                : AzureValidationInfo.error(result.getUnavailabilityMessage(), text);
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }
}
