package com.microsoft.azure.hdinsight.sdk.storage;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;

public class ADLSGen2StorageAccount extends HDStorageAccount {
    public ADLSGen2StorageAccount(IClusterDetail clusterDetail, String fullStorageBlobName, String key, boolean isDefault, String defaultContainer) {
        super(clusterDetail, fullStorageBlobName, key, isDefault, defaultContainer);
    }

    @Override
    public StorageAccountTypeEnum getAccountType() {
        return StorageAccountTypeEnum.ADLSGen2;
    }
}
