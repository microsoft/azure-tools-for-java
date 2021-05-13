/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.deploy;

import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.LogLevel;
import com.microsoft.azure.toolkit.intellij.function.FunctionAppComboBoxModel;
import com.microsoft.azure.toolkit.lib.appservice.ApplicationInsightsConfig;
import com.microsoft.azure.toolkit.lib.appservice.MonitorConfig;
import com.microsoft.azure.toolkit.lib.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.intellij.function.runner.IntelliJFunctionContext;
import com.microsoft.azure.toolkit.intellij.function.runner.core.FunctionUtils;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FunctionDeployModel extends IntelliJFunctionContext {

    private boolean isNewResource;
    private String functionId;
    private boolean enableApplicationLog;
    private LogLevel applicationLogLevel;

    public FunctionDeployModel() {

    }

    public void saveModel(FunctionAppComboBoxModel functionAppComboBoxModel) {
        setFunctionId(functionAppComboBoxModel.getResourceId());
        setAppName(functionAppComboBoxModel.getAppName());
        setResourceGroup(functionAppComboBoxModel.getResourceGroup());
        setSubscription(functionAppComboBoxModel.getSubscriptionId());
        if (functionAppComboBoxModel.isNewCreateResource() && functionAppComboBoxModel.getFunctionAppConfig() != null) {
            setNewResource(true);
            final FunctionAppConfig functionAppConfig = functionAppComboBoxModel.getFunctionAppConfig();
            setAppServicePlanName(functionAppConfig.getServicePlan().name());
            setAppServicePlanResourceGroup(functionAppConfig.getServicePlan().resourceGroupName());
            setPricingTier(functionAppConfig.getServicePlan().pricingTier().toSkuDescription().size());
            setRegion(functionAppConfig.getRegion().getName());
            setOs(functionAppConfig.getPlatform().getOs().name());
            setJavaVersion(functionAppConfig.getPlatform().getStackVersionOrJavaVersion());
            final MonitorConfig monitorConfig = functionAppConfig.getMonitorConfig();
            final ApplicationInsightsConfig insightsModel = monitorConfig.getApplicationInsightsConfig();
            if (insightsModel != null) {
                setInsightsName(insightsModel.getName());
                setInstrumentationKey(insightsModel.getInstrumentationKey());
            } else {
                setInsightsName(null);
                setInstrumentationKey(null);
            }
            setEnableApplicationLog(monitorConfig.isEnableApplicationLog());
            setApplicationLogLevel(monitorConfig.getApplicationLogLevel());
        } else {
            setNewResource(false);
            final FunctionApp functionApp = functionAppComboBoxModel.getResource();
            if (functionApp != null) {
                setRegion(functionApp.regionName());
                setOs(functionApp.operatingSystem().name());
                setJavaVersion(FunctionUtils.getFunctionJavaVersion(functionApp));
            }
        }
    }

}
