/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
