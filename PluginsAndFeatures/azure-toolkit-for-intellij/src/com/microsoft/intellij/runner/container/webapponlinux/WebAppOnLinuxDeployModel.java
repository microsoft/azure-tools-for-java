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

package com.microsoft.intellij.runner.container.webapponlinux;

import com.intellij.util.xmlb.XmlSerializer;
import com.microsoft.azuretools.core.mvp.model.webapp.PrivateRegistryImageSetting;

import org.jdom.Element;

public class WebAppOnLinuxDeployModel {
    private static final String ELEMENT_ACR = "AzureContainerRegistry";
    private static final String ATTRIBUTE_SERVER_URL = "ServerUrl";
    private static final String ATTRIBUTE_USERNAME = "Username";
    private static final String ATTRIBUTE_PASSWORD = "Password";
    private static final String ATTRIBUTE_IMAGENAME_WITH_TAG = "ImageNameWithTag";
    private static final String ATTRIBUTE_STARTUP_FILE = "StartUpFile";
    private static final String ELEMENT_WEBAPP = "WebAppOnLinux";
    private static final String ATTRIBUTE_WEBAPP_ID = "WebAppId";
    private static final String ATTRIBUTE_WEBAPP_NAME = "WebAppName";
    private static final String ATTRIBUTE_RESOURCE_GROUP_NAME = "ResourceGroupName";
    private static final String ATTRIBUTE_SUBSCRIPTION_ID = "SubscriptionId";

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


    public WebAppOnLinuxDeployModel() {
        privateRegistryImageSetting = new PrivateRegistryImageSetting();
    }


    /**
     * Load conf from existing xml node.
     *
     * @param element xml node
     */
    public void readExternal(Element element) {
        XmlSerializer.deserializeInto(this, element);
//        PrivateRegistryImageSetting acrInfo = getPrivateRegistryImageSetting();
//        Element acrElement = element.getChild(ELEMENT_ACR);
//        acrInfo.setServerUrl(acrElement.getAttributeValue(ATTRIBUTE_SERVER_URL));
//        acrInfo.setUsername(acrElement.getAttributeValue(ATTRIBUTE_USERNAME));
//        acrInfo.setPassword(acrElement.getAttributeValue(ATTRIBUTE_PASSWORD));
//        acrInfo.setImageNameWithTag(acrElement.getAttributeValue(ATTRIBUTE_IMAGENAME_WITH_TAG));
//        acrInfo.setStartupFile(acrElement.getAttributeValue(ATTRIBUTE_STARTUP_FILE));
//
//        Element webElement = element.getChild(ELEMENT_WEBAPP);
//        setWebAppId(webElement.getAttributeValue(ATTRIBUTE_WEBAPP_ID));
//        setWebAppName(webElement.getAttributeValue(ATTRIBUTE_WEBAPP_NAME));
//        setResourceGroupName(webElement.getAttributeValue(ATTRIBUTE_RESOURCE_GROUP_NAME));
//        setSubscriptionId(webElement.getAttributeValue(ATTRIBUTE_SUBSCRIPTION_ID));
    }

    /**
     * Save conf to xml node.
     *
     * @param element xml node
     */
    public void writeExternal(Element element) {
        XmlSerializer.serializeInto(this, element);
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

}
