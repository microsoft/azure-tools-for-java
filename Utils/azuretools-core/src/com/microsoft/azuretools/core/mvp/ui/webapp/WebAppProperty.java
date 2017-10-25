package com.microsoft.azuretools.core.mvp.ui.webapp;

import java.util.Map;

import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azuretools.core.mvp.ui.base.ResourceProperty;

public class WebAppProperty extends ResourceProperty {

    private String status;
    private String appServicePlan;
    private String url;
    private String pricingTier;
    private String javaVersion;
    private String container;
    private String containerVersion;
    private OperatingSystem operatingSystem;
    private Map<String, String> appSettings;

    public WebAppProperty(String name, String type, String groupName, String regionName, String subscriptionId,
            String status, String appServicePlan, String url, String pricingTier, String javaVersion, String container,
            String containerVersion, OperatingSystem operatingSystem, Map<String, String> appSettings) {
        super(name, type, groupName, regionName, subscriptionId);
        this.status = status;
        this.appServicePlan = appServicePlan;
        this.url = url;
        this.pricingTier = pricingTier;
        this.javaVersion = javaVersion;
        this.container = container;
        this.containerVersion = containerVersion;
        this.operatingSystem = operatingSystem;
        this.appSettings = appSettings;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public String getAppServicePlan() {
        return appServicePlan;
    }

    public void setAppServicePlan(String appServicePlan) {
        this.appServicePlan = appServicePlan;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPricingTier() {
        return pricingTier;
    }

    public void setPricingTier(String pricingTier) {
        this.pricingTier = pricingTier;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public String getContainerVersion() {
        return containerVersion;
    }

    public void setContainerVersion(String containerVersion) {
        this.containerVersion = containerVersion;
    }

    public OperatingSystem getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(OperatingSystem operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public Map<String, String> getAppSettings() {
        return appSettings;
    }

    public void setAppSettings(Map<String, String> appSettings) {
        this.appSettings = appSettings;
    }
}
