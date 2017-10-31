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
package com.microsoft.azure.hdinsight.sdk.cluster;

import com.google.gson.annotations.Expose;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class HDInsightAdditionalClusterDetail implements IClusterDetail {

    private String clusterName;
    private String userName;
    private String passWord;

    @Expose
    private HDStorageAccount defaultStorageAccount;

    public HDInsightAdditionalClusterDetail(String clusterName, String userName, String passWord, HDStorageAccount storageAccount) {
        this.clusterName = clusterName;
        this.userName = userName;
        this.passWord = passWord;
        defaultStorageAccount = storageAccount;
    }

    @Override
    public boolean isEmulator() { return false; }

    @Override
    public boolean isConfigInfoAvailable() {
        return false;
    }

    @Override
    public String getName() {
        return clusterName;
    }

    @Override
    public String getTitle() {
        return Optional.ofNullable(getSparkVersion())
                .filter(ver -> !ver.trim().isEmpty())
                .map(ver -> getName() + "(Spark: " + ver + ")")
                .orElse(getName());
    }

    @Override
    public String getState() {
        return null;
    }

    @Override
    public String getLocation() {
        return null;
    }

    @Override
    public String getConnectionUrl() {
        return String.format("https://%s.azurehdinsight.net", getName());
    }

    @Override
    public String getCreateDate() {
        return null;
    }

    @Override
    public ClusterType getType() {
        return null;
    }

    @Override
    public String getResourceGroup(){
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public String getSparkVersion() {
        return null;
    }

    @Override
    public SubscriptionDetail getSubscription() {
        return null;
    }

    @Override
    public int getDataNodes() {
        return 0;
    }

    @Override
    public String getHttpUserName() throws HDIException {
        return userName;
    }

    @Override
    public String getHttpPassword() throws HDIException {
        return passWord;
    }

    @Override
    public String getOSType() {
        return null;
    }

    @Override
    public IHDIStorageAccount getStorageAccount() throws HDIException {
        return defaultStorageAccount;
    }

    @Override
    public List<HDStorageAccount> getAdditionalStorageAccounts() {
        return null;
    }

    @Override
    public void getConfigurationInfo() throws IOException, HDIException {

    }
}
