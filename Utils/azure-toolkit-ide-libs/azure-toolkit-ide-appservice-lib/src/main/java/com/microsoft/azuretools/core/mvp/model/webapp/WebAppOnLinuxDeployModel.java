/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.webapp;

import com.azure.resourcemanager.appservice.models.LogLevel;
import com.microsoft.azuretools.core.mvp.model.container.pojo.DockerHostRunSetting;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
    private String appServicePlanResourceGroupName;
    private String appServicePlanName;
    // deprecated
    private String targetPath;
    private String targetName;
    private String dockerFilePath;

    // web server log
    private boolean enableWebServerLogging = false;
    private Integer webServerLogQuota = 35;
    private Integer webServerRetentionPeriod = null;
    private boolean enableDetailedErrorMessage = false;
    private boolean enableFailedRequestTracing = false;
    // application log
    private boolean enableApplicationLog = false;
    private String applicationLogLevel = LogLevel.ERROR.toString();
    // docker related properties
    private DockerHostRunSetting dockerHostRunSetting = new DockerHostRunSetting();
    private String containerRegistryId;

    public WebAppOnLinuxDeployModel() {
        privateRegistryImageSetting = new PrivateRegistryImageSetting();
    }
}
