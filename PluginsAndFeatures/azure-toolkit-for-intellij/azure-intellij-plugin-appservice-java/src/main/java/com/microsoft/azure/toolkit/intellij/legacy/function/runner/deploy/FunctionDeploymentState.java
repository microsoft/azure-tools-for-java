/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandlerMessenger;
import com.microsoft.azure.toolkit.intellij.connector.function.FunctionSupported;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunProfileState;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.legacy.function.FunctionAppService;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionConfiguration;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.RunProcessHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class FunctionDeploymentState extends AzureRunProfileState<FunctionAppBase<?, ?, ?>> {

    private static final int LIST_TRIGGERS_MAX_RETRY = 5;
    private static final int LIST_TRIGGERS_RETRY_PERIOD_IN_SECONDS = 10;
    private static final String AUTH_LEVEL = "authLevel";
    private static final String HTTP_TRIGGER = "httpTrigger";
    private static final String HTTP_TRIGGER_URLS = "HTTP Trigger Urls:";
    private static final String NO_ANONYMOUS_HTTP_TRIGGER = "No anonymous HTTP Triggers found in deployed function app, skip list triggers.";
    private static final String UNABLE_TO_LIST_NONE_ANONYMOUS_HTTP_TRIGGERS = "Some http trigger urls cannot be displayed " +
            "because they are non-anonymous. To access the non-anonymous triggers, please refer https://aka.ms/azure-functions-key.";
    private static final String FAILED_TO_LIST_TRIGGERS = "Deployment succeeded, but failed to list http trigger urls.";
    private static final String SYNCING_TRIGGERS = "Syncing triggers and fetching function information";
    private static final String SYNCING_TRIGGERS_WITH_RETRY = "Syncing triggers and fetching function information (Attempt {0}/{1})...";
    private static final String NO_TRIGGERS_FOUNDED = "No triggers found in deployed function app";

    private final FunctionDeployConfiguration functionDeployConfiguration;
    private final FunctionDeployModel deployModel;
    private File stagingFolder;

    /**
     * Place to execute the Web App deployment task.
     */
    public FunctionDeploymentState(Project project, FunctionDeployConfiguration functionDeployConfiguration) {
        super(project);
        this.functionDeployConfiguration = functionDeployConfiguration;
        this.deployModel = functionDeployConfiguration.getModel();
    }

    @Nullable
    @Override
    @AzureOperation(name = "internal/function.deploy_app")
    public FunctionAppBase<?, ?, ?> executeSteps(@NotNull RunProcessHandler processHandler, @NotNull Operation operation) {
        final RunProcessHandlerMessenger messenger = new RunProcessHandlerMessenger(processHandler);
        OperationContext.current().setMessager(messenger);
        applyResourceConnection();
        final FunctionAppBase<?, ?, ?> target = FunctionAppService.getInstance().createOrUpdateFunctionApp(deployModel.getFunctionAppConfig());
        stagingFolder = FunctionUtils.getTempStagingFolder();
        prepareStagingFolder(stagingFolder, processHandler, operation);
        // deploy function to Azure
        FunctionAppService.getInstance().deployFunctionApp(target, stagingFolder);
        operation.trackProperties(OperationContext.action().getTelemetryProperties());
        return target;
    }

    private void applyResourceConnection() {
        if (CollectionUtils.isEmpty(functionDeployConfiguration.getConnections())) {
            return;
        }
        functionDeployConfiguration.getConnections().stream()
                .filter(connection -> connection.getResource().getDefinition() instanceof FunctionSupported)
                .forEach(connection -> ((FunctionSupported) connection.getResource().getDefinition())
                        .getPropertiesForFunction(connection.getResource().getData(), connection)
                        .forEach((key, value) -> functionDeployConfiguration.getConfig().getAppSettings().put(key.toString(), value.toString())));
    }

    @AzureOperation(name = "boundary/function.prepare_staging_folder.folder|app", params = {"stagingFolder.getName()", "this.deployModel.getFunctionAppConfig().getName()"})
    private void prepareStagingFolder(File stagingFolder, RunProcessHandler processHandler, final @NotNull Operation operation) {
        final Module module = functionDeployConfiguration.getModule();
        if (module == null) {
            throw new AzureToolkitRuntimeException("Cannot find a valid module in function deploy configuration.");
        }
        final Path hostJsonPath = FunctionUtils.getDefaultHostJson(project);
        final PsiMethod[] methods = ReadAction.compute(() -> FunctionUtils.findFunctionsByAnnotation(module));
        final Path folder = stagingFolder.toPath();
        try {
            final Map<String, FunctionConfiguration> configMap =
                    FunctionUtils.prepareStagingFolder(folder, hostJsonPath, project, module, methods);
            operation.trackProperty(TelemetryConstants.TRIGGER_TYPE, StringUtils.join(FunctionUtils.getFunctionBindingList(configMap), ","));
        } catch (final AzureToolkitRuntimeException e) {
            throw e;
        } catch (final Throwable e) {
            final String error = String.format("failed prepare staging folder[%s]", folder);
            throw new AzureToolkitRuntimeException(error, e);
        }
    }

    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.FUNCTION, TelemetryConstants.DEPLOY_FUNCTION_APP);
    }

    @Override
    @AzureOperation(name = "boundary/function.complete_deployment.app", params = {"this.deployModel.getFunctionAppConfig().getName()"})
    protected void onSuccess(FunctionAppBase<?, ?, ?> result, @NotNull RunProcessHandler processHandler) {
        processHandler.setText(message("appService.deploy.hint.succeed"));
        processHandler.notifyComplete();
        functionDeployConfiguration.setAppSettings(result.getAppSettings());
        FunctionUtils.cleanUpStagingFolder(stagingFolder);
    }

    @Override
    protected void onFail(@NotNull Throwable error, @NotNull RunProcessHandler processHandler) {
        super.onFail(error, processHandler);
        FunctionUtils.cleanUpStagingFolder(stagingFolder);
    }

    @Override
    protected Map<String, String> getTelemetryMap() {
        return deployModel.getTelemetryProperties();
    }
}
