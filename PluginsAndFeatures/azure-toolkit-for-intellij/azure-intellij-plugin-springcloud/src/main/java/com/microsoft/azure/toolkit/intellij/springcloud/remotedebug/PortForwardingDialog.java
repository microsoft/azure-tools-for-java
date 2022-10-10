/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.remotedebug;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudAppComboBox;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudAppInstanceComboBox;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudClusterComboBox;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;

public class PortForwardingDialog extends DialogWrapper {
    private JPanel contentPanel;
    private SpringCloudAppComboBox appComboBox;
    private JLabel appLabel;
    private JLabel appInstanceLabel;
    private SpringCloudAppInstanceComboBox appInstanceComboBox;
    private SubscriptionComboBox subscriptionComboBox;
    private SpringCloudClusterComboBox appServiceComboBox;

    protected PortForwardingDialog(@Nullable Project project) {
        super(project);
        init();
        initListeners();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return contentPanel;
    }

    public void setSelectedAppInstance(SpringCloudAppInstance appInstance) {
        subscriptionComboBox.setValue(appInstance.getSubscription());
        appServiceComboBox.setValue(appInstance.getParent().getParent().getParent());
        appComboBox.setValue(appInstance.getParent().getParent());
        appInstanceComboBox.setValue(appInstance);
    }

    public SpringCloudAppInstance getSelectedAppInstance() {
        return appInstanceComboBox.getValue();
    }

    private void initListeners() {
        this.subscriptionComboBox.addItemListener(this::onSubscriptionChanged);
        this.appServiceComboBox.addItemListener(this::onClusterChanged);
        this.appComboBox.addItemListener(this::onAppChanged);
    }

    private void onSubscriptionChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final Subscription subscription = this.subscriptionComboBox.getValue();
            this.appServiceComboBox.setSubscription(subscription);
        }
    }

    private void onClusterChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final SpringCloudCluster cluster = this.appServiceComboBox.getValue();
            this.appComboBox.setCluster(cluster);
        }
    }

    private void onAppChanged(final ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
            final SpringCloudApp app = this.appComboBox.getValue();
            this.appInstanceComboBox.setApp(app);
        }
    }
}
