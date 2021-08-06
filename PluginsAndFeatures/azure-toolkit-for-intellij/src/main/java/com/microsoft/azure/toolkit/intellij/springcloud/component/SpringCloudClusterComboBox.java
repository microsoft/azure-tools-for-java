/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class SpringCloudClusterComboBox extends AzureComboBox<SpringCloudCluster> {

    private Subscription subscription;

    @Override
    protected String getItemText(final Object item) {
        if (Objects.isNull(item)) {
            return AzureComboBox.EMPTY_ITEM;
        }
        return ((SpringCloudCluster) item).name();
    }

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null) {
            this.clear();
            return;
        }
        this.refreshItems();
    }

    @NotNull
    @Override
    @AzureOperation(
        name = "springcloud|cluster.list.subscription",
        params = {"this.subscription.getId()"},
        type = AzureOperation.Type.SERVICE
    )
    protected List<? extends SpringCloudCluster> loadItems() throws Exception {
        if (Objects.nonNull(this.subscription)) {
            final String sid = this.subscription.getId();
            final AzureSpringCloud az = Azure.az(AzureSpringCloud.class).subscription(sid);
            return az.clusters();
        }
        return Collections.emptyList();
    }
}
