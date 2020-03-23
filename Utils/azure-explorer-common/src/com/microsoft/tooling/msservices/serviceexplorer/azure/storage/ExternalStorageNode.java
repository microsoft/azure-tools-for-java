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