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
        return this.clusterDetail == null ? "" : this.clusterDetail.getSubscription().getSubscriptionId();
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
