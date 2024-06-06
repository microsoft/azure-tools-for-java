/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.guidance;

import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.containerapps.component.AzureContainerAppsEnvironmentComboBox;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ContainerAppEnvInput implements AzureFormJPanel<ContainerAppsEnvironment> {
    private AzureContainerAppsEnvironmentComboBox cbEnv;
    private JPanel pnlRoot;

    private Subscription subscription;

    public ContainerAppEnvInput() {
        $$$setupUI$$$();
        this.cbEnv.setRequired(true);
        this.cbEnv.setLabel("Environment");
    }

    @Override
    public ContainerAppsEnvironment getValue() {
        return cbEnv.getValue();
    }

    public void setValue(final ContainerAppsEnvironment value) {
        this.cbEnv.setValue(value);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(cbEnv);
    }

    @Override
    public JPanel getContentPanel() {
        return pnlRoot;
    }

    public void setSubscription(final Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        this.cbEnv.setSubscription(subscription);
    }
}
