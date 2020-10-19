/*
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

package com.microsoft.intellij.wizards.createarmvm;

import com.intellij.openapi.project.Project;
import com.intellij.ui.wizard.WizardNavigationState;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.ijidea.actions.SelectSubscriptionsAction;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.intellij.ui.components.AzureWizardStep;
import com.microsoft.intellij.wizards.VMWizardModel;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import icons.CommonIcons;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SubscriptionStep extends AzureWizardStep<VMWizardModel> implements TelemetryProperties {
    VMWizardModel model;
    private JPanel rootPanel;
    private JList createVmStepsList;
    private JButton buttonLogin;
    private JComboBox<SubscriptionDetail> subscriptionComboBox;
    private JLabel userInfoLabel;
    private Project project;

    public SubscriptionStep(final VMWizardModel model, final Project project) {
        super("Choose a Subscription", null, null);

        this.model = model;
        this.project = project;

        model.configStepList(createVmStepsList, 0);

        buttonLogin.setIcon(CommonIcons.Subscriptions);

        buttonLogin.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                SelectSubscriptionsAction.onShowSubscriptions(project);
                loadSubscriptions();
            }
        });

        subscriptionComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent itemEvent) {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED && itemEvent.getItem() instanceof SubscriptionDetail) {
                    model.setSubscription((SubscriptionDetail) itemEvent.getItem());
                    model.setRegion(null);
                }
            }
        });
        loadSubscriptions();
    }

    @Override
    public JComponent prepare(WizardNavigationState wizardNavigationState) {
        rootPanel.revalidate();
        model.getCurrentNavigationState().NEXT.setEnabled(subscriptionComboBox.getSelectedItem() != null);
        return rootPanel;
    }

    private void loadSubscriptions() {
        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            // not signed in
            if (azureManager == null) {
                model.getCurrentNavigationState().NEXT.setEnabled(false);
                return;
            }
            SubscriptionManager subscriptionManager = azureManager.getSubscriptionManager();
            List<SubscriptionDetail> subscriptionDetails = subscriptionManager.getSubscriptionDetails();
            List<SubscriptionDetail> selectedSubscriptions = subscriptionDetails.stream().filter(SubscriptionDetail::isSelected).collect(Collectors.toList());

            subscriptionComboBox.setModel(new DefaultComboBoxModel<>(selectedSubscriptions.toArray(new SubscriptionDetail[selectedSubscriptions.size()])));
            if (selectedSubscriptions.size() > 0) {
                model.setSubscription(selectedSubscriptions.get(0));
            }
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().logError("An error occurred when trying to load Subscriptions\n\n" + ex.getMessage(), ex);
        }

//            if (manager.authenticated()) {
//                String upn = manager.getUserInfo().getUniqueName();
//                userInfoLabel.setText("Signed in as: " + (upn.contains("#") ? upn.split("#")[1] : upn));
//            } else {
//                userInfoLabel.setText("");
//            }
    }

    @Override
    public Map<String, String> toProperties() {
        return model.toProperties();
    }
}
