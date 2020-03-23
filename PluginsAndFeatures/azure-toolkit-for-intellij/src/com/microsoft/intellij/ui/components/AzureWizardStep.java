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

package com.microsoft.intellij.ui.components;

import com.intellij.ui.wizard.WizardModel;
import com.intellij.ui.wizard.WizardStep;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.TelemetryProperties;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Customized WizardStep specifically for Azure Intellij Plugin.
 * So that we can perform common actions in a base-class level for example telemetry.
 * Literally all concrete WizardStep implementations should inherit from this class rather than WizardStep.
 */
public abstract class AzureWizardStep<T extends WizardModel> extends WizardStep<T> {
    protected AzureWizardStep() {
    }

    public AzureWizardStep(String title) {
        super(title);
    }

    public AzureWizardStep(String title, String explanation) {
        super(title, explanation);
    }

    public AzureWizardStep(String title, String explanation, Icon icon) {
        super(title, explanation, icon);
    }

    public AzureWizardStep(String title, String explanation, Icon icon, String helpId) {
        super(title, explanation, icon, helpId);
    }

    protected void sendTelemetryOnAction(final String action) {
        final Map<String, String> properties = new HashMap<>();
        properties.put("WizardStep", this.getClass().getSimpleName());
        properties.put("Action", action);
        properties.put("Title", this.getTitle());

        if (this instanceof TelemetryProperties) {
            properties.putAll(((TelemetryProperties) this).toProperties());
        }

        AppInsightsClient.createByType(AppInsightsClient.EventType.WizardStep, this.getClass().getSimpleName(), action, properties);
    }

    @Override
    public WizardStep onNext(T model) {
        sendTelemetryOnAction("Next");
        return super.onNext(model);
    }

    @Override
    public boolean onFinish() {
        sendTelemetryOnAction("Finish");
        return super.onFinish();
    }

    @Override
    public boolean onCancel() {
        sendTelemetryOnAction("Cancel");
        return super.onCancel();
    }

    @Override
    public WizardStep onPrevious(T model) {
        sendTelemetryOnAction("Previous");
        return super.onPrevious(model);
    }
}
