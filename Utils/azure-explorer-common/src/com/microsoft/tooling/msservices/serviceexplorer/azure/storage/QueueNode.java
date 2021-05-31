/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2018-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.storage;

import com.microsoft.azure.CommonIcons;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.model.storage.Queue;
import com.microsoft.tooling.msservices.serviceexplorer.*;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

import java.util.HashMap;
import java.util.Map;

public class QueueNode extends RefreshableNode implements TelemetryProperties{

    private static final String ACTION_DELETE = "Delete";
    private static final String ACTION_VIEW_QUEUE = "View Queue";
    private static final String ACTION_CLEAR_QUEUE = "Clear Queue";

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.storageAccount.getSubscriptionId());
        return properties;
    }

    public class ViewQueue extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            onNodeClick(e);
        }
    }

    public class DeleteQueue extends AzureNodeActionPromptListener {
        public DeleteQueue() {
            super(QueueNode.this,
                    String.format("Are you sure you want to delete the queue \"%s\"?", queue.getName()),
                    "Deleting Queue");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e) {
            Object openedFile = DefaultLoader.getUIHelper().getOpenedFile(getProject(), storageAccount.getName(), queue);

            if (openedFile != null) {
                DefaultLoader.getIdeHelper().closeFile(getProject(), openedFile);
            }

            try {
                StorageClientSDKManager.getManager().deleteQueue(storageAccount, queue);

                parent.removeAllChildNodes();
                ((QueueModule) parent).load(false);
            } catch (AzureCmdException ex) {
                throw new RuntimeException("An error occurred while attempting to delete queue", ex);
            }
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) {
        }
    }

    public class ClearQueue extends AzureNodeActionPromptListener {
        public ClearQueue() {
            super(QueueNode.this,
                    String.format("Are you sure you want to clear the queue \"%s\"?", queue.getName()),
                    "Clearing Queue");
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e) {
            try {
                StorageClientSDKManager.getManager().clearQueue(storageAccount, queue);

                DefaultLoader.getUIHelper().refreshQueue(getProject(), storageAccount, queue);
            } catch (AzureCmdException ex) {
                throw new RuntimeException("An error occurred while attempting to clear queue.", ex);
            }
        }

        @Override
        protected void onSubscriptionsChanged(NodeActionEvent e) {
        }
    }

    private static final String QUEUE_MODULE_ID = QueueNode.class.getName();
    private static final String ICON_PATH = "StorageAccount/queue.svg";
    private final Queue queue;
    private final ClientStorageAccount storageAccount;

    public QueueNode(QueueModule parent, ClientStorageAccount storageAccount, Queue queue) {
        super(QUEUE_MODULE_ID, queue.getName(), parent, ICON_PATH, true);

        this.storageAccount = storageAccount;
        this.queue = queue;

        loadActions();
    }

    @Override
    protected void onNodeClick(NodeActionEvent ex) {
        final Object openedFile = DefaultLoader.getUIHelper().getOpenedFile(getProject(), storageAccount.getName(), queue);

        if (openedFile == null) {
            DefaultLoader.getUIHelper().openItem(getProject(), storageAccount, queue, " [Queue]", "Queue", "StorageAccount/queue.svg");
        } else {
            DefaultLoader.getUIHelper().openItem(getProject(), openedFile);
        }
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        DefaultLoader.getUIHelper().refreshQueue(getProject(), storageAccount, queue);
    }

    @Override
    protected void loadActions() {
        addAction(ACTION_VIEW_QUEUE, new ViewQueue());
        addAction(ACTION_CLEAR_QUEUE, new ClearQueue());
        addAction(ACTION_DELETE, CommonIcons.ACTION_DISCARD, new DeleteQueue(), Groupable.DEFAULT_GROUP, Sortable.LOW_PRIORITY);
        super.loadActions();
    }
}
