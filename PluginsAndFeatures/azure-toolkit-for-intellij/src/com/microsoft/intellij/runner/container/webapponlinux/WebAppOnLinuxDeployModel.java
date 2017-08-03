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

    private final PrivateRegistryImageSetting azureContainerRegistryInfo;
    private final WebAppOnLinuxInfo webAppOnLinuxInfo;

    public WebAppOnLinuxDeployModel() {
        azureContainerRegistryInfo = new PrivateRegistryImageSetting();
        webAppOnLinuxInfo = new WebAppOnLinuxInfo();
    }

    public PrivateRegistryImageSetting getAzureContainerRegistryInfo() {
        return azureContainerRegistryInfo;
    }

    public WebAppOnLinuxInfo getWebAppOnLinuxInfo() {
        return webAppOnLinuxInfo;
    }

    /**
     * Load conf from existing xml node.
     *
     * @param element xml node
     */
    public void readExternal(Element element) {
        PrivateRegistryImageSetting acrInfo = getAzureContainerRegistryInfo();
        Element acrElement = element.getChild(ELEMENT_ACR);
        acrInfo.setServerUrl(acrElement.getAttributeValue(ATTRIBUTE_SERVER_URL));
        acrInfo.setUsername(acrElement.getAttributeValue(ATTRIBUTE_USERNAME));
        acrInfo.setPassword(acrElement.getAttributeValue(ATTRIBUTE_PASSWORD));
        acrInfo.setImageNameWithTag(acrElement.getAttributeValue(ATTRIBUTE_IMAGENAME_WITH_TAG));
        acrInfo.setStartupFile(acrElement.getAttributeValue(ATTRIBUTE_STARTUP_FILE));

        WebAppOnLinuxInfo webInfo = getWebAppOnLinuxInfo();
        Element webElement = element.getChild(ELEMENT_WEBAPP);
        webInfo.setWebAppId(webElement.getAttributeValue(ATTRIBUTE_WEBAPP_ID));
        webInfo.setWebAppName(webElement.getAttributeValue(ATTRIBUTE_WEBAPP_NAME));
        webInfo.setResourceGroupName(webElement.getAttributeValue(ATTRIBUTE_RESOURCE_GROUP_NAME));
        webInfo.setSubscriptionId(webElement.getAttributeValue(ATTRIBUTE_SUBSCRIPTION_ID));
    }

    /**
     * Save conf to xml node.
     *
     * @param element xml node
     */
    public void writeExternal(Element element) {
        PrivateRegistryImageSetting acrInfo = getAzureContainerRegistryInfo();
        Element acrElement = new Element(ELEMENT_ACR);
        if (acrInfo.getServerUrl() != null) {
            acrElement.setAttribute(ATTRIBUTE_SERVER_URL, acrInfo.getServerUrl());
        }
        if (acrInfo.getUsername() != null) {
            acrElement.setAttribute(ATTRIBUTE_USERNAME, acrInfo.getUsername());
        }
        if (acrInfo.getPassword() != null) {
            acrElement.setAttribute(ATTRIBUTE_PASSWORD, acrInfo.getPassword());
        }
        if (acrInfo.getImageNameWithTag() != null) {
            acrElement.setAttribute(ATTRIBUTE_IMAGENAME_WITH_TAG, acrInfo.getImageNameWithTag());
        }
        if (acrInfo.getStartupFile() != null) {
            acrElement.setAttribute(ATTRIBUTE_STARTUP_FILE, acrInfo.getStartupFile());
        }
        element.addContent(acrElement);

        WebAppOnLinuxInfo webInfo = getWebAppOnLinuxInfo();
        Element webElement = new Element(ELEMENT_WEBAPP);
        if (webInfo.getWebAppId() != null) {
            webElement.setAttribute(ATTRIBUTE_WEBAPP_ID, webInfo.getWebAppId());
        }
        if (webInfo.getWebAppName() != null) {
            webElement.setAttribute(ATTRIBUTE_WEBAPP_NAME, webInfo.getWebAppName());
        }
        if (webInfo.getResourceGroupName() != null) {
            webElement.setAttribute(ATTRIBUTE_RESOURCE_GROUP_NAME, webInfo.getResourceGroupName());
        }
        if (webInfo.getSubscriptionId() != null) {
            webElement.setAttribute(ATTRIBUTE_SUBSCRIPTION_ID, webInfo.getSubscriptionId());
        }
        element.addContent(webElement);
    }

    public class WebAppOnLinuxInfo {
        private String webAppId;
        private String webAppName;
        private String subscriptionId;
        private String resourceGroupName;

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
    }
}
