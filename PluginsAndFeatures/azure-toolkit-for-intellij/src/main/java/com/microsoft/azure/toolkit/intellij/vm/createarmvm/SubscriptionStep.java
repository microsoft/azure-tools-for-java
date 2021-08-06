/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.createarmvm;

import com.intellij.openapi.project.Project;
import com.intellij.ui.wizard.WizardNavigationState;
import com.microsoft.azure.toolkit.intellij.appservice.subscription.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.vm.VMWizardModel;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.intellij.actions.SelectSubscriptionsAction;
import com.microsoft.intellij.ui.components.AzureWizardStep;
import org.apache.commons.collections4.CollectionUtils;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.event.ItemEvent;
import java.util.Map;

public class SubscriptionStep extends AzureWizardStep<VMWizardModel> implements TelemetryProperties {
    VMWizardModel model;
    private JPanel rootPanel;
    private JList createVmStepsList;
    private JButton buttonLogin;
    private com.microsoft.azure.toolkit.intellij.appservice.subscription.SubscriptionComboBox subscriptionComboBox;
    private JLabel userInfoLabel;
    private Project project;

    public SubscriptionStep(final VMWizardModel model, final Project project) {
        super("Choose a Subscription", null, null);

        this.model = model;
        this.project = project;

        model.configStepList(createVmStepsList, 0);

        buttonLogin.addActionListener(actionEvent -> SelectSubscriptionsAction.selectSubscriptions(project).subscribe());

        subscriptionComboBox.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED && itemEvent.getItem() instanceof Subscription) {
                model.setSubscription((Subscription) itemEvent.getItem());
                model.setRegion(null);
            }
        });

        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null || CollectionUtils.isEmpty(azureManager.getSelectedSubscriptions())) {
            model.getCurrentNavigationState().NEXT.setEnabled(false);
            return;
        }
        Subscription subscription = azureManager.getSelectedSubscriptions().get(0);
        subscriptionComboBox.setValue(subscription);
        this.model.setSubscription(subscription);
    }

    private void createUIComponents() {
        this.subscriptionComboBox = new SubscriptionComboBox();
    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        rootPanel.revalidate();
        model.getCurrentNavigationState().NEXT.setEnabled(subscriptionComboBox.getSelectedItem() != null);
        return rootPanel;
    }

    @Override
    public Map<String, String> toProperties() {
        return model.toProperties();
    }
}
