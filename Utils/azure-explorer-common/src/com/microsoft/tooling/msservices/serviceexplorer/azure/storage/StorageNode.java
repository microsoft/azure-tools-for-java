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

package com.microsoft.tooling.msservices.serviceexplorer.azure.storage;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.AzureIconSymbol;
import com.microsoft.tooling.msservices.serviceexplorer.BasicActionBuilder;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;

import java.util.HashMap;
import java.util.Map;

public class StorageNode extends Node implements TelemetryProperties {
    private static final String STORAGE_ACCOUNT_ICON_PATH = "StorageAccount_16.png";

    private final StorageAccount storageAccount;
    private String subscriptionId;

    public StorageNode(Node parent, String subscriptionId, StorageAccount storageAccount) {
        super(storageAccount.name(), storageAccount.name(), parent, STORAGE_ACCOUNT_ICON_PATH, true);

        this.subscriptionId = subscriptionId;
        this.storageAccount = storageAccount;

        loadActions();
    }

    @Override
    public @Nullable AzureIconSymbol getIconSymbol() {
        return AzureIconSymbol.StorageAccount.MODULE;
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        properties.put(AppInsightsConstants.Region, this.storageAccount.regionName());
        return properties;
    }

    @AzureOperation(name = ActionConstants.StorageAccount.OPEN_IN_PORTAL, type = AzureOperation.Type.ACTION)
    private void openInPortal() {
        openResourcesInPortal(subscriptionId, storageAccount.id());
    }

    @AzureOperation(name = ActionConstants.StorageAccount.DELETE, type = AzureOperation.Type.ACTION)
    private void delete() {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        // not signed in
        if (azureManager == null) {
            return;
        }
        Azure azure = azureManager.getAzure(subscriptionId);
        azure.storageAccounts().deleteByResourceGroup(storageAccount.resourceGroupName(), storageAccount.name());
        DefaultLoader.getIdeHelper().invokeLater(() -> getParent().removeDirectChildNode(StorageNode.this));
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        addAction(initActionBuilder(this::openInPortal).withAction(AzureActionEnum.OPEN_IN_PORTAL).withBackgroudable(true).build());
        addAction(initActionBuilder(this::delete).withAction(AzureActionEnum.DELETE).withBackgroudable(true).withPromptable(true).build());
        return super.initActions();
    }

    protected final BasicActionBuilder initActionBuilder(Runnable runnable) {
        return new BasicActionBuilder(runnable)
                .withModuleName(StorageModule.MODULE_NAME)
                .withInstanceName(name);
    }

    public StorageAccount getStorageAccount() {
        return storageAccount;
    }

    @Override
    public String getToolTip() {
        return storageAccount.name() + "\n" + storageAccount.regionName()
                + "<br>" + storageAccount.resourceGroupName();
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }
}
