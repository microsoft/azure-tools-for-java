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
package com.microsoft.azure.hdinsight.serverexplore.hdinsightnode;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccountTypeEnum;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.telemetry.AppInsightsConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.RefreshableNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorageAccountNode extends RefreshableNode implements TelemetryProperties {
    private static final String STORAGE_ACCOUNT_MODULE_ID = StorageAccountNode.class.getName();
    private static final String ICON_PATH = CommonConst.StorageAccountIConPath;
    private static final String ADLS_ICON_PATH = CommonConst.ADLS_STORAGE_ACCOUNT_ICON_PATH;
    private static final String DEFAULT_STORAGE_FLAG = "(default)";

    private IHDIStorageAccount storageAccount;

    public StorageAccountNode(Node parent, IHDIStorageAccount storageAccount, boolean isDefaultStorageAccount) {
       super(STORAGE_ACCOUNT_MODULE_ID, isDefaultStorageAccount ? storageAccount.getName() + DEFAULT_STORAGE_FLAG : storageAccount.getName(), parent, getIconPath(storageAccount));
        this.storageAccount = storageAccount;
        load(false);
    }

    @Override
    protected void refreshItems()
            throws AzureCmdException {
        //TelemetryManager.postEvent(TelemetryCommon.HDInsightExplorerStorageAccountExpand, null, null);
        if(storageAccount.getAccountType() == StorageAccountTypeEnum.BLOB) {
            HDStorageAccount blobStorageAccount = (HDStorageAccount)storageAccount;
            String defaultContainer = blobStorageAccount.getDefaultContainer();
            List<BlobContainer> containerList = StorageClientSDKManager.getManager().getBlobContainers(((HDStorageAccount) storageAccount).getConnectionString());
            for (BlobContainer blobContainer : containerList) {
                addChildNode(new BlobContainerNode(this, blobStorageAccount, blobContainer, !StringHelper.isNullOrWhiteSpace(defaultContainer) && defaultContainer.equals(blobContainer.getName())));
            }
        } else if(storageAccount.getAccountType() == StorageAccountTypeEnum.ADLS) {
            // TODO adls support
        }
    }

    private static String getIconPath(IHDIStorageAccount storageAccount) {
        if(storageAccount.getAccountType() == StorageAccountTypeEnum.ADLS) {
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
}


