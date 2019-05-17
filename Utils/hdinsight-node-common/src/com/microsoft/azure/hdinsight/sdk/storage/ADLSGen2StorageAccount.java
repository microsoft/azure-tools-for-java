package com.microsoft.azure.hdinsight.sdk.storage;

import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.AzureHttpObservable;
import com.microsoft.azure.hdinsight.sdk.rest.azure.storageaccounts.StorageAccountAccessKey;
import com.microsoft.azure.hdinsight.sdk.rest.azure.storageaccounts.api.PostListKeysResponse;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import rx.Observable;

public class ADLSGen2StorageAccount extends HDStorageAccount implements ILogger {
    public final static String DefaultScheme = "abfs";
    private String accessKey;

    public ADLSGen2StorageAccount(IClusterDetail clusterDetail, String fullStorageBlobName, String key, boolean isDefault, String defaultFileSystem, String scheme) {
        super(clusterDetail, fullStorageBlobName, key, isDefault, defaultFileSystem);
        this.scheme = scheme;
        this.accessKey = getAccessKeyList(clusterDetail.getSubscription())
                .toBlocking()
                .firstOrDefault(new StorageAccountAccessKey())
                .getValue();
    }

    @Override
    public StorageAccountType getAccountType() {
        return StorageAccountType.ADLSGen2;
    }

    public String getAccessKey(){
        return this.accessKey;
    }

    private Observable<StorageAccountAccessKey> getAccessKeyList(SubscriptionDetail subscription) {
        return Observable.fromCallable(() -> AuthMethodManager.getInstance().getAzureManager().getAzure(subscription.getSubscriptionId()))
                .flatMap(azure -> azure.storageAccounts().listAsync())
                .doOnNext(accountList -> log().debug(String.format("Listing storage accounts in subscription %s, accounts %s", subscription.getSubscriptionName(), accountList)))
                .filter(accountList -> accountList.name().equals(getName()))
                .map(ac -> ac.resourceGroupName())
                .first()
                .doOnNext(rgName -> log().info(String.format("Finish getting storage account %s resource group name %s", getName(), rgName)))
                .flatMap(rgName -> new AzureHttpObservable(subscription, "2018-07-01").post(String.format("https://management.azure.com/subscriptions/%s/resourceGroups/%s/providers/Microsoft.Storage/storageAccounts/%s/listKeys",
                        subscription.getSubscriptionId(), rgName, getName()), null, null, null, PostListKeysResponse.class))
                .flatMap(keyList -> Observable.from(keyList.getKeys()));
    }
}
