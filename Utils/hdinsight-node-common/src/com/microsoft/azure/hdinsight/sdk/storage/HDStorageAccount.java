/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.storage;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;

public class HDStorageAccount extends ClientStorageAccount implements IHDIStorageAccount {
    public final static String DefaultScheme = "wasbs";
    private String fullStorageBlobName;
    private boolean isDefaultStorageAccount;
    private String defaultContainer;
    private IClusterDetail clusterDetail;
    public String scheme;

    public HDStorageAccount(@Nullable IClusterDetail clusterDetail, String fullStorageBlobName, String key, boolean isDefault, String defaultContainer) {
        super(getStorageShortName(fullStorageBlobName));
        this.setPrimaryKey(key);
        this.fullStorageBlobName = fullStorageBlobName;
        this.isDefaultStorageAccount = isDefault;
        this.defaultContainer = defaultContainer;
        this.clusterDetail = clusterDetail;
        this.scheme = DefaultScheme;
    }

    @Override
    public String getSubscriptionId() {
        return this.clusterDetail == null ? "" : this.clusterDetail.getSubscription().getId();
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public StorageAccountType getAccountType() {
        return StorageAccountType.BLOB;
    }

    @Override
    public String getDefaultContainerOrRootPath() {
        return defaultContainer;
    }

    public String getFullStorageBlobName() {
        return fullStorageBlobName;
    }

    public boolean isDefaultStorageAccount() {
        return isDefaultStorageAccount;
    }

    public String getDefaultContainer() {
        return defaultContainer;
    }

    public String getscheme() {
        return scheme;
    }

    private static String getStorageShortName(@NotNull final String fullStorageBlobName) {
        // only lowercase letters and numbers exist in a valid storage short name
        // so we can get the storage short name from storage full name by splitting directly
        // For example:
        //      full name: 'teststorage.blob.core.windows.net', so short name should be 'teststorage'
        return fullStorageBlobName.split("\\.")[0];
    }
}
