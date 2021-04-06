/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2018-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.storage;

import com.microsoft.azure.management.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.serviceexplorer.*;

import java.util.HashMap;
import java.util.Map;

public class ContainerNode extends RefreshableNode implements TelemetryProperties{

    private static final String ACTION_DELETE = "Delete";
    private static final String ACTION_VIEW_BLOB_CONTAINER = "View Blob Container";

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        if (this.storageAccount != null) {
            properties.put(AppInsightsConstants.SubscriptionId, ResourceId.fromString(this.storageAccount.id()).subscriptionId());
            properties.put(AppInsightsConstants.Region, this.storageAccount.regionName());
        }
        return properties;
    }

    public class ViewBlobContainer extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            onNodeClick(e);
        }
    }

    private static final String CONTAINER_MODULE_ID = ContainerNode.class.getName();
    private static final String ICON_PATH = "StorageAccount/BlobFile.svg";
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
        final Object openedFile = DefaultLoader.getUIHelper().getOpenedFile(getProject(),
                storageAccount != null
                        ? storageAccount.name()
                        : clientStorageAccount.getName()
                , blobContainer);

        if (openedFile == null) {
            if (storageAccount != null) {
                DefaultLoader.getUIHelper().openItem(getProject(), storageAccount, blobContainer, " [Container]", "BlobContainer", "StorageAccount/BlobFile.svg");
            } else {
                DefaultLoader.getUIHelper().openItem(getProject(), clientStorageAccount, blobContainer, " [Container]", "BlobContainer", "StorageAccount/BlobFile.svg");
            }
        } else {
            DefaultLoader.getUIHelper().openItem(getProject(), openedFile);
        }
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        String accountName = storageAccount != null ? storageAccount.name() : clientStorageAccount.getName();
        DefaultLoader.getUIHelper().refreshBlobs(getProject(), accountName, blobContainer);
    }

    @Override
    protected void loadActions() {
        addAction(ACTION_VIEW_BLOB_CONTAINER, new ViewBlobContainer());
        addAction(initActionBuilder(this::delete).withAction(AzureActionEnum.DELETE).withBackgroudable(true).withPromptable(true).build());
        super.loadActions();
    }

    @AzureOperation(name = ActionConstants.StorageAccount.DELETE_BLOB_CONTAINER, type = AzureOperation.Type.ACTION)
    private void delete() {
        Object openedFile = DefaultLoader.getUIHelper().getOpenedFile(getProject(),
                storageAccount != null
                        ? storageAccount.name()
                        : clientStorageAccount.getName(),
                blobContainer);

        if (openedFile != null) {
            DefaultLoader.getIdeHelper().closeFile(getProject(), openedFile);
        }

        try {
            if (storageAccount != null) {
                StorageClientSDKManager.getManager().deleteBlobContainer(storageAccount, blobContainer);
            } else {
                StorageClientSDKManager.getManager().deleteBlobContainer(clientStorageAccount, blobContainer);
            }
            parent.removeAllChildNodes();
            ((RefreshableNode) parent).load(false);
        } catch (AzureCmdException ex) {
            throw new RuntimeException("An error occurred while attempting to delete blob storage", ex);
        }
    }

    private BasicActionBuilder initActionBuilder(Runnable runnable) {
        return new BasicActionBuilder(runnable)
                .withModuleName(StorageModule.MODULE_NAME)
                .withInstanceName(name);
    }
}
