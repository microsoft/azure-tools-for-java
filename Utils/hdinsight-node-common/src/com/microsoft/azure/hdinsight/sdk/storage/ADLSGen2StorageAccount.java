/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.storage;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.AzureHttpObservable;
import com.microsoft.azure.hdinsight.sdk.rest.azure.storageaccounts.StorageAccountAccessKey;
import com.microsoft.azure.hdinsight.sdk.rest.azure.storageaccounts.api.PostListKeysResponse;
import com.microsoft.azure.management.resources.fluentcore.arm.models.HasResourceGroup;
import com.microsoft.azure.management.storage.implementation.StorageManager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.authmanage.IdeAzureAccount;
import rx.Observable;

public class ADLSGen2StorageAccount extends HDStorageAccount implements ILogger {
    public final static String DefaultScheme = "abfs";

    public ADLSGen2StorageAccount(IClusterDetail clusterDetail, String fullStorageBlobName, String key, boolean isDefault, String defaultFileSystem, String scheme) {
        super(clusterDetail, fullStorageBlobName, key, isDefault, defaultFileSystem);
        this.scheme = scheme;
        key = getAccessKeyList(clusterDetail.getSubscription())
                .toBlocking()
                .firstOrDefault(new StorageAccountAccessKey())
                .getValue();

        this.setPrimaryKey(key);
    }

    public ADLSGen2StorageAccount(IClusterDetail clusterDetail, String fullStorageBlobName, boolean isDefault, String defaultFileSystem) {
        super(clusterDetail, fullStorageBlobName, null, isDefault, defaultFileSystem);
        this.scheme = DefaultScheme;
    }

    public String getStorageRootPath() {
        return String.format("%s://%s@%s", this.getscheme(), this.getDefaultContainer(), this.getFullStorageBlobName());
    }

    @Override
    public StorageAccountType getAccountType() {
        return StorageAccountType.ADLSGen2;
    }

    private Observable<StorageAccountAccessKey> getAccessKeyList(Subscription subscription) {
        final String sid = subscription.getId();
        return Observable.fromCallable(() -> IdeAzureAccount.getInstance().authenticateForTrack1(sid, StorageManager.configure(), (t, c) -> c.authenticate(t, sid)))
                .flatMap(azure -> azure.storageAccounts().listAsync())
                .doOnNext(accountList -> log().debug(String.format("Listing storage accounts in subscription %s, accounts %s", subscription.getName(), accountList)))
                .filter(accountList -> accountList.name().equals(getName()))
                .map(HasResourceGroup::resourceGroupName)
                .first()
                .doOnNext(rgName -> log().info(String.format("Finish getting storage account %s resource group name %s", getName(), rgName)))
                .flatMap(rgName -> new AzureHttpObservable(subscription, "2018-07-01").post(String.format("https://management.azure.com/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Storage/storageAccounts/%s/listKeys",
                    sid, rgName, getName()), null, null, null, PostListKeysResponse.class))
                .flatMap(keyList -> Observable.from(keyList.getKeys()));
    }
}
