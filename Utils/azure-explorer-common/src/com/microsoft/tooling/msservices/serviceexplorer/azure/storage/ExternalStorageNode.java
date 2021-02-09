/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.storage;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.DETACH_STORAGE_ACCOUNT;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.STORAGE;

import com.microsoft.tooling.msservices.helpers.ExternalStorageHelper;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

import java.util.Map;

public class ExternalStorageNode extends ClientStorageNode {
    private class DetachAction extends AzureNodeActionPromptListener {
        public DetachAction() {
            super(ExternalStorageNode.this,
                    String.format("This operation will detach external storage account %s.\nAre you sure you want to continue?", storageAccount.getName()),
                    "Detaching External Storage Account");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e)
                throws AzureCmdException {
            Node node = e.getAction().getNode();
            node.getParent().removeDirectChildNode(node);

            ExternalStorageHelper.detach(storageAccount);
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
            return DETACH_STORAGE_ACCOUNT;
        }
    }

    private static final String WAIT_ICON_PATH = "externalstorageaccount.png";

    public ExternalStorageNode(StorageModule parent, ClientStorageAccount sm) {
        super(sm.getName(), sm.getName(), parent, WAIT_ICON_PATH, sm, true);

        loadActions();
    }

    @Override
    protected void refreshItems()
            throws AzureCmdException {
        if (storageAccount.getPrimaryKey().isEmpty()) {
            try {
                NodeActionListener listener = node2Actions.get(this.getClass()).get(0).getConstructor().newInstance();
                listener.actionPerformedAsync(new NodeActionEvent(new NodeAction(this, this.getName()))).get();
            } catch (Throwable t) {
                throw new AzureCmdException("Error opening external storage", t);
            }
        } else {
            fillChildren();
        }
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        addAction("Detach", new DetachAction());

        return super.initActions();
    }
}
