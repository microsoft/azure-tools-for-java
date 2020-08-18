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

package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.ClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccountType;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
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

    private IHDIStorageAccount storageAccount;
    @NotNull
    private IClusterDetail clusterDetail;

    public StorageAccountNode(Node parent, @NotNull IHDIStorageAccount storageAccount, @NotNull IClusterDetail clusterDetail, boolean isDefaultStorageAccount) {
       super(STORAGE_ACCOUNT_MODULE_ID, isDefaultStorageAccount ? storageAccount.getName() + DEFAULT_STORAGE_FLAG : storageAccount.getName(), parent, getIconPath(storageAccount));
        this.storageAccount = storageAccount;
        this.clusterDetail = clusterDetail;
        loadAdditionalActions();
    }

    protected void loadAdditionalActions() {
        if (clusterDetail instanceof ClusterDetail) {
            addAction("Open Storage in Azure Management Portal", new NodeActionListener() {
                @Override
                protected void actionPerformed(NodeActionEvent e) {
                    String subscriptionId = clusterDetail.getSubscription().getSubscriptionId();
                    String storageRelativePath = ((ClusterDetail) clusterDetail).getId() + REST_SEGMENT_STORAGE_ACCOUNT;
                    try {
                        openResourcesInPortal(subscriptionId, storageRelativePath);
                    } catch (AzureCmdException ex) {
                        log().warn("Can't open Storage in Azure Management Portal", ex);
                    }
                }
            });
        }
    }

    private static String getIconPath(IHDIStorageAccount storageAccount) {
        if(storageAccount.getAccountType() == StorageAccountType.ADLS) {
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


