/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.webapp;

public class WebAppOnLinuxDeployModel {
    private PrivateRegistryImageSetting privateRegistryImageSetting;
    private boolean creatingNewWebAppOnLinux;
    private String webAppId;
    private String webAppName;
    private String subscriptionId;
    private String resourceGroupName;
    private boolean creatingNewResourceGroup;
    private String locationName;
    private String pricingSkuTier;
    private String pricingSkuSize;
    private boolean creatingNewAppServicePlan;
    private String appServicePlanId;
    private String appServicePlanName;
    private String targetPath;
    private String targetName;
    private String dockerFilePath;


    public WebAppOnLinuxDeployModel() {
        privateRegistryImageSetting = new PrivateRegistryImageSetting();
    }

    public String getWebAppId() {
        return webAppId;
    }

    public void setWebAppId(String webAppId) {
        this.webAppId = webAppId;
    }

    public String getWebAppName() {
        return webAppName;
    }

    public void setWebAppName(String webAppName) {
        this.webAppName = webAppName;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public boolean isCreatingNewResourceGroup() {
        return creatingNewResourceGroup;
    }

    public void setCreatingNewResourceGroup(boolean creatingNewResourceGroup) {
        this.creatingNewResourceGroup = creatingNewResourceGroup;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public String getPricingSkuTier() {
        return pricingSkuTier;
    }

    public void setPricingSkuTier(String pricingSkuTier) {
        this.pricingSkuTier = pricingSkuTier;
    }

    public String getPricingSkuSize() {
        return pricingSkuSize;
    }

    public void setPricingSkuSize(String pricingSkuSize) {
        this.pricingSkuSize = pricingSkuSize;
    }

    public boolean isCreatingNewWebAppOnLinux() {
        return creatingNewWebAppOnLinux;
    }

    public void setCreatingNewWebAppOnLinux(boolean creatingNewWebAppOnLinux) {
        this.creatingNewWebAppOnLinux = creatingNewWebAppOnLinux;
    }

    public PrivateRegistryImageSetting getPrivateRegistryImageSetting() {
        return privateRegistryImageSetting;
    }

    public void setPrivateRegistryImageSetting(PrivateRegistryImageSetting privateRegistryImageSetting) {
        this.privateRegistryImageSetting = privateRegistryImageSetting;
    }

    public boolean isCreatingNewAppServicePlan() {
        return creatingNewAppServicePlan;
    }

    public void setCreatingNewAppServicePlan(boolean creatingNewAppServicePlan) {
        this.creatingNewAppServicePlan = creatingNewAppServicePlan;
    }

    public String getAppServicePlanId() {
        return appServicePlanId;
    }

    public void setAppServicePlanId(String appServicePlanId) {
        this.appServicePlanId = appServicePlanId;
    }

    public String getAppServicePlanName() {
        return appServicePlanName;
    }

    public void setAppServicePlanName(String appServicePlanName) {
        this.appServicePlanName = appServicePlanName;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public String getTargetPath() {
        return this.targetPath;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getTargetName() {
        return this.targetName;
    }

    public String getDockerFilePath() {
        return dockerFilePath;
    }

    public void setDockerFilePath(String dockerFilePath) {
        this.dockerFilePath = dockerFilePath;
    }
}
