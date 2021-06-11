/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.library.function;

import com.google.common.base.Preconditions;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.ApplicationInsightsComponent;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionApp.DefinitionStages.WithCreate;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.toolkit.intellij.function.runner.deploy.FunctionDeployModel;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.legacy.appservice.OperatingSystemEnum;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionExtensionVersion;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.RuntimeConfiguration;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.runtime.FunctionRuntimeHandler;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.runtime.LinuxFunctionRuntimeHandler;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.runtime.WindowsFunctionRuntimeHandler;
import com.microsoft.azure.toolkit.lib.legacy.function.utils.FunctionUtils;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

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

    private final FunctionDeployModel ctx;
    private final Operation operation;
    private final IAzureMessager messenger;

    public CreateFunctionHandler(FunctionDeployModel ctx, Operation operation) {
        Preconditions.checkNotNull(ctx);
        this.ctx = ctx;
        this.operation = operation;
        this.messenger = AzureMessager.getMessager();
    }

    public FunctionApp execute() {
        final FunctionApp app = getFunctionApp();
        if (app == null) {
            return createFunctionApp();
        } else {
            final String error = message("function.create.error.targetExists", ctx.getAppName());
            final String action = "change the name of the web app and try later";
            throw new AzureToolkitRuntimeException(error, action);
        }
    }
    // endregion

    // region Create or update Azure Functions

    @AzureOperation(
        name = "function.create_detail",
        params = {"this.ctx.getAppName()"},
        type = AzureOperation.Type.SERVICE
    )
    private FunctionApp createFunctionApp() {
        messenger.info(message("function.create.hint.startCreateFunction"));
        final WithCreate withCreate;
        try {
            final FunctionRuntimeHandler runtimeHandler = getFunctionRuntimeHandler();
            withCreate = runtimeHandler.defineAppWithRuntime();
        } catch (final AzureExecutionException e) {
            final String error = String.format("failed to initialize configuration to create web app[%s]", this.ctx.getAppName());
            final String action = "confirm if the web app is properly configured";
            throw new AzureToolkitRuntimeException(error, e, action);
        }
        configureApplicationLog(withCreate);

        final Map<String, String> appSettings = getAppSettingsWithDefaultValue();
        appSettings.putAll(bindingApplicationInsights());
        withCreate.withAppSettings(appSettings);

        final FunctionApp result = withCreate.create();
        operation.trackProperty("pricingTier", ctx.getPricingTier());
        messenger.info(message("function.create.hint.functionCreated", ctx.getAppName()));
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
        name = "function|ai.create",
        params = {"this.ctx.getAppName()"},
        type = AzureOperation.Type.SERVICE
    )
    private Map<String, String> bindingApplicationInsights() {
        final boolean disableAppInsights = StringUtils.isAllEmpty(ctx.getInsightsName(), ctx.getInstrumentationKey());
        operation.trackProperty("disableAppInsights", String.valueOf(disableAppInsights));
        if (disableAppInsights) {
            return Collections.emptyMap();
        }
        String instrumentationKey = ctx.getInstrumentationKey();
        if (StringUtils.isEmpty(instrumentationKey)) {
            final String region = ctx.getRegion();
            final ApplicationInsightsComponent insights;
            try {
                insights = AzureSDKManager.getOrCreateApplicationInsights(ctx.getSubscription(), ctx.getResourceGroup(), ctx.getInsightsName(), region);
                instrumentationKey = insights.instrumentationKey();
            } catch (final IOException | RuntimeException e) {
                // swallow exception for application insights, which should not block function creation
                messenger.warning(message("function.create.error.createApplicationInsightsFailed", ctx.getAppName()));
            }
        }
        return Collections.singletonMap(APP_INSIGHTS_INSTRUMENTATION_KEY, instrumentationKey);
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
                throw new AzureExecutionException(message("function.create.error.invalidRuntime", os));
        }
        return builder.appName(ctx.getAppName()).resourceGroup(ctx.getResourceGroup()).runtime(ctx.getRuntime())
                      .region(Region.fromName(ctx.getRegion())).pricingTier(getPricingTier())
                      .servicePlanName(ctx.getAppServicePlanName())
                      .servicePlanResourceGroup(ctx.getAppServicePlanResourceGroup())
                      .functionExtensionVersion(getFunctionExtensionVersion())
                      .azure(this.ctx.getAzureClient())
                      .javaVersion(JavaVersion.fromString(ctx.getJavaVersion()))
                      .build();
    }

    private OperatingSystemEnum getOsEnum() throws AzureExecutionException {
        final RuntimeConfiguration runtime = ctx.getRuntime();
        if (runtime != null && StringUtils.isNotBlank(runtime.getOs())) {
            return OperatingSystemEnum.fromString(runtime.getOs());
        }
        return DEFAULT_OS;
    }

    private void setDefaultAppSetting(Map result, String settingName, String settingIsEmptyMessage,
                                      String settingValue) {

        final String setting = (String) result.get(settingName);
        if (StringUtils.isEmpty(setting)) {
            messenger.info(settingIsEmptyMessage);
            result.put(settingName, settingValue);
        }
    }

    private void overrideDefaultAppSetting(Map result, String settingName, String settingIsEmptyMessage,
                                           String settingValue, String changeSettingMessage) {

        final String setting = (String) result.get(settingName);
        if (StringUtils.isEmpty(setting)) {
            messenger.info(settingIsEmptyMessage);
        } else if (!setting.equals(settingValue)) {
            messenger.warning(String.format(changeSettingMessage, setting));
        }
        result.put(settingName, settingValue);
    }

    public com.microsoft.azure.management.appservice.PricingTier getPricingTier() {
        if (StringUtils.isEmpty(ctx.getPricingTier())) {
            return null;
        }

        //TODO(hanli): replace track1 PricingTier to track2
        return PricingTier.BASIC_B2;
    }

    @AzureOperation(
        name = "function.get.rg",
        params = {"this.ctx.getAppName()", "this.ctx.getResourceGroup()"},
        type = AzureOperation.Type.TASK
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
        final Map settings =
            com.microsoft.azure.toolkit.intellij.function.runner.core.FunctionUtils.loadAppSettingsFromSecurityStorage(ctx.getAppSettingsKey());
        overrideDefaultAppSetting(settings, FUNCTIONS_WORKER_RUNTIME_NAME, message("function.hint.setFunctionWorker"),
                                  FUNCTIONS_WORKER_RUNTIME_VALUE, message("function.hint.changeFunctionWorker"));
        setDefaultAppSetting(settings, FUNCTIONS_EXTENSION_VERSION_NAME, message("function.hint.setFunctionVersion"),
                             FUNCTIONS_EXTENSION_VERSION_VALUE);
        com.microsoft.azure.toolkit.intellij.function.runner.core.FunctionUtils.saveAppSettingsToSecurityStorage(ctx.getAppSettingsKey(), settings);
        return settings;
    }
}
