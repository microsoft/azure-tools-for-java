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

import static com.microsoft.azuretools.telemetry.TelemetryConstants.DELETE_BLOB_CONTAINER;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.STORAGE;

import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

import java.util.HashMap;
import java.util.Map;

public class ContainerNode extends Node implements TelemetryProperties{
    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, ResourceId.fromString(this.storageAccount.id()).subscriptionId());
        properties.put(AppInsightsConstants.Region, this.storageAccount.regionName());
        return properties;
    }

    public class RefreshAction extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            DefaultLoader.getUIHelper().refreshBlobs(getProject(), storageAccount.name(), blobContainer);
        }
    }

    public class ViewBlobContainer extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            onNodeClick(e);
        }
    }

    public class DeleteBlobContainer extends AzureNodeActionPromptListener {
        public DeleteBlobContainer() {
            super(ContainerNode.this,
                    String.format("Are you sure you want to delete the blob container \"%s\"?", blobContainer.getName()),
                    "Deleting Blob Container");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e)
                throws AzureCmdException {
            Object openedFile = DefaultLoader.getUIHelper().getOpenedFile(getProject(), storageAccount.name(), blobContainer);

            if (openedFile != null) {
                DefaultLoader.getIdeHelper().closeFile(getProject(), openedFile);
            }

            EventUtil.executeWithLog(STORAGE, DELETE_BLOB_CONTAINER, (operation) -> {
                    StorageClientSDKManager.getManager().deleteBlobContainer(storageAccount, blobContainer);
                    parent.removeAllChildNodes();
                    ((RefreshableNode) parent).load(false);
                }, (ex) ->
                    DefaultLoader.getUIHelper()
                        .showException("An error occurred while attempting to delete blob storage", ex,
                            "MS Services - Error Deleting Blob Storage", false, true)
                );
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e)
                throws AzureCmdException {
        }
    }

    private static final String CONTAINER_MODULE_ID = ContainerNode.class.getName();
    private static final String ICON_PATH = "BlobFile_16.png";
    private final BlobContainer blobContainer;
    private StorageAccount storageAccount;
    private ClientStorageAccount clientStorageAccount;

    public ContainerNode(final Node parent, StorageAccount sa, BlobContainer bc) {
        super(CONTAINER_MODULE_ID, bc.getName(), parent, ICON_PATH, true);

        blobContainer = bc;
        storageAccount = sa;

        loadActions();
    }

    public ContainerNode(final Node parent, ClientStorageAccount sa, BlobContainer bc) {
        super(CONTAINER_MODULE_ID, bc.getName(), parent, ICON_PATH, true);

        blobContainer = bc;
        clientStorageAccount = sa;

        loadActions();
    }

    @Override
    protected void onNodeClick(NodeActionEvent e) {
        final Object openedFile = DefaultLoader.getUIHelper().getOpenedFile(getProject(), storageAccount.name(), blobContainer);

        if (openedFile == null) {
            DefaultLoader.getUIHelper().openItem(getProject(), storageAccount, blobContainer, " [Container]", "BlobContainer", "BlobFile_16.png");
        } else {
            DefaultLoader.getUIHelper().openItem(getProject(), openedFile);
        }
    }

    @Override
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        return ImmutableMap.of(
                "Refresh", RefreshAction.class,
                "View Blob Container", ViewBlobContainer.class,
                "Delete", DeleteBlobContainer.class);
    }
}