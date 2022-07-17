/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
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
    public Subscription getSubscription() {
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
