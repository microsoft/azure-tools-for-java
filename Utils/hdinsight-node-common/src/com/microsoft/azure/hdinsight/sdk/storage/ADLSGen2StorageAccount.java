package com.microsoft.azure.hdinsight.sdk.storage;

import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;

public class ADLSGen2StorageAccount extends HDStorageAccount {
    public final static String DefaultScheme = "abfs";
    public ADLSGen2StorageAccount(IClusterDetail clusterDetail, String fullStorageBlobName, String key, boolean isDefault, String defaultFileSystem, String scheme) {
        super(clusterDetail, fullStorageBlobName, key, isDefault, defaultFileSystem);
        this.scheme = scheme;
    }

    @Override
    public StorageAccountTypeEnum getAccountType() {
        return StorageAccountTypeEnum.ADLSGen2;
    }
}
