/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.library.function;

import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionApp.Update;
import com.microsoft.azure.toolkit.intellij.function.runner.deploy.FunctionDeployModel;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.legacy.appservice.AppServiceUtils;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeployTarget;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeployTargetType;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeploymentType;
import com.microsoft.azure.toolkit.lib.legacy.appservice.OperatingSystemEnum;
import com.microsoft.azure.toolkit.lib.legacy.appservice.handlers.ArtifactHandler;
import com.microsoft.azure.toolkit.lib.legacy.appservice.handlers.artifact.ArtifactHandlerBase;
import com.microsoft.azure.toolkit.lib.legacy.appservice.handlers.artifact.FTPArtifactHandlerImpl;
import com.microsoft.azure.toolkit.lib.legacy.appservice.handlers.artifact.ZIPArtifactHandlerImpl;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.RuntimeConfiguration;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.artifact.DockerArtifactHandler;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.artifact.MSDeployArtifactHandlerImpl;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.artifact.RunFromBlobArtifactHandlerImpl;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.artifact.RunFromZipArtifactHandlerImpl;
import com.microsoft.azure.toolkit.lib.legacy.function.model.FunctionResource;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.legacy.appservice.DeploymentType.DOCKER;
import static com.microsoft.azure.toolkit.lib.legacy.appservice.DeploymentType.RUN_FROM_BLOB;
import static com.microsoft.azure.toolkit.lib.legacy.appservice.DeploymentType.RUN_FROM_ZIP;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

/**
 * Deploy artifacts to target Azure Functions in Azure.
 * Todo: Move the handler to tools-common
 */
public class DeployFunctionHandler {
    private static final int LIST_TRIGGERS_MAX_RETRY = 3;
    private static final int LIST_TRIGGERS_RETRY_PERIOD_IN_SECONDS = 10;
    private static final String FUNCTIONS_WORKER_RUNTIME_NAME = "FUNCTIONS_WORKER_RUNTIME";
    private static final String FUNCTIONS_WORKER_RUNTIME_VALUE = "java";
    private static final String FUNCTIONS_EXTENSION_VERSION_NAME = "FUNCTIONS_EXTENSION_VERSION";
    private static final String FUNCTIONS_EXTENSION_VERSION_VALUE = "~3";
    private static final String AUTH_LEVEL = "authLevel";
    private static final String HTTP_TRIGGER = "httpTrigger";

    private static final OperatingSystemEnum DEFAULT_OS = OperatingSystemEnum.Windows;
    private final FunctionDeployModel model;
    private final Operation operation;
    private final IAzureMessager messenger;

    public DeployFunctionHandler(@NotNull FunctionDeployModel model, @NotNull Operation operation) {
        this.model = model;
        this.operation = operation;
        this.messenger = AzureMessager.getMessager();
    }

    @AzureOperation(name = "function.deploy_artifact", type = AzureOperation.Type.SERVICE)
    public FunctionApp execute() throws Exception {
        final FunctionApp app = getFunctionApp();
        updateFunctionAppSettings(app);
        final DeployTarget deployTarget = new DeployTarget(app, DeployTargetType.FUNCTION);
        messenger.info(message("function.deploy.hint.startDeployFunction"));
        getArtifactHandler().publish(deployTarget);
        messenger.info(message("function.deploy.hint.deployDone", model.getAppName()));
        listHTTPTriggerUrls();
        return (FunctionApp) deployTarget.getApp();
    }

    private void updateFunctionAppSettings(final FunctionApp app) {
        messenger.info(message("function.deploy.hint.updateFunctionApp"));
        // Work around of https://github.com/Azure/azure-sdk-for-java/issues/1755
        final Update update = app.update();
        configureAppSettings(update::withAppSettings, getAppSettingsWithDefaultValue());
        update.apply();
        messenger.info(message("function.deploy.hint.updateDone", model.getAppName()));
    }

    private void configureAppSettings(final Consumer<Map> withAppSettings, final Map appSettings) {
        if (appSettings != null && !appSettings.isEmpty()) {
            withAppSettings.accept(appSettings);
        }
    }

    /**
     * List anonymous HTTP Triggers url after deployment
     */
    @AzureOperation(name = "function|trigger.list_urls", type = AzureOperation.Type.SERVICE)
    private void listHTTPTriggerUrls() {
        final List<FunctionResource> triggers = listFunctions();
        final List<FunctionResource> httpFunction =
                triggers.stream()
                        .filter(function -> function.getTrigger() != null &&
                                StringUtils.equalsIgnoreCase(function.getTrigger().getType(), HTTP_TRIGGER))
                        .collect(Collectors.toList());
        final List<FunctionResource> anonymousTriggers =
                httpFunction.stream()
                            .filter(bindingResource -> bindingResource.getTrigger() != null &&
                                    StringUtils.equalsIgnoreCase(
                                            (CharSequence) bindingResource.getTrigger().getProperty(AUTH_LEVEL),
                                            AuthorizationLevel.ANONYMOUS.toString()))
                            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(httpFunction) || CollectionUtils.isEmpty(anonymousTriggers)) {
            messenger.info(message("function.deploy.hint.noAnonymousHttpTrigger"));
            return;
        }
        messenger.info(message("function.deploy.hint.httpTriggerUrls"));
        anonymousTriggers.forEach(trigger -> messenger.info(String.format("\t %s : %s", trigger.getName(), trigger.getTriggerUrl())));
        if (anonymousTriggers.size() < httpFunction.size()) {
            messenger.info(message("function.deploy.error.listHttpTriggerFailed"));
        }
    }

    /**
     * Sync triggers and return function list of deployed function app
     * Will retry when get empty result, the max retry times is LIST_TRIGGERS_MAX_RETRY
     * @return List of functions in deployed function app
     */
    @AzureOperation(
        name = "function|trigger.list",
        params = {"this.model.getAppName()"},
        type = AzureOperation.Type.SERVICE
    )
    private List<FunctionResource> listFunctions() {
        final FunctionApp functionApp = getFunctionApp();
        for (int i = 0; i < LIST_TRIGGERS_MAX_RETRY; i++) {
            try {
                Thread.sleep(LIST_TRIGGERS_RETRY_PERIOD_IN_SECONDS * 1000);
                messenger.info(String.format(message("function.deploy.hint.syncTriggers"), i + 1, LIST_TRIGGERS_MAX_RETRY));
                functionApp.syncTriggers();
                final List<FunctionResource> triggers =
                        model.getAzureClient().appServices().functionApps()
                             .listFunctions(model.getResourceGroup(), model.getAppName()).stream()
                             .map(FunctionResource::parseFunction)
                             .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(triggers)) {
                    return triggers;
                }
            } catch (final Exception exception) {
                // swallow sdk request runtime exception
            }
        }
        final String error = String.format("No triggers found in function app[%s]", model.getAppName());
        final String action = "try recompile the project by and deploy again.";
        throw new AzureToolkitRuntimeException(error, action);
    }

    private OperatingSystemEnum getOsEnum() throws AzureExecutionException {
        final RuntimeConfiguration runtime = model.getRuntime();
        if (runtime != null && StringUtils.isNotBlank(runtime.getOs())) {
            return OperatingSystemEnum.fromString(runtime.getOs());
        }
        return DEFAULT_OS;
    }

    private DeploymentType getDeploymentType() throws AzureExecutionException {
        final DeploymentType deploymentType = DeploymentType.fromString(model.getDeploymentType());
        return deploymentType == DeploymentType.EMPTY ? getDeploymentTypeByRuntime() : deploymentType;
    }

    private DeploymentType getDeploymentTypeByRuntime() throws AzureExecutionException {
        final OperatingSystemEnum operatingSystemEnum = getOsEnum();
        switch (operatingSystemEnum) {
            case Docker:
                return DOCKER;
            case Linux:
                return isDedicatedPricingTier() ? RUN_FROM_ZIP : RUN_FROM_BLOB;
            default:
                return RUN_FROM_ZIP;
        }
    }

    private boolean isDedicatedPricingTier() {
        return AppServiceUtils.getPricingTierFromString(model.getPricingTier()) != null;
    }

    private FunctionApp getFunctionApp() {
        return model.getAzureClient().appServices().functionApps().getById(model.getFunctionId());
    }

    // region get App Settings
    private Map getAppSettingsWithDefaultValue() {
        final Map settings =
            com.microsoft.azure.toolkit.intellij.function.runner.core.FunctionUtils.loadAppSettingsFromSecurityStorage(model.getAppSettingsKey());
        overrideDefaultAppSetting(settings, FUNCTIONS_WORKER_RUNTIME_NAME, message("function.hint.setFunctionWorker"),
                                  FUNCTIONS_WORKER_RUNTIME_VALUE, message("function.hint.changeFunctionWorker"));
        setDefaultAppSetting(settings, FUNCTIONS_EXTENSION_VERSION_NAME, message("function.hint.setFunctionVersion"),
                             FUNCTIONS_EXTENSION_VERSION_VALUE);
        com.microsoft.azure.toolkit.intellij.function.runner.core.FunctionUtils.saveAppSettingsToSecurityStorage(model.getAppSettingsKey(), settings);
        return settings;
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

    private ArtifactHandler getArtifactHandler() throws AzureExecutionException {
        final ArtifactHandlerBase.Builder builder;
        final DeploymentType deploymentType = getDeploymentType();
        operation.trackProperty("deploymentType", deploymentType.name());
        switch (deploymentType) {
            case MSDEPLOY:
                builder = new MSDeployArtifactHandlerImpl.Builder().functionAppName(this.model.getAppName());
                break;
            case FTP:
                builder = new FTPArtifactHandlerImpl.Builder();
                break;
            case ZIP:
                builder = new ZIPArtifactHandlerImpl.Builder();
                break;
            case RUN_FROM_BLOB:
                builder = new RunFromBlobArtifactHandlerImpl.Builder();
                break;
            case DOCKER:
                builder = new DockerArtifactHandler.Builder();
                break;
            case EMPTY:
            case RUN_FROM_ZIP:
                builder = new RunFromZipArtifactHandlerImpl.Builder();
                break;
            default:
                throw new AzureExecutionException(message("function.deploy.error.unknownType"));
        }
        return builder
                .stagingDirectoryPath(this.model.getDeploymentStagingDirectoryPath())
                .build();
    }

}
