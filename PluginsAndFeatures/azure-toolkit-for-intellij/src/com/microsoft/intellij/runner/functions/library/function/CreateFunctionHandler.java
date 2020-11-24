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

package com.microsoft.intellij.runner.functions.library.function;

import com.google.common.base.Preconditions;
import com.microsoft.azure.common.Utils;
import com.microsoft.azure.common.appservice.OperatingSystemEnum;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.function.configurations.ElasticPremiumPricingTier;
import com.microsoft.azure.common.function.configurations.FunctionExtensionVersion;
import com.microsoft.azure.common.function.configurations.RuntimeConfiguration;
import com.microsoft.azure.common.function.handlers.runtime.FunctionRuntimeHandler;
import com.microsoft.azure.common.function.handlers.runtime.LinuxFunctionRuntimeHandler;
import com.microsoft.azure.common.function.handlers.runtime.WindowsFunctionRuntimeHandler;
import com.microsoft.azure.common.function.utils.FunctionUtils;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.common.utils.AppServiceUtils;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.ApplicationInsightsComponent;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.intellij.runner.functions.deploy.FunctionDeployModel;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

/**
 * Deploy artifacts to target Azure Functions in Azure. If target Azure
 * Functions doesn't exist, it will be created.
 */
public class CreateFunctionHandler {
    private static final String FUNCTIONS_WORKER_RUNTIME_NAME = "FUNCTIONS_WORKER_RUNTIME";
    private static final String FUNCTIONS_WORKER_RUNTIME_VALUE = "java";
    private static final String FUNCTIONS_EXTENSION_VERSION_NAME = "FUNCTIONS_EXTENSION_VERSION";
    private static final String FUNCTIONS_EXTENSION_VERSION_VALUE = "~3";

    private static final OperatingSystemEnum DEFAULT_OS = OperatingSystemEnum.Windows;
    private static final String APP_INSIGHTS_INSTRUMENTATION_KEY = "APPINSIGHTS_INSTRUMENTATIONKEY";

    private FunctionDeployModel ctx;

    public CreateFunctionHandler(FunctionDeployModel ctx) {
        Preconditions.checkNotNull(ctx);
        this.ctx = ctx;
    }

    public FunctionApp execute() {
        final FunctionApp app = getFunctionApp();
        if (app == null) {
            return createFunctionApp();
        } else {
            final String error = String.format(message("function.create.error.targetExists"), ctx.getAppName());
            final String action = "change the name of the web app and try later";
            throw new AzureToolkitRuntimeException(error, action);
        }
    }
    // endregion

    // region Create or update Azure Functions

    @AzureOperation(
        value = "create function app[%s, rg=%s, sp=%s] in subscription[%s]",
        params = {"@ctx.getAppName()", "@ctx.getResourceGroup()", "@ctx.getAppServicePlanName()", "@ctx.getSubscription()"},
        type = AzureOperation.Type.SERVICE
    )
    private FunctionApp createFunctionApp() {
        Log.prompt(message("function.create.hint.startCreateFunction"));
        final WithCreate withCreate;
        try {
            final FunctionRuntimeHandler runtimeHandler = getFunctionRuntimeHandler();
            withCreate = runtimeHandler.defineAppWithRuntime();
        } catch (final AzureExecutionException e) {
            final String error = String.format("failed to initialize configuration to create web app[%s]", this.ctx.getAppName());
            final String action = "confirm if the web app is properly configured";
            throw new AzureToolkitRuntimeException(error, e, action);
        }
        bindingApplicationInsights();
        configureApplicationLog(withCreate);
        configureAppSettings(withCreate::withAppSettings, getAppSettingsWithDefaultValue());
        FunctionApp result = withCreate.create();
        Log.prompt(String.format(message("function.create.hint.functionCreated"), ctx.getAppName()));
        return result;
    }

    private WithCreate configureApplicationLog(WithCreate withCreate) {
        if (ctx.isEnableApplicationLog()) {
            return (WithCreate) withCreate.defineDiagnosticLogsConfiguration()
                                          .withApplicationLogging()
                                          .withLogLevel(ctx.getApplicationLogLevel())
                                          .withApplicationLogsStoredOnFileSystem().attach();
        }
        return withCreate;
    }

    @AzureOperation(
        value = "create application insights for function[%s]",
        params = {"@ctx.getAppName()"},
        type = AzureOperation.Type.SERVICE
    )
    private void bindingApplicationInsights() {
        if (StringUtils.isAllEmpty(ctx.getInsightsName(), ctx.getInstrumentationKey())) {
            return;
        }
        String instrumentationKey = ctx.getInstrumentationKey();
        if (StringUtils.isEmpty(instrumentationKey)) {
            final String region = ctx.getRegion();
            final ApplicationInsightsComponent insights;
            try {
                insights = AzureSDKManager.getOrCreateApplicationInsights(ctx.getSubscription(), ctx.getResourceGroup(), ctx.getInsightsName(), region);
                instrumentationKey = insights.instrumentationKey();
            } catch (final IOException e) {
                Log.prompt(String.format(message("function.create.error.createApplicationInsightsFailed"), ctx.getAppName()));
            }
        }
        ctx.getAppSettings().put(APP_INSIGHTS_INSTRUMENTATION_KEY, instrumentationKey);
    }

    private void configureAppSettings(final Consumer<Map> withAppSettings, final Map appSettings) {
        if (appSettings != null && !appSettings.isEmpty()) {
            withAppSettings.accept(appSettings);
        }
    }

    // endregion

    private FunctionRuntimeHandler getFunctionRuntimeHandler() throws AzureExecutionException {
        final FunctionRuntimeHandler.Builder<?> builder;
        final OperatingSystemEnum os = getOsEnum();
        switch (os) {
            case Windows:
                builder = new WindowsFunctionRuntimeHandler.Builder();
                break;
            case Linux:
                builder = new LinuxFunctionRuntimeHandler.Builder();
                break;
            case Docker:
                throw new UnsupportedOperationException(message("function.create.error.dockerNotSupport"));
            default:
                throw new AzureExecutionException(String.format(message("function.create.error.invalidRuntime"), os));
        }
        return builder.appName(ctx.getAppName()).resourceGroup(ctx.getResourceGroup()).runtime(ctx.getRuntime())
                      .region(Region.fromName(ctx.getRegion())).pricingTier(getPricingTier())
                      .servicePlanName(ctx.getAppServicePlanName())
                      .servicePlanResourceGroup(ctx.getAppServicePlanResourceGroup())
                      .functionExtensionVersion(getFunctionExtensionVersion())
                      .azure(this.ctx.getAzureClient())
                      .javaVersion(FunctionUtils.parseJavaVersion(ctx.getJavaVersion()))
                      .build();
    }

    private OperatingSystemEnum getOsEnum() throws AzureExecutionException {
        final RuntimeConfiguration runtime = ctx.getRuntime();
        if (runtime != null && StringUtils.isNotBlank(runtime.getOs())) {
            return Utils.parseOperationSystem(runtime.getOs());
        }
        return DEFAULT_OS;
    }

    private void setDefaultAppSetting(Map result, String settingName, String settingIsEmptyMessage,
                                      String settingValue) {

        final String setting = (String) result.get(settingName);
        if (StringUtils.isEmpty(setting)) {
            Log.prompt(settingIsEmptyMessage);
            result.put(settingName, settingValue);
        }
    }

    private void overrideDefaultAppSetting(Map result, String settingName, String settingIsEmptyMessage,
                                           String settingValue, String changeSettingMessage) {

        final String setting = (String) result.get(settingName);
        if (StringUtils.isEmpty(setting)) {
            Log.prompt(settingIsEmptyMessage);
        } else if (!setting.equals(settingValue)) {
            Log.warn(String.format(changeSettingMessage, setting));
        }
        result.put(settingName, settingValue);
    }

    public PricingTier getPricingTier() {
        if (StringUtils.isEmpty(ctx.getPricingTier())) {
            return null;
        }
        final String pricingTier = ctx.getPricingTier();
        final ElasticPremiumPricingTier elasticPremiumPricingTier = ElasticPremiumPricingTier.fromString(pricingTier);
        return elasticPremiumPricingTier != null ? elasticPremiumPricingTier.toPricingTier()
                : AppServiceUtils.getPricingTierFromString(pricingTier);
    }

    @AzureOperation(
        value = "get function app[%s] in resource group[%s]",
        params = {"@ctx.getAppName()", "@ctx.getResourceGroup()"},
        type = AzureOperation.Type.SERVICE
    )
    private FunctionApp getFunctionApp() {
        return ctx.getAzureClient().appServices().functionApps().getByResourceGroup(ctx.getResourceGroup(), ctx.getAppName());
    }

    private FunctionExtensionVersion getFunctionExtensionVersion() throws AzureExecutionException {
        final String extensionVersion = (String) getAppSettingsWithDefaultValue().get(FUNCTIONS_EXTENSION_VERSION_NAME);
        return FunctionUtils.parseFunctionExtensionVersion(extensionVersion);
    }

    // region get App Settings
    private Map getAppSettingsWithDefaultValue() {
        final Map settings = ctx.getAppSettings();
        overrideDefaultAppSetting(settings, FUNCTIONS_WORKER_RUNTIME_NAME, message("function.hint.setFunctionWorker"),
                FUNCTIONS_WORKER_RUNTIME_VALUE, message("function.hint.changeFunctionWorker"));
        setDefaultAppSetting(settings, FUNCTIONS_EXTENSION_VERSION_NAME, message("function.hint.setFunctionVersion"),
                FUNCTIONS_EXTENSION_VERSION_VALUE);
        return settings;
    }
}
