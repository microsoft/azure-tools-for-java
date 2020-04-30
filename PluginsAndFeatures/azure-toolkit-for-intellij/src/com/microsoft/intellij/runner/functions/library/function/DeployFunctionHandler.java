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
import com.microsoft.azure.common.appservice.DeployTargetType;
import com.microsoft.azure.common.appservice.DeploymentType;
import com.microsoft.azure.common.appservice.OperatingSystemEnum;
import com.microsoft.azure.common.deploytarget.DeployTarget;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.common.function.configurations.RuntimeConfiguration;
import com.microsoft.azure.common.function.handlers.artifact.DockerArtifactHandler;
import com.microsoft.azure.common.function.handlers.artifact.MSDeployArtifactHandlerImpl;
import com.microsoft.azure.common.function.handlers.artifact.RunFromBlobArtifactHandlerImpl;
import com.microsoft.azure.common.function.handlers.artifact.RunFromZipArtifactHandlerImpl;
import com.microsoft.azure.common.handlers.ArtifactHandler;
import com.microsoft.azure.common.handlers.artifact.ArtifactHandlerBase;
import com.microsoft.azure.common.handlers.artifact.FTPArtifactHandlerImpl;
import com.microsoft.azure.common.handlers.artifact.ZIPArtifactHandlerImpl;
import com.microsoft.azure.common.utils.AppServiceUtils;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionApp.Update;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.intellij.runner.functions.library.IAppServiceContext;
import com.microsoft.intellij.runner.functions.library.IPrompter;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.function.Consumer;

import static com.microsoft.azure.common.appservice.DeploymentType.DOCKER;
import static com.microsoft.azure.common.appservice.DeploymentType.RUN_FROM_BLOB;
import static com.microsoft.azure.common.appservice.DeploymentType.RUN_FROM_ZIP;

/**
 * Deploy artifacts to target Azure Functions in Azure. If target Azure
 * Functions doesn't exist, it will be created.
 */
public class DeployFunctionHandler {
    private static final String FUNCTIONS_WORKER_RUNTIME_NAME = "FUNCTIONS_WORKER_RUNTIME";
    private static final String FUNCTIONS_WORKER_RUNTIME_VALUE = "java";
    private static final String SET_FUNCTIONS_WORKER_RUNTIME = "Set function worker runtime to java";
    private static final String CHANGE_FUNCTIONS_WORKER_RUNTIME = "Function worker runtime doesn't " +
            "meet the requirement, change it from %s to java";
    private static final String FUNCTIONS_EXTENSION_VERSION_NAME = "FUNCTIONS_EXTENSION_VERSION";
    private static final String FUNCTIONS_EXTENSION_VERSION_VALUE = "~3";
    private static final String SET_FUNCTIONS_EXTENSION_VERSION = "Functions extension version " +
            "isn't configured, setting up the default value";
    private static final JavaVersion DEFAULT_JAVA_VERSION = JavaVersion.JAVA_8_NEWEST;
    private static final String VALID_JAVA_VERSION_PATTERN = "^1\\.8.*"; // For now we only support function with java 8

    private static final String DEPLOY_START = "Trying to deploy the function app...";
    private static final String DEPLOY_FINISH = "Successfully deployed the function app at https://%s.azurewebsites.net";
    private static final String FUNCTION_APP_UPDATE = "Updating the specified function app...";
    private static final String FUNCTION_APP_UPDATE_DONE = "Successfully updated the function app %s.";
    private static final String HOST_JAVA_VERSION = "Java version of function host : %s";
    private static final String HOST_JAVA_VERSION_OFF = "Java version of function host is not initiated," +
            " set it to Java 8";
    private static final String HOST_JAVA_VERSION_INCORRECT = "Java version of function host %s does not" +
            " meet the requirement of Azure Functions, set it to Java 8";
    private static final String UNKNOW_DEPLOYMENT_TYPE = "The value of <deploymentType> is unknown, supported values are: " +
            "ftp, zip, msdeploy, run_from_blob and run_from_zip.";

    private static final OperatingSystemEnum DEFAULT_OS = OperatingSystemEnum.Windows;
    private IAppServiceContext ctx;
    private IPrompter prompter;

    public DeployFunctionHandler(IAppServiceContext ctx, IPrompter prompter) {
        Preconditions.checkNotNull(ctx);
        this.ctx = ctx;
        this.prompter = prompter;
    }

    public FunctionApp execute() throws Exception {

        final FunctionApp app = getFunctionApp();
        updateFunctionAppSettings(app);
        final DeployTarget deployTarget = new DeployTarget(app, DeployTargetType.FUNCTION);
        prompt(DEPLOY_START);
        getArtifactHandler().publish(deployTarget);
        prompt(String.format(DEPLOY_FINISH, ctx.getAppName()));
        return (FunctionApp) deployTarget.getApp();
    }

    // endregion

    // region Create or update Azure Functions
    private void updateFunctionAppSettings(final FunctionApp app) throws AzureExecutionException {
        prompt(FUNCTION_APP_UPDATE);
        // Work around of https://github.com/Azure/azure-sdk-for-java/issues/1755
        final Update update = app.update();
        checkHostJavaVersion(app, update); // Check Java Version of Server
        configureAppSettings(update::withAppSettings, getAppSettingsWithDefaultValue());
        update.apply();
        prompt(String.format(FUNCTION_APP_UPDATE_DONE, ctx.getAppName()));
    }

    private void checkHostJavaVersion(final FunctionApp app, final Update update) {
        final JavaVersion serverJavaVersion = app.javaVersion();
        if (serverJavaVersion.toString().matches(VALID_JAVA_VERSION_PATTERN)) {
            prompt(String.format(HOST_JAVA_VERSION, serverJavaVersion));
        } else if (serverJavaVersion.equals(JavaVersion.OFF)) {
            prompt(HOST_JAVA_VERSION_OFF);
            update.withJavaVersion(DEFAULT_JAVA_VERSION);
        } else {
            prompt(HOST_JAVA_VERSION_INCORRECT);
            update.withJavaVersion(DEFAULT_JAVA_VERSION);
        }
    }

    private void configureAppSettings(final Consumer<Map> withAppSettings, final Map appSettings) {
        if (appSettings != null && !appSettings.isEmpty()) {
            withAppSettings.accept(appSettings);
        }
    }

    // endregion

    private OperatingSystemEnum getOsEnum() throws AzureExecutionException {
        final RuntimeConfiguration runtime = ctx.getRuntime();
        if (runtime != null && StringUtils.isNotBlank(runtime.getOs())) {
            return Utils.parseOperationSystem(runtime.getOs());
        }
        return DEFAULT_OS;
    }

    private DeploymentType getDeploymentType() throws AzureExecutionException {
        final DeploymentType deploymentType = DeploymentType.fromString(ctx.getDeploymentType());
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
        return AppServiceUtils.getPricingTierFromString(ctx.getPricingTier()) != null;
    }

    private FunctionApp getFunctionApp() {
        try {
            return ctx.getAzureClient().appServices().functionApps().getByResourceGroup(ctx.getResourceGroup(),
                    ctx.getAppName());
        } catch (Exception ex) {
            // Swallow exception for non-existing Azure Functions
        }
        return null;
    }

    // region get App Settings
    private Map getAppSettingsWithDefaultValue() {
        final Map settings = ctx.getAppSettings();
        overrideDefaultAppSetting(settings, FUNCTIONS_WORKER_RUNTIME_NAME, SET_FUNCTIONS_WORKER_RUNTIME,
                FUNCTIONS_WORKER_RUNTIME_VALUE, CHANGE_FUNCTIONS_WORKER_RUNTIME);
        setDefaultAppSetting(settings, FUNCTIONS_EXTENSION_VERSION_NAME, SET_FUNCTIONS_EXTENSION_VERSION,
                FUNCTIONS_EXTENSION_VERSION_VALUE);
        return settings;
    }

    private void setDefaultAppSetting(Map result, String settingName, String settingIsEmptyMessage,
                                      String settingValue) {

        final String setting = (String) result.get(settingName);
        if (StringUtils.isEmpty(setting)) {
            prompt(settingIsEmptyMessage);
            result.put(settingName, settingValue);
        }
    }

    private void overrideDefaultAppSetting(Map result, String settingName, String settingIsEmptyMessage,
                                           String settingValue, String changeSettingMessage) {

        final String setting = (String) result.get(settingName);
        if (StringUtils.isEmpty(setting)) {
            prompt(settingIsEmptyMessage);
        } else if (!setting.equals(settingValue)) {
            prompt(String.format(changeSettingMessage, setting));
        }
        result.put(settingName, settingValue);
    }

    private ArtifactHandler getArtifactHandler() throws AzureExecutionException {
        final ArtifactHandlerBase.Builder builder;

        final DeploymentType deploymentType = getDeploymentType();
        switch (deploymentType) {
            case MSDEPLOY:
                builder = new MSDeployArtifactHandlerImpl.Builder().functionAppName(this.ctx.getAppName());
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
                throw new AzureExecutionException(UNKNOW_DEPLOYMENT_TYPE);
        }
        return builder
                .stagingDirectoryPath(this.ctx.getDeploymentStagingDirectoryPath())
                .build();
    }

    private void prompt(String promptMessage) {
        prompter.prompt(promptMessage);
    }
}
