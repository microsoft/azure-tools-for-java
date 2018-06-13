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

public class SparkServerlessClusterProvisionSettingsModel implements Cloneable {
    @NotNull
    private String clusterName;
    @NotNull
    private String adlAccount;
    @NotNull
    private String sparkEvents;
    @NotNull
    private int masterCores;
    @NotNull
    private int masterMemory;
    @NotNull
    private int workerCores;
    @NotNull
    private int workerMemory;
    @NotNull
    private int workerNumberOfContainers = 0;
    @NotNull
    private int availableAU;
    @NotNull
    private int totalAU;
    @NotNull
    private int calculatedAU;

    // TODO: Asterisk(*) will be set before titles to warn user there are errors.
    // TODO: Currently only the following titles are added to this class, do we need to add other titles?
    @NotNull
    private String storageRootPathLabelTitle;
    @NotNull
    private String clusterNameLabelTitle;
    @NotNull
    private String adlAccountLabelTitle;
    @NotNull
    private String sparkEventsLabelTitle;
    @NotNull
    private String workerNumberOfContainersLabelTitle;

    @Nullable
    private String errorMessage;

    @NotNull
    public String getClusterName() {
        return clusterName;
    }

    public SparkServerlessClusterProvisionSettingsModel setClusterName(@NotNull String clusterName) {
        this.clusterName = clusterName;
        return this;
    }

    @NotNull
    public String getAdlAccount() {
        return adlAccount;
    }

    public SparkServerlessClusterProvisionSettingsModel setAdlAccount(@NotNull String adlAccount) {
        this.adlAccount = adlAccount;
        return this;
    }

    @NotNull
    public String getSparkEvents() {
        return sparkEvents;
    }

    public SparkServerlessClusterProvisionSettingsModel setSparkEvents(@NotNull String sparkEvents) {
        this.sparkEvents = sparkEvents;
        return this;
    }

    @NotNull
    public int getAvailableAU() {
        return availableAU;
    }

    public SparkServerlessClusterProvisionSettingsModel setAvailableAU(int availableAU) {
        this.availableAU = availableAU;
        return this;
    }

    @NotNull
    public int getTotalAU() {
        return totalAU;
    }

    public SparkServerlessClusterProvisionSettingsModel setTotalAU(@NotNull int totalAU) {
        this.totalAU = totalAU;
        return this;
    }

    @NotNull
    public int getCalculatedAU() {
        return calculatedAU;
    }

    public SparkServerlessClusterProvisionSettingsModel setCalculatedAU(@NotNull int calculatedAU) {
        this.calculatedAU = calculatedAU;
        return this;
    }

    @NotNull
    public int getMasterCores() {
        return masterCores;
    }

    public SparkServerlessClusterProvisionSettingsModel setMasterCores(@NotNull int masterCores) {
        this.masterCores = masterCores;
        return this;
    }

    @NotNull
    public int getMasterMemory() {
        return masterMemory;
    }

    public SparkServerlessClusterProvisionSettingsModel setMasterMemory(@NotNull int masterMemory) {
        this.masterMemory = masterMemory;
        return this;
    }

    @NotNull
    public int getWorkerCores() {
        return workerCores;
    }

    public SparkServerlessClusterProvisionSettingsModel setWorkerCores(@NotNull int workerCores) {
        this.workerCores = workerCores;
        return this;
    }

    @NotNull
    public int getWorkerMemory() {
        return workerMemory;
    }

    public SparkServerlessClusterProvisionSettingsModel setWorkerMemory(@NotNull int workerMemory) {
        this.workerMemory = workerMemory;
        return this;
    }

    @NotNull
    public int getWorkerNumberOfContainers() {
        return workerNumberOfContainers;
    }

    public SparkServerlessClusterProvisionSettingsModel setWorkerNumberOfContainers(
            @NotNull int workerNumberOfContainers) {
        this.workerNumberOfContainers = workerNumberOfContainers;
        return this;
    }

    @NotNull
    public String getClusterNameLabelTitle() {
        return clusterNameLabelTitle;
    }

    public SparkServerlessClusterProvisionSettingsModel setClusterNameLabelTitle(@NotNull String clusterNameLabelTitle) {
        this.clusterNameLabelTitle = clusterNameLabelTitle;
        return this;
    }

    @NotNull
    public String getAdlAccountLabelTitle() {
        return adlAccountLabelTitle;
    }

    public SparkServerlessClusterProvisionSettingsModel setAdlAccountLabelTitle(@NotNull String adlAccountLabelTitle) {
        this.adlAccountLabelTitle = adlAccountLabelTitle;
        return this;
    }

    @NotNull
    public String getSparkEventsLabelTitle() {
        return sparkEventsLabelTitle;
    }

    public SparkServerlessClusterProvisionSettingsModel setSparkEventsLabelTitle(
            @NotNull String sparkEventsLabelTitle) {
        this.sparkEventsLabelTitle = sparkEventsLabelTitle;
        return this;
    }

    @NotNull
    public String getWorkerNumberOfContainersLabelTitle() {
        return workerNumberOfContainersLabelTitle;
    }

    public SparkServerlessClusterProvisionSettingsModel setWorkerNumberOfContainersLabelTitle(
            @NotNull String workerNumberOfContainersLabelTitle) {
        this.workerNumberOfContainersLabelTitle = workerNumberOfContainersLabelTitle;
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

    public SparkServerlessClusterProvisionSettingsModel setErrorMessage(@Nullable String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        // Here is a shadow clone, not deep clone
        return super.clone();
    }
}
