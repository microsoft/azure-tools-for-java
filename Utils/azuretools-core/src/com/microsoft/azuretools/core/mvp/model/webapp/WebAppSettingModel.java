/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.webapp;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.LogLevel;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.utils.WebAppUtils;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class WebAppSettingModel {

    // common settings
    private boolean creatingNew = false;
    private String subscriptionId = "";
    // deploy related
    private String webAppId = "";
    private String targetPath = "";
    private String targetName = "";
    private String projectBase = "";
    private String projectType = "";
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
    // web server log
    private boolean enableWebServerLogging = false;
    private Integer webServerLogQuota = 35;
    private Integer webServerRetentionPeriod = null;
    private boolean enableDetailedErrorMessage = false;
    private boolean enableFailedRequestTracing = false;
    // application log
    private boolean enableApplicationLog = false;
    private LogLevel applicationLogLevel = LogLevel.ERROR;
    private OperatingSystem os = OperatingSystem.LINUX;

    public RuntimeStack getLinuxRuntime() {
        return new RuntimeStack(this.stack, this.version);
    }

    public OperatingSystem getOS() {
        return this.os;
    }

    public void setOS(OperatingSystem os) {
        this.os = os;
    }

    public Map<String, String> getTelemetryProperties(Map<String, String> properties) {
        Map<String, String> result = new HashMap<>();
        try {
            if (properties != null) {
                result.putAll(properties);
            }
            result.put(TelemetryConstants.RUNTIME, os == OperatingSystem.LINUX ?
                    "linux-" + getLinuxRuntime().toString() : "windows-" + getWebContainer());
            result.put(TelemetryConstants.WEBAPP_DEPLOY_TO_SLOT, String.valueOf(isDeployToSlot()));
            result.put(TelemetryConstants.SUBSCRIPTIONID, getSubscriptionId());
            result.put(TelemetryConstants.CREATE_NEWWEBAPP, String.valueOf(isCreatingNew()));
            result.put(TelemetryConstants.CREATE_NEWASP, String.valueOf(isCreatingAppServicePlan()));
            result.put(TelemetryConstants.CREATE_NEWRG, String.valueOf(isCreatingResGrp()));
            result.put(TelemetryConstants.FILETYPE, WebAppUtils.getFileType(getTargetName()));
        } catch (final Exception ignore) {
        }
        return result;
    }

}
