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
package com.microsoft.tooling.msservices.model.storage;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.utils.StorageAccoutUtils;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.ServiceTreeItem;

public class ClientStorageAccount implements ServiceTreeItem {
    public static final String DEFAULT_PROTOCOL = StorageAccoutUtils.DEFAULT_PROTOCOL;
    public static final String DEFAULT_ENDPOINTS_PROTOCOL_KEY = StorageAccoutUtils.DEFAULT_ENDPOINTS_PROTOCOL_KEY;
    public static final String ACCOUNT_NAME_KEY = StorageAccoutUtils.ACCOUNT_NAME_KEY;
    public static final String ACCOUNT_KEY_KEY = StorageAccoutUtils.ACCOUNT_KEY_KEY;
    public static final String ENDPOINT_SUFFIX_KEY = StorageAccoutUtils.ENDPOINT_SUFFIX_KEY;
    public static final String BLOB_ENDPOINT_KEY = StorageAccoutUtils.BLOB_ENDPOINT_KEY;
    public static final String QUEUE_ENDPOINT_KEY = StorageAccoutUtils.QUEUE_ENDPOINT_KEY;
    public static final String TABLE_ENDPOINT_KEY = StorageAccoutUtils.TABLE_ENDPOINT_KEY;
    public static final String DEFAULT_CONN_STR_TEMPLATE = StorageAccoutUtils.DEFAULT_CONN_STR_TEMPLATE;
    public static final String CUSTOM_CONN_STR_TEMPLATE = StorageAccoutUtils.CUSTOM_CONN_STR_TEMPLATE;
    protected String subscriptionId;

    private String name;
    private String primaryKey = "";
    private String protocol = "";
    private String blobsUri = "";
    private String queuesUri = "";
    private String tablesUri = "";
    private boolean useCustomEndpoints;
    private boolean loading;

    public ClientStorageAccount(@NotNull String name) {
        this.name = name;
        this.protocol = DEFAULT_PROTOCOL;
    }

    @Override
    public boolean isLoading() {
        return loading;
    }

    @Override
    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(@NotNull String primaryKey) {
        this.primaryKey = primaryKey;
    }

    @NotNull
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(@NotNull String protocol) {
        this.protocol = protocol;
    }

    @NotNull
    public String getBlobsUri() {
        return blobsUri;
    }

    public void setBlobsUri(@NotNull String blobsUri) {
        this.blobsUri = blobsUri;
    }

    @NotNull
    public String getQueuesUri() {
        return queuesUri;
    }

    public void setQueuesUri(@NotNull String queuesUri) {
        this.queuesUri = queuesUri;
    }

    @NotNull
    public String getTablesUri() {
        return tablesUri;
    }

    public void setTablesUri(@NotNull String tablesUri) {
        this.tablesUri = tablesUri;
    }

    public boolean isUseCustomEndpoints() {
        return useCustomEndpoints;
    }

    public void setUseCustomEndpoints(boolean useCustomEndpoints) {
        this.useCustomEndpoints = useCustomEndpoints;
    }

    @NotNull
    public String getConnectionString() {
        if (isUseCustomEndpoints()) {
            return String.format(ClientStorageAccount.CUSTOM_CONN_STR_TEMPLATE,
                    getBlobsUri(),
                    getQueuesUri(),
                    getTablesUri(),
                    getName(),
                    getPrimaryKey());
        } else {
            return String.format(ClientStorageAccount.DEFAULT_CONN_STR_TEMPLATE,
                    getProtocol(),
                    getName(),
                    getPrimaryKey(),
                    StorageClientSDKManager.getEndpointSuffix());
        }
    }

    @Override
    public String toString() {
        return name + (loading ? " (loading...)" : "");
    }

    @NotNull
    public String getSubscriptionId() {
        return subscriptionId;
    }
}