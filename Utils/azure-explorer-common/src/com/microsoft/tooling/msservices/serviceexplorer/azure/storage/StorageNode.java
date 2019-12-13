/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.tooling.msservices.serviceexplorer.azure.storage;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.DELETE_STORAGE_ACCOUNT;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.OPEN_STORAGE_IN_PORTAL;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.STORAGE;

public class StorageNode extends Node implements TelemetryProperties {
    private static final String STORAGE_ACCOUNT_ICON_PATH = "StorageAccount_16.png";

    private final StorageAccount storageAccount;
    private String subscriptionId;

    public StorageNode(Node parent, String subscriptionId, StorageAccount storageAccount) {
        super(storageAccount.name(), storageAccount.name(), parent, STORAGE_ACCOUNT_ICON_PATH,  true);

        this.subscriptionId = subscriptionId;
        this.storageAccount = storageAccount;

        loadActions();
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.subscriptionId);
        properties.put(AppInsightsConstants.Region, this.storageAccount.regionName());
        return properties;
    }

    public class OpenInPortalAction extends AzureNodeActionListener {

        public OpenInPortalAction() {
            super(StorageNode.this, "View storage in portal");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e) throws AzureCmdException {
            openResourcesInPortal(subscriptionId, storageAccount.id());
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {

        }

        @Override
        protected String getServiceName(NodeActionEvent event) {
            return STORAGE;
        }

        @Override
        protected String getOperationName(NodeActionEvent event) {
            return OPEN_STORAGE_IN_PORTAL;
        }
    }

    public class DeleteStorageAccountAction extends AzureNodeActionPromptListener {
        public DeleteStorageAccountAction() {
            super(StorageNode.this,
                    String.format("This operation will delete storage account %s.\nAre you sure you want to continue?", storageAccount.name()),
                    "Deleting Storage Account");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e)
                throws AzureCmdException {
            try {
                AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
                // not signed in
                if (azureManager == null) {
                    return;
                }
                Azure azure = azureManager.getAzure(subscriptionId);
                azure.storageAccounts().deleteByResourceGroup(storageAccount.resourceGroupName(), storageAccount.name());
                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        // instruct parent node to remove this node
                        getParent().removeDirectChildNode(StorageNode.this);
                    }
                });
            } catch (Exception ex) {
                DefaultLoader.getUIHelper().showException("An error occurred while attempting to delete storage account.", ex,
                        "MS Services - Error Deleting Storage Account", false, true);
            }
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) throws AzureCmdException {
        }

        @Override
        protected String getServiceName(NodeActionEvent event) {
            return STORAGE;
        }

        @Override
        protected String getOperationName(NodeActionEvent event) {
            return DELETE_STORAGE_ACCOUNT;
        }
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        addAction("Open in Portal", new OpenInPortalAction());
        addAction("Delete", new DeleteStorageAccountAction());
        return super.initActions();
    }

    public StorageAccount getStorageAccount() {
        return storageAccount;
    }

    @Override
    public String getToolTip() {
        return storageAccount.name() + "\n" + storageAccount.regionName()
                + "<br>" + storageAccount.resourceGroupName();
    }

    public String getSubscriptionId() { return subscriptionId; }
}