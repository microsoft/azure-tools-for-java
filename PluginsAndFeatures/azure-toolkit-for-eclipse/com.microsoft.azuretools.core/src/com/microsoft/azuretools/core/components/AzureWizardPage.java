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

import com.microsoft.azuretools.telemetry.AppInsightsClient;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;

public abstract class AzureWizardPage extends WizardPage{
    private static final String WIZARD_PAGE = "WizardPage";
    private static final String TITLE = "Title";
    private static final String WIZARD_DIALOG = "WizardDialog";

    protected AzureWizardPage(String pageName) {
        super(pageName);
    }

    protected AzureWizardPage(String pageName,
            String title,
            ImageDescriptor titleImage) {
        super(pageName, title, titleImage);
    }

    public void sendButtonClickedTelemetry(String action) {
        final Map<String, String> properties = new HashMap<>();
        String simpleName = this.getClass().getSimpleName();
        if (simpleName == null) {
            simpleName = "";
        }
        properties.put(WIZARD_PAGE, simpleName);
        String title = this.getTitle();
        if (title == null) {
            title = "";
        }
        properties.put(TITLE, title);
        String wizardDialog = this.getShell().getText();
        if (wizardDialog == null) {
            wizardDialog = "";
        }
        properties.put(WIZARD_DIALOG, wizardDialog);
        AppInsightsClient.createByType(AppInsightsClient.EventType.WizardStep,
                simpleName, action, properties);
    }
}
