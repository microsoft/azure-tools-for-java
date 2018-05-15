package com.microsoft.azure.sparkserverless.serverexplore;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public class SparkServerlessClusterProvisionSettingsModel implements Cloneable {
    private String clusterName;
    private String adlAccount;
    private String previousSparkEvents;
    private String masterCores;
    private String masterMemory;
    private String workerCores;
    private String workerMemory;
    private String workerNumberOfContainers;
    private String availableAU;
    private String totalAU;
    private String calculatedAU;

    private String clusterNameLabelTitle;
    private String adlAccountLabelTitle;

    private String errorMessage;

    public String getClusterName() {
        return clusterName;
    }

    public SparkServerlessClusterProvisionSettingsModel setClusterName(String clusterName) {
        this.clusterName = clusterName;
        return this;
    }

    public String getAdlAccount() {
        return adlAccount;
    }

    public SparkServerlessClusterProvisionSettingsModel setAdlAccount(String adlAccount) {
        this.adlAccount = adlAccount;
        return this;
    }

    public String getClusterNameLabelTitle() {
        return clusterNameLabelTitle;
    }

    public SparkServerlessClusterProvisionSettingsModel setClusterNameLabelTitle(String clusterNameLabelTitle) {
        this.clusterNameLabelTitle = clusterNameLabelTitle;
        return this;
    }

    public String getAdlAccountLabelTitle() {
        return adlAccountLabelTitle;
    }

    public SparkServerlessClusterProvisionSettingsModel setAdlAccountLabelTitle(String adlAccountLabelTitle) {
        this.adlAccountLabelTitle = adlAccountLabelTitle;
        return this;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public SparkServerlessClusterProvisionSettingsModel setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        // Here is a shadow clone, not deep clone
        return super.clone();
    }
}
