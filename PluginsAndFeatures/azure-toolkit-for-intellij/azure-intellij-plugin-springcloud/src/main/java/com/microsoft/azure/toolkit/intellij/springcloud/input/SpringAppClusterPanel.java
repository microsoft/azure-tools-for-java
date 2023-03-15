/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.input;

import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudClusterComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SpringAppClusterPanel implements AzureFormJPanel<SpringCloudCluster> {
    private SpringCloudClusterComboBox cbCluster;
    private JPanel pnlRoot;

    private String subscriptionId;

    public SpringAppClusterPanel() {
        $$$setupUI$$$();
        this.cbCluster.setRequired(true);
        this.cbCluster.setLabel("Cluster");
    }

    @Override
    public SpringCloudCluster getValue() {
        return cbCluster.getValue();
    }

    public void setValue(final SpringCloudCluster value) {
        this.cbCluster.setValue(value);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(cbCluster);
    }

    @Override
    public JPanel getContentPanel() {
        return pnlRoot;
    }

    public void setSubscriptionId(final String subscriptionId){
        if (Objects.equals(subscriptionId, this.subscriptionId)) {
            return;
        }
        this.subscriptionId = subscriptionId;
        final Subscription subscription = StringUtils.isEmpty(subscriptionId) ? null :
                Azure.az(AzureAccount.class).account().getSubscription(subscriptionId);
        this.cbCluster.setSubscription(subscription);
    }
}
