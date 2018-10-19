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
package com.microsoft.azure.sparkserverless.serverexplore;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

public class SparkServerlessClusterProvisionSettingsModel {
    @NotNull
    private String clusterName;
    @NotNull
    private String adlAccount;
    @NotNull
    private String sparkEvents;
    private int masterCores;
    private int masterMemory;
    private int workerCores;
    private int workerMemory;
    private int workerNumberOfContainers = 0;
    private int availableAU;
    private int totalAU;
    private int calculatedAU;
    private boolean refreshEnabled;
    @Nullable
    private String clusterGuid;
    @NotNull
    private String storageRootPathLabelTitle;
    @Nullable
    private String errorMessage;
    @Nullable
    private String requestId;

    public boolean getRefreshEnabled() {
        return refreshEnabled;
    }

    @NotNull
    public SparkServerlessClusterProvisionSettingsModel setRefreshEnabled(boolean refreshEnabled) {
        this.refreshEnabled = refreshEnabled;
        return this;
    }

    @Nullable
    public String getRequestId() {
        return requestId;
    }

    @NotNull
    public SparkServerlessClusterProvisionSettingsModel setRequestId(@Nullable String requestId) {
        this.requestId = requestId;
        return this;
    }

    @NotNull
    public String getClusterName() {
        return clusterName;
    }

    @NotNull
    public SparkServerlessClusterProvisionSettingsModel setClusterName(@NotNull String clusterName) {
        this.clusterName = clusterName;
        return this;
    }

    @NotNull
    public String getAdlAccount() {
        return adlAccount;
    }

    @NotNull
    public SparkServerlessClusterProvisionSettingsModel setAdlAccount(@NotNull String adlAccount) {
        this.adlAccount = adlAccount;
        return this;
    }

    @NotNull
    public String getSparkEvents() {
        return sparkEvents;
    }

    @NotNull
    public SparkServerlessClusterProvisionSettingsModel setSparkEvents(@NotNull String sparkEvents) {
        this.sparkEvents = sparkEvents;
        return this;
    }

    public int getAvailableAU() {
        return availableAU;
    }

    @NotNull
    public SparkServerlessClusterProvisionSettingsModel setAvailableAU(int availableAU) {
        this.availableAU = availableAU;
        return this;
    }

    public int getTotalAU() {
        return totalAU;
    }

    @NotNull
    public SparkServerlessClusterProvisionSettingsModel setTotalAU(int totalAU) {
        this.totalAU = totalAU;
        return this;
    }

    public int getCalculatedAU() {
        return calculatedAU;
    }

    @NotNull
    public SparkServerlessClusterProvisionSettingsModel setCalculatedAU(int calculatedAU) {
        this.calculatedAU = calculatedAU;
        return this;
    }

    public int getMasterCores() {
        return masterCores;
    }

    @NotNull
    public SparkServerlessClusterProvisionSettingsModel setMasterCores(int masterCores) {
        this.masterCores = masterCores;
        return this;
    }

    public int getMasterMemory() {
        return masterMemory;
    }

    @NotNull
    public SparkServerlessClusterProvisionSettingsModel setMasterMemory(int masterMemory) {
        this.masterMemory = masterMemory;
        return this;
    }

    public int getWorkerCores() {
        return workerCores;
    }

    @NotNull
    public SparkServerlessClusterProvisionSettingsModel setWorkerCores(int workerCores) {
        this.workerCores = workerCores;
        return this;
    }

    public int getWorkerMemory() {
        return workerMemory;
    }

    @NotNull
    public SparkServerlessClusterProvisionSettingsModel setWorkerMemory(int workerMemory) {
        this.workerMemory = workerMemory;
        return this;
    }

    @NotNull
    public int getWorkerNumberOfContainers() {
        return workerNumberOfContainers;
    }

    @Nullable
    public String getClusterGuid() {
        return clusterGuid;
    }

    @NotNull
    public SparkServerlessClusterProvisionSettingsModel setWorkerNumberOfContainers(int workerNumberOfContainers) {
        this.workerNumberOfContainers = workerNumberOfContainers;
        return this;
    }

    @NotNull
    public String getStorageRootPathLabelTitle() {
        return storageRootPathLabelTitle;
    }

    @NotNull
    public SparkServerlessClusterProvisionSettingsModel setStorageRootPathLabelTitle(
            @NotNull String storageRootPathLabelTitle) {
        this.storageRootPathLabelTitle = storageRootPathLabelTitle;
        return this;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    @NotNull
    public SparkServerlessClusterProvisionSettingsModel setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public SparkServerlessClusterProvisionSettingsModel setClusterGuid(@NotNull String clusterGuid) {
        this.clusterGuid = clusterGuid;
        return this;
    }
}
