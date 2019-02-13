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
package com.microsoft.azure.hdinsight.sdk.storage;

import com.microsoft.azure.hdinsight.sdk.cluster.ClusterIdentity;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.model.ServiceTreeItem;

public class ADLSStorageAccount implements IHDIStorageAccount, ServiceTreeItem {
    private final String name;
    private boolean isDefaultStorageAccount;
    private final String defaultRootFolderPath;
    private final IClusterDetail clusterDetail;
    private final ClusterIdentity clusterIdentity;
    private ADLSCertificateInfo certificateInfo;
    private final String storageSchema;

    public ADLSStorageAccount(IClusterDetail clusterDetail, String name, boolean isDefault, String defaultRootPath, ClusterIdentity clusterIdentity, String storageSchema) {
        this.name = name;
        this.isDefaultStorageAccount = isDefault;
        this.defaultRootFolderPath = defaultRootPath;
        this.clusterDetail = clusterDetail;
        this.clusterIdentity = clusterIdentity;
        this.storageSchema = storageSchema;
    }

    @Override
    public boolean isLoading() {
        return false;
    }

    @Override
    public void setLoading(boolean loading) {

    }

    @Override
    public String getSubscriptionId() {
        return this.clusterDetail.getSubscription().getSubscriptionId();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public StorageAccountTypeEnum getAccountType() {
        return StorageAccountTypeEnum.ADLS;
    }

    @Override
    public String getDefaultContainerOrRootPath() {
        return defaultRootFolderPath;
    }

    @Override
    public String getStorageSchema(){
        return storageSchema;
    }

    @NotNull
    public ClusterIdentity getClusterIdentity() {
        return this.clusterIdentity;
    }

    @NotNull
    public ADLSCertificateInfo getCertificateInfo() throws HDIException {
        if (this.certificateInfo == null) {
            try {
                this.certificateInfo = new ADLSCertificateInfo(this.clusterIdentity);
                return certificateInfo;
            } catch (Exception e) {
                throw  new HDIException("get ADLS certificate error", e.getMessage());
            }
        } else {
            return this.certificateInfo;
        }
    }
}
