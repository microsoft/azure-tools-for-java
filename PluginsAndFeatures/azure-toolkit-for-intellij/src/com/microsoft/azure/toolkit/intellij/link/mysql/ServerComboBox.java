/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.link.mysql;

import com.microsoft.azure.management.mysql.v2020_01_01.Server;
import com.microsoft.azure.management.mysql.v2020_01_01.implementation.MySQLManager;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ServerComboBox extends AzureComboBox<Server> {

    private Subscription subscription;

    public ServerComboBox() {
        super(true);
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

    @Override
    protected String getItemText(Object item) {
        if (item instanceof Server) {
            return ((Server) item).name();
        }
        return super.getItemText(item);
    }

    @Override
    protected List<? extends Server> loadItems() throws Exception {
        if (Objects.isNull(subscription)) {
            return new ArrayList<>();
        }
        AzureManager manager = AuthMethodManager.getInstance().getAzureManager();
        if (Objects.isNull(manager)) {
            return new ArrayList<>();
        }
        final MySQLManager mySQLManager = manager.getMySQLManager(subscription.subscriptionId());
        return mySQLManager.servers().list();
    }

    @Override
    public boolean isRequired() {
        return true;
    }

}
