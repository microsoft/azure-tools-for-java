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
