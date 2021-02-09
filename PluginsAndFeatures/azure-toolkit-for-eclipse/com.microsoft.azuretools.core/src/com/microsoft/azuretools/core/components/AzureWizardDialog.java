/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.components;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.TelemetryProperties;

public class AzureWizardDialog extends WizardDialog{
    public AzureWizardDialog(Shell parentShell, IWizard newWizard) {
        super(parentShell, newWizard);
    }

    public void sendTelemetryOnAction(final String action) {
        Map<String, String> properties = new HashMap<>();
        properties.put("WizardStep", this.getCurrentPage().getClass().getSimpleName());
        properties.put("Action", action);
        properties.put("Title", this.getCurrentPage().getName());

        if(this.getWizard() instanceof TelemetryProperties) {
            properties.putAll(((TelemetryProperties) this.getWizard()).toProperties());
        }

        AppInsightsClient.createByType(AppInsightsClient.EventType.WizardStep, this.getClass().getSimpleName(), action, properties);
    }

    @Override
    protected void nextPressed() {
        IWizardPage page = getCurrentPage().getNextPage();
        if (page == null) {
            // something must have happened getting the next page
            return;
        }
        sendTelemetryOnAction("Next");
        super.nextPressed();
    }

    @Override
    protected void backPressed() {
        IWizardPage page = getCurrentPage().getPreviousPage();
        if (page == null) {
            // should never happen since we have already visited the page
            return;
        }
        sendTelemetryOnAction("Previos");
        super.backPressed();
    }

    @Override
    protected void cancelPressed() {
        sendTelemetryOnAction("Cancel");
        super.cancelPressed();
    }

    @Override
    protected void finishPressed() {
        sendTelemetryOnAction("Finish");
        super.finishPressed();
    }



}
