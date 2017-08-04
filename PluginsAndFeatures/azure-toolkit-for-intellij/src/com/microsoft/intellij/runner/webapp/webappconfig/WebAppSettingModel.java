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

package com.microsoft.intellij.runner.webapp.webappconfig;

import org.jdom.Element;

public class WebAppSettingModel {

    private static final String WEB_APP_ID = "WebAppId";
    private static final String SUBSCRIPTION_ID = "SubscriptionId";
    private static final String WEB_APP_URL = "webAppUrl";
    private static final String USING_EXISTING = "usingExisting";
    private static final String DEPLOY_TO_ROOT = "deployToRoot";
    private static final String TARGET_PATH = "targetPath";
    private static final String TARGET_NAME = "targetName";

    // common settings
    private boolean creatingNew = false;
    private String subscriptionId = "";
    // deploy related
    private String webAppId = "";
    private String webAppUrl = "";
    private String targetPath = "";
    private String targetName = "";
    private boolean deployToRoot = true;
    // create related
    private String webAppName = "";
    private String webContainer = "";

    private boolean creatingResGrp = false;
    private String resourceGroup = "";

    private boolean creatingAppServicePlan = false;
    private String appServicePlan = "";
    private String region = "";
    private String pricing = "";

    private String jdkChoice = "";
    private String jdkUrl = "";
    private String storageKey = "";

    public String getWebAppId() {
        return webAppId;
    }

    public String getSubscriptionId() {
        return this.subscriptionId;
    }

    public void setWebAppId(String webAppId) {
        this.webAppId = webAppId;
    }

    public void setSubscriptionId(String subId) {
        this.subscriptionId = subId;
    }

    public String getWebAppUrl() {
        return webAppUrl;
    }

    public void setWebAppUrl(String webAppUrl) {
        this.webAppUrl = webAppUrl;
    }

    public boolean isDeployToRoot() {
        return deployToRoot;
    }

    public void setDeployToRoot(boolean deployToRoot) {
        this.deployToRoot = deployToRoot;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public boolean isCreatingNew() {
        return creatingNew;
    }

    public void setCreatingNew(boolean creatingNew) {
        this.creatingNew = creatingNew;
    }


    public void readExternal(Element element) {
        if (element != null) {
            this.webAppId = element.getAttributeValue(WEB_APP_ID);
            this.subscriptionId = element.getAttributeValue(SUBSCRIPTION_ID);
            this.webAppUrl = element.getAttributeValue(WEB_APP_URL);
            this.creatingNew = Boolean.valueOf(element.getAttributeValue(USING_EXISTING));
            this.deployToRoot = Boolean.valueOf(element.getAttributeValue(DEPLOY_TO_ROOT));
            this.targetPath = element.getAttributeValue(TARGET_PATH);
            this.targetName = element.getAttributeValue(TARGET_NAME);
        }
    }

    public void writeExternal(Element element) {
        element.setAttribute(WEB_APP_ID, this.webAppId);
        element.setAttribute(SUBSCRIPTION_ID, this.subscriptionId);
        element.setAttribute(WEB_APP_URL, this.webAppUrl);
        element.setAttribute(USING_EXISTING, String.valueOf(this.creatingNew));
        element.setAttribute(DEPLOY_TO_ROOT, String.valueOf(this.deployToRoot));
        element.setAttribute(TARGET_PATH, this.targetPath);
        element.setAttribute(TARGET_NAME, this.targetName);
    }

    public String getWebAppName() {
        return webAppName;
    }

    public void setWebAppName(String webAppName) {
        this.webAppName = webAppName;
    }

    public String getWebContainer() {
        return webContainer;
    }

    public void setWebContainer(String webContainer) {
        this.webContainer = webContainer;
    }

    public String getResourceGroup() {
        return resourceGroup;
    }

    public void setResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public boolean isCreatingResGrp() {
        return creatingResGrp;
    }

    public void setCreatingResGrp(boolean creatingResGrp) {
        this.creatingResGrp = creatingResGrp;
    }

    public boolean isCreatingAppServicePlan() {
        return creatingAppServicePlan;
    }

    public void setCreatingAppServicePlan(boolean creatingAppServicePlan) {
        this.creatingAppServicePlan = creatingAppServicePlan;
    }

    public String getAppServicePlan() {
        return appServicePlan;
    }

    public void setAppServicePlan(String appServicePlan) {
        this.appServicePlan = appServicePlan;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPricing() {
        return pricing;
    }

    public void setPricing(String pricing) {
        this.pricing = pricing;
    }

    public String getJdkChoice() {
        return jdkChoice;
    }

    public void setJdkChoice(String jdkChoice) {
        this.jdkChoice = jdkChoice;
    }

    public String getJdkUrl() {
        return jdkUrl;
    }

    public void setJdkUrl(String jdkUrl) {
        this.jdkUrl = jdkUrl;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }
}
