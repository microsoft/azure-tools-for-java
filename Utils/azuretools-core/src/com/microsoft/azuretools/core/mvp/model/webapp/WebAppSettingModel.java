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

package com.microsoft.azuretools.core.mvp.model.webapp;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.utils.WebAppUtils;
import java.util.HashMap;
import java.util.Map;

public class WebAppSettingModel {

    // common settings
    private boolean creatingNew = false;
    private String subscriptionId = "";
    // deploy related
    private String webAppId = "";
    private String targetPath = "";
    private String targetName = "";
    private boolean deployToRoot = true;
    private boolean deployToSlot = false;
    private String slotName;
    private String newSlotName;
    private String newSlotConfigurationSource;
    // create related
    private String webAppName = "";
    private String webContainer = "";

    private boolean creatingResGrp = false;
    private String resourceGroup = "";

    private boolean creatingAppServicePlan = false;
    private String appServicePlanName = "";
    private String appServicePlanId = "";
    private String region = "";
    private String pricing = "";
    private JavaVersion jdkVersion = JavaVersion.JAVA_8_NEWEST;
    private String stack = "TOMCAT";
    private String version = "8.5-jre8";

    private OperatingSystem os = OperatingSystem.LINUX;

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

    public boolean isDeployToRoot() {
        return deployToRoot;
    }

    public void setDeployToRoot(boolean deployToRoot) {
        this.deployToRoot = deployToRoot;
    }

    public boolean isDeployToSlot() {
        return deployToSlot;
    }

    public void setDeployToSlot(boolean deployToSlot) {
        this.deployToSlot = deployToSlot;
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

    public String getWebAppName() {
        return webAppName;
    }

    public void setWebAppName(String webAppName) {
        this.webAppName = webAppName;
    }

    public String getSlotName() {
        return this.slotName;
    }

    public void setSlotName(final String slotName) {
        this.slotName = slotName;
    }

    public String getNewSlotName() {
        return this.newSlotName;
    }

    public void setNewSlotName(final String newSlotName) {
        this.newSlotName = newSlotName;
    }

    public String getNewSlotConfigurationSource() {
        return this.newSlotConfigurationSource;
    }

    public void setNewSlotConfigurationSource(final String newSlotConfigurationSource) {
        this.newSlotConfigurationSource = newSlotConfigurationSource;
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

    public String getAppServicePlanName() {
        return appServicePlanName;
    }

    public void setAppServicePlanName(String appServicePlan) {
        this.appServicePlanName = appServicePlan;
    }

    public String getAppServicePlanId() {
        return appServicePlanId;
    }

    public void setAppServicePlanId(String appServicePlanId) {
        this.appServicePlanId = appServicePlanId;
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

    public JavaVersion getJdkVersion() {
        return jdkVersion;
    }

    public void setJdkVersion(JavaVersion jdkVersion) {
        this.jdkVersion = jdkVersion;
    }

    public String getStack() {
        return this.stack;
    }

    public void setStack(final String value) {
        this.stack = value;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(final String value) {
        this.version = value;
    }

    public RuntimeStack getLinuxRuntime() {
        return new RuntimeStack(this.stack, this.version);
    }

    public OperatingSystem getOS() {
        return this.os;
    }

    public void setOS(final OperatingSystem value) {
        this.os = value;
    }

    public Map<String, String> getTelemetryProperties(Map<String, String> properties) {
        Map<String, String> result = new HashMap<>();
        try {
            if (properties != null) {
                result.putAll(properties);
            }
            result.put(TelemetryConstants.RUNTIME,
                os == OperatingSystem.LINUX ? "linux-" + getLinuxRuntime()
                    .toString() : "windows-" + getWebContainer());
            result.put(TelemetryConstants.WEBAPP_DEPLOY_TO_SLOT, String.valueOf(isDeployToSlot()));
            result.put(TelemetryConstants.SUBSCRIPTIONID, getSubscriptionId());
            result.put(TelemetryConstants.CREATE_NEWWEBAPP, String.valueOf(isCreatingNew()));
            result.put(TelemetryConstants.CREATE_NEWASP, String.valueOf(isCreatingAppServicePlan()));
            result.put(TelemetryConstants.CREATE_NEWRG, String.valueOf(isCreatingResGrp()));
            result.put(TelemetryConstants.FILETYPE, WebAppUtils.getFileType(getTargetName()));
        } catch (Exception ignore) {
        }
        return result;
    }

}
