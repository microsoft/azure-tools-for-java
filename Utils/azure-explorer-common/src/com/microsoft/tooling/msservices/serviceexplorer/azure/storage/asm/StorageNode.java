/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.storage.asm;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.DELETE_STORAGE_ACCOUNT;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.STORAGE;

import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.storage.ClientStorageNode;

import java.util.HashMap;
import java.util.Map;

public class StorageNode extends ClientStorageNode implements TelemetryProperties {
    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.storageAccount.getSubscriptionId());
        return properties;
    }

    public class DeleteStorageAccountAction extends AzureNodeActionPromptListener {
        public DeleteStorageAccountAction() {
            super(StorageNode.this,
                    String.format("This operation will delete storage account %s.\nAre you sure you want to continue?", storageAccount.getName()),
                    "Deleting Storage Account");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e)
                throws AzureCmdException {
//            try {
                // TODO
//                AzureManagerImpl.getManager(getProject()).deleteStorageAccount(storageAccount);

                DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        // instruct parent node to remove this node
                        getParent().removeDirectChildNode(StorageNode.this);
                    }
                });
//            } catch (AzureCmdException ex) {
//                DefaultLoader.getUIHelper().showException("An error occurred while attempting to delete storage account.", ex,
//                        "MS Services - Error Deleting Storage Account", false, true);
//            }
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e)
                throws AzureCmdException {
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

    private static final String WAIT_ICON_PATH = "StorageAccount_16.png";
    private static final String DEFAULT_STORAGE_FLAG = "(default)";
    private final ClientStorageAccount storageAccount;

    public StorageNode(Node parent, ClientStorageAccount sm, boolean isDefaultStorageAccount) {
        super(sm.getName(), isDefaultStorageAccount ? sm.getName() + DEFAULT_STORAGE_FLAG : sm.getName(), parent, WAIT_ICON_PATH, sm, true);

        this.storageAccount = sm;

        loadActions();
    }

    @Override
    protected void refreshItems()
            throws AzureCmdException {
        fillChildren();
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        addAction("Delete", new DeleteStorageAccountAction());
        return super.initActions();
    }
}
