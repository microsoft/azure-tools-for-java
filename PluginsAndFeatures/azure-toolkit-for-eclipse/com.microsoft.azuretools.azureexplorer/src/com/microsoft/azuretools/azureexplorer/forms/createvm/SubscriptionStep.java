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

package com.microsoft.azuretools.azureexplorer.forms.createvm;

import java.util.List;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azuretools.core.Activator;


public class SubscriptionStep extends WizardPage {
    private Label subscriptionLabel;
    private Combo subscriptionComboBox;

    private CreateVMWizard wizard;

    public SubscriptionStep(CreateVMWizard wizard) {
        super("Create new Virtual Machine", "Choose a Subscription", Activator.getImageDescriptor("icons/large/Azure.png"));
        this.wizard = wizard;
    }

    @Override
    public void createControl(Composite parent) {
        GridLayout gridLayout = new GridLayout(1, false);
        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.grabExcessVerticalSpace = true;
        gridData.verticalAlignment = GridData.BEGINNING;
        Composite container = new Composite(parent, 0);
        container.setLayout(gridLayout);
        container.setLayoutData(gridData);

        createSubscriptionCombo(container);

//        this.buttonLogin = new Button(container, SWT.PUSH);
//        this.buttonLogin.setImage(Activator.getImageDescriptor("icons/settings.png").createImage());
//        gridData = new GridData();
//        gridData.horizontalIndent = 5;
//        gridData.widthHint = 50;
//        gridData.heightHint = 40;
//        gridData.verticalAlignment = GridData.BEGINNING;
//        buttonLogin.setLayoutData(gridData);
//        buttonLogin.addSelectionListener(new SelectionAdapter() {
//            @Override
//            public void widgetSelected(SelectionEvent e) {
//                Dialog subscriptionsDialog = new ManageSubscriptionDialog(PluginUtil.getParentShell(), true, false);
//                subscriptionsDialog.open();
//                loadSubscriptions();
//            }
//        });

        this.setControl(container);
    }

    private void createSubscriptionCombo(Composite container) {
        Composite composite = new Composite(container, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.verticalAlignment = GridData.BEGINNING;

        gridData.grabExcessHorizontalSpace = true;
        composite.setLayout(gridLayout);
        composite.setLayoutData(gridData);

        this.subscriptionLabel = new Label(composite, SWT.LEFT);
        this.subscriptionLabel.setText("Choose the subscription to use when creating the new virtual machine:");

        this.subscriptionComboBox = new Combo(composite, SWT.READ_ONLY);
        gridData = new GridData();
//        gridData.widthHint = 182;
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;
        subscriptionComboBox.setLayoutData(gridData);
        subscriptionComboBox.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (subscriptionComboBox.getText() != null && !(subscriptionComboBox.getText().length() == 0)) {
                    wizard.setSubscription((SubscriptionDetail) subscriptionComboBox.getData(subscriptionComboBox.getText()));
                }
            }
        });
        loadSubscriptions();
    }

    private void loadSubscriptions() {
        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            // not signed in
            if (azureManager == null) {
                return;
            }
            /*
             * if (manager.authenticated()) { String upn =
             * manager.getUserInfo().getUniqueName();
             * userInfoLabel.setText("Signed in as: " + (upn.contains("#") ?
             * upn.split("#")[1] : upn)); } else { userInfoLabel.setText(""); }
             */

            SubscriptionManager subscriptionManager = azureManager.getSubscriptionManager();
            List<SubscriptionDetail> subscriptionDetails = subscriptionManager.getSubscriptionDetails();
            for (SubscriptionDetail subscription : subscriptionDetails) {
                if (subscription.isSelected()) {
                    subscriptionComboBox.add(subscription.getSubscriptionName());
                    subscriptionComboBox.setData(subscription.getSubscriptionName(), subscription);
                }
            }
            if (!subscriptionDetails.isEmpty()) {
                subscriptionComboBox.select(0);
                wizard.setSubscription(
                        (SubscriptionDetail) subscriptionComboBox.getData(subscriptionComboBox.getText()));
            }
            setPageComplete(!subscriptionDetails.isEmpty());
        } catch (Exception ex) {
            DefaultLoader.getUIHelper()
                    .logError("An error occurred when trying to load Subscriptions\n\n" + ex.getMessage(), ex);
        }
    }
}
