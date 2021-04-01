/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2018-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.storage;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.DELETE_STORAGE_TABLE;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.STORAGE;

import com.microsoft.azure.CommonIcons;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.Table;
import com.microsoft.tooling.msservices.serviceexplorer.*;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureNodeActionPromptListener;

import java.util.HashMap;
import java.util.Map;

public class TableNode extends RefreshableNode implements TelemetryProperties {

    private static final String ACTION_DELETE = "Delete";
    private static final String ACTION_VIEW_TABLE = "View Table";

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, ResourceId.fromString(this.storageAccount.id()).subscriptionId());
        properties.put(AppInsightsConstants.Region, this.storageAccount.regionName());
        return properties;
    }

    public class ViewTable extends NodeActionListener {
        @Override
        public void actionPerformed(NodeActionEvent e) {
            onNodeClick(e);
        }
    }

    public class DeleteTable extends AzureNodeActionPromptListener {
        public DeleteTable() {
            super(TableNode.this,
                    String.format("Are you sure you want to delete the table \"%s\"?", table.getName()),
                    "Deleting Table");
        }

        @Override
        public void actionPerformed(final NodeActionEvent e) {
            Object openedFile = DefaultLoader.getUIHelper().getOpenedFile(getProject(), storageAccount.name(), table);

            if (openedFile != null) {
                DefaultLoader.getIdeHelper().closeFile(getProject(), openedFile);
            }

            try {
                StorageClientSDKManager.getManager().deleteTable(storageAccount, table);

                parent.removeAllChildNodes();
                ((TableModule) parent).load(false);
            } catch (AzureCmdException ex) {
                throw new RuntimeException("An error occurred while attempting to delete table.", ex);
            }
        }

        @Override
        protected void azureNodeAction(NodeActionEvent e)
                throws AzureCmdException {
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
            return DELETE_STORAGE_TABLE;
        }
    }

    private static final String TABLE_MODULE_ID = TableNode.class.getName();
    private static final String ICON_PATH = "container.svg";
    private final Table table;
    private final StorageAccount storageAccount;

    public TableNode(TableModule parent, StorageAccount storageAccount, Table table) {
        super(TABLE_MODULE_ID, table.getName(), parent, ICON_PATH, true);

        this.storageAccount = storageAccount;
        this.table = table;

        loadActions();
    }

    @Override
    protected void onNodeClick(NodeActionEvent ex) {
        final Object openedFile = DefaultLoader.getUIHelper().getOpenedFile(getProject(), storageAccount.name(), table);

        if (openedFile == null) {
            DefaultLoader.getUIHelper().openItem(getProject(), storageAccount, table, " [Table]", "Table", "container.svg");
        } else {
            DefaultLoader.getUIHelper().openItem(getProject(), openedFile);
        }
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        DefaultLoader.getUIHelper().refreshTable(getProject(), storageAccount, table);
    }

    @Override
    protected void loadActions() {
        addAction(ACTION_VIEW_TABLE, new ViewTable());
        addAction(ACTION_DELETE, CommonIcons.ACTION_DISCARD, new DeleteTable(), NodeActionPosition.BOTTOM);
        super.loadActions();
    }
}
