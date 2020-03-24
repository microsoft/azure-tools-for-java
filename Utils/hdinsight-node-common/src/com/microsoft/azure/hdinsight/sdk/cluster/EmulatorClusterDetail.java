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

package com.microsoft.azure.hdinsight.sdk.cluster;

import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.io.IOException;
import java.util.Optional;

public class EmulatorClusterDetail implements IClusterDetail {

    private String clusterName;
    private String userName;
    private String passWord;
    private String livyEndpoint;
    private String sshEndpoint;
    private String sparkHistoryEndpoint;
    private String ambariEndpoint;

    public EmulatorClusterDetail(String clusterName, String userName, String passWord, String livyEndpoint, String sshEndpoint, String sparkHistoryEndpoint, String ambariEndpoint) {
        this.clusterName = clusterName;
        this.userName = userName;
        this.passWord = passWord;
        this.livyEndpoint = livyEndpoint;
        this.sshEndpoint = sshEndpoint;
        this.sparkHistoryEndpoint = sparkHistoryEndpoint;
        this.ambariEndpoint = ambariEndpoint;
    }

    public String getSparkHistoryEndpoint() { return sparkHistoryEndpoint; }

    public String getAmbariEndpoint() { return ambariEndpoint; }

    public String getSSHEndpoint() { return sshEndpoint; }

    @Override
        public boolean isEmulator() {
            return true;
        }

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
                .map(ver -> getName() + " (Spark: " + ver + " Emulator)")
                .orElse(getName() + " (Emulator)");
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
            return livyEndpoint;
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
        public String getVersion() {
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
        public String getResourceGroup() {
            return null;
        }

    @Override
    @Nullable
        public IHDIStorageAccount getStorageAccount() {
            return null;
        }

    @Override
        public void getConfigurationInfo() throws IOException, HDIException, AzureCmdException {

        }

    @Override
    public String getSparkVersion() {
        return "1.6.0";
    }
}
