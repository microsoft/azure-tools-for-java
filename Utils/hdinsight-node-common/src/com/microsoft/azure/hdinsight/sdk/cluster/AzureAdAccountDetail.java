/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

// When we access blob or queue data with client tool, client makes requests to Azure Storage under the covers.
// A request to Azure Storage can be authorized using either our Azure AD account or the storage account access key.
// (https://docs.microsoft.com/en-us/azure/storage/common/storage-access-blobs-queues-portal)
// If we want to use Azure AD account to access the storage data corresponding to the cluster, we should implement
// interface AzureAdAccountDetail so that we can use ADLSGen2OAuthHttpObservable to prepare for VFS or upload artifact
// to storage of the cluster.
public interface AzureAdAccountDetail {
    String getTenantId();
}
