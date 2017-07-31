package com.microsoft.intellij.container.run.remote;

import org.jdom.Element;

public class ContainerRemoteRunModel {
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

    private final AzureContainerRegistryInfo azureContainerRegistryInfo;
    private final WebAppOnLinuxInfo webAppOnLinuxInfo;

    public ContainerRemoteRunModel() {
        azureContainerRegistryInfo = new AzureContainerRegistryInfo();
        webAppOnLinuxInfo = new WebAppOnLinuxInfo();
    }

    public AzureContainerRegistryInfo getAzureContainerRegistryInfo() {
        return azureContainerRegistryInfo;
    }

    public WebAppOnLinuxInfo getWebAppOnLinuxInfo() {
        return webAppOnLinuxInfo;
    }

    /**
     * Load conf from existing xml node.
     * @param element xml node
     */
    public void readExternal(Element element) {
        AzureContainerRegistryInfo acrInfo = getAzureContainerRegistryInfo();
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
     * @param element xml node
     */
    public void writeExternal(Element element) {
        AzureContainerRegistryInfo acrInfo = getAzureContainerRegistryInfo();
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

    public class AzureContainerRegistryInfo {
        private String serverUrl;
        private String username;
        private String password;
        private String imageNameWithTag;
        private String startupFile;

        public String getServerUrl() {
            return serverUrl;
        }

        public void setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getImageNameWithTag() {
            return imageNameWithTag;
        }

        public void setImageNameWithTag(String imageNameWithTag) {
            this.imageNameWithTag = imageNameWithTag;
        }

        public String getStartupFile() {
            return startupFile;
        }

        public void setStartupFile(String startupFile) {
            this.startupFile = startupFile;
        }
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
