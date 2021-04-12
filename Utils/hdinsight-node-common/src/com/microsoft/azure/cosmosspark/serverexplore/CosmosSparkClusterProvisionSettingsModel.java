/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.cosmosspark.serverexplore;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.util.Map;

public class CosmosSparkClusterProvisionSettingsModel {
    @NotNull
    private String clusterName;
    @NotNull
    private String adlAccount;
    @NotNull
    private String sparkEvents;
    @NotNull
    private Map<String, String> extendedProperties;
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
    public CosmosSparkClusterProvisionSettingsModel setRefreshEnabled(boolean refreshEnabled) {
        this.refreshEnabled = refreshEnabled;
        return this;
    }

    @Nullable
    public String getRequestId() {
        return requestId;
    }

    @NotNull
    public CosmosSparkClusterProvisionSettingsModel setRequestId(@Nullable String requestId) {
        this.requestId = requestId;
        return this;
    }

    @NotNull
    public String getClusterName() {
        return clusterName;
    }

    @NotNull
    public CosmosSparkClusterProvisionSettingsModel setClusterName(@NotNull String clusterName) {
        this.clusterName = clusterName;
        return this;
    }

    @NotNull
    public String getAdlAccount() {
        return adlAccount;
    }

    @NotNull
    public CosmosSparkClusterProvisionSettingsModel setAdlAccount(@NotNull String adlAccount) {
        this.adlAccount = adlAccount;
        return this;
    }

    @NotNull
    public String getSparkEvents() {
        return sparkEvents;
    }

    @NotNull
    public CosmosSparkClusterProvisionSettingsModel setSparkEvents(@NotNull String sparkEvents) {
        this.sparkEvents = sparkEvents;
        return this;
    }

    @NotNull
    public Map<String, String> getExtendedProperties() {
        return extendedProperties;
    }

    @NotNull
    public CosmosSparkClusterProvisionSettingsModel setExtendedProperties(@NotNull Map<String, String> extendedProperties) {
        this.extendedProperties = extendedProperties;
        return this;
    }

    public int getAvailableAU() {
        return availableAU;
    }

    @NotNull
    public CosmosSparkClusterProvisionSettingsModel setAvailableAU(int availableAU) {
        this.availableAU = availableAU;
        return this;
    }

    public int getTotalAU() {
        return totalAU;
    }

    @NotNull
    public CosmosSparkClusterProvisionSettingsModel setTotalAU(int totalAU) {
        this.totalAU = totalAU;
        return this;
    }

    public int getCalculatedAU() {
        return calculatedAU;
    }

    @NotNull
    public CosmosSparkClusterProvisionSettingsModel setCalculatedAU(int calculatedAU) {
        this.calculatedAU = calculatedAU;
        return this;
    }

    public int getMasterCores() {
        return masterCores;
    }

    @NotNull
    public CosmosSparkClusterProvisionSettingsModel setMasterCores(int masterCores) {
        this.masterCores = masterCores;
        return this;
    }

    public int getMasterMemory() {
        return masterMemory;
    }

    @NotNull
    public CosmosSparkClusterProvisionSettingsModel setMasterMemory(int masterMemory) {
        this.masterMemory = masterMemory;
        return this;
    }

    public int getWorkerCores() {
        return workerCores;
    }

    @NotNull
    public CosmosSparkClusterProvisionSettingsModel setWorkerCores(int workerCores) {
        this.workerCores = workerCores;
        return this;
    }

    public int getWorkerMemory() {
        return workerMemory;
    }

    @NotNull
    public CosmosSparkClusterProvisionSettingsModel setWorkerMemory(int workerMemory) {
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
    public CosmosSparkClusterProvisionSettingsModel setWorkerNumberOfContainers(int workerNumberOfContainers) {
        this.workerNumberOfContainers = workerNumberOfContainers;
        return this;
    }

    @NotNull
    public String getStorageRootPathLabelTitle() {
        return storageRootPathLabelTitle;
    }

    @NotNull
    public CosmosSparkClusterProvisionSettingsModel setStorageRootPathLabelTitle(
            @NotNull String storageRootPathLabelTitle) {
        this.storageRootPathLabelTitle = storageRootPathLabelTitle;
        return this;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    @NotNull
    public CosmosSparkClusterProvisionSettingsModel setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    public CosmosSparkClusterProvisionSettingsModel setClusterGuid(@NotNull String clusterGuid) {
        this.clusterGuid = clusterGuid;
        return this;
    }
}
