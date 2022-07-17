/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccountType;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;

import java.util.HashMap;
import java.util.Map;

public class StorageAccountNode extends Node implements TelemetryProperties, ILogger {
    private static final String STORAGE_ACCOUNT_MODULE_ID = StorageAccountNode.class.getName();
    private static final String ICON_PATH = CommonConst.StorageAccountIConPath;
    private static final String ADLS_ICON_PATH = CommonConst.ADLS_STORAGE_ACCOUNT_ICON_PATH;
    private static final String DEFAULT_STORAGE_FLAG = "(default)";
    private static final String REST_SEGMENT_STORAGE_ACCOUNT = "/storageaccounts";

    private final IHDIStorageAccount storageAccount;
    @NotNull
    private final IClusterDetail clusterDetail;

    public StorageAccountNode(Node parent, @NotNull IHDIStorageAccount storageAccount, @NotNull IClusterDetail clusterDetail, boolean isDefaultStorageAccount) {
        super(STORAGE_ACCOUNT_MODULE_ID, isDefaultStorageAccount ? storageAccount.getName() + DEFAULT_STORAGE_FLAG : storageAccount.getName(), parent,
              getIconPath(storageAccount));
        this.storageAccount = storageAccount;
        this.clusterDetail = clusterDetail;
        loadAdditionalActions();
    }

    protected void loadAdditionalActions() {
        if (clusterDetail instanceof ClusterDetail) {
            addAction("Open Storage in Azure Management Portal", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    final String subscriptionId = clusterDetail.getSubscription().getId();
                    final String storageRelativePath = ((ClusterDetail) clusterDetail).getId() + REST_SEGMENT_STORAGE_ACCOUNT;
                    openResourcesInPortal(subscriptionId, storageRelativePath);
                }
            });
        }
    }

    private static String getIconPath(IHDIStorageAccount storageAccount) {
        if (storageAccount.getAccountType() == StorageAccountType.ADLS) {
            return ADLS_ICON_PATH;
        } else {
            return ICON_PATH;
        }
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        properties.put(AppInsightsConstants.SubscriptionId, this.storageAccount.getSubscriptionId());
        return properties;
    }

    @Override
    public String getServiceName() {
        return TelemetryConstants.HDINSIGHT;
    }
}


