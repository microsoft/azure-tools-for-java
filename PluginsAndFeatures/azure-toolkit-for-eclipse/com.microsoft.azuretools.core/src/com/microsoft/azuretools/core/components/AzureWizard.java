/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved. 
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files 
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, 
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR 
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.azuretools.core.components;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Composite;

import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

public abstract class AzureWizard extends Wizard{
	protected AzureWizard() {
		super();
	}
	
	public void sendTelemetryOnAction(final String action) {
		Map<String, String> properties = new HashMap<>();
		properties.put("WizardStep", this.getContainer().getCurrentPage().getClass().getSimpleName());
		properties.put("Action", action);
		properties.put("Title", this.getContainer().getCurrentPage().getName());
		
		if(this instanceof TelemetryProperties) {
			properties.putAll(((TelemetryProperties) this).toProperties());
		}
		
		AppInsightsClient.createByType(AppInsightsClient.EventType.WizardStep, this.getClass().getSimpleName(), action, properties);
	}
	
	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		IWizardPage nextPage = super.getNextPage(page);
		if(nextPage != null) { sendTelemetryOnAction("Next"); }
		return nextPage;
	}
	
	@Override
	public IWizardPage getPreviousPage(IWizardPage page) {
		IWizardPage previousPage = super.getPreviousPage(page);
		if(previousPage != null) { sendTelemetryOnAction("Previos"); }
		return previousPage;
	}
	
	@Override
	public boolean performCancel() {
		sendTelemetryOnAction("Cancel");
        return super.performCancel();
    }
	
	@Override
	public boolean performFinish() {
		sendTelemetryOnAction("Finish");
		return true;
	}

}
