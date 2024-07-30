/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import com.microsoft.azure.toolkit.ide.appservice.AppServiceActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandlerMessenger;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.DotEnvBeforeRunTaskProvider;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunProfileState;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.intellij.storage.connection.BaseStorageAccountResourceDefinition;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateFunctionAppTask;
import com.microsoft.azure.toolkit.lib.appservice.task.DeployFunctionAppTask;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionConfiguration;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
    private File stagingFolder;

    /**
     * Place to execute the Web App deployment task.
     */
    public FunctionDeploymentState(Project project, FunctionDeployConfiguration functionDeployConfiguration) {
        super(project);
        this.functionDeployConfiguration = functionDeployConfiguration;
    }

    @Nullable
    @Override
    @AzureOperation(name = "internal/function.deploy_app")
    public FunctionAppBase<?, ?, ?> executeSteps(@NotNull RunProcessHandler processHandler, @NotNull Operation operation) {
        final IAzureMessager messenger = AzureMessager.getDefaultMessager();
        OperationContext.current().setMessager(new RunProcessHandlerMessenger(processHandler));
        applyResourceConnection();
        final FunctionAppBase<?, ?, ?> target = createOrUpdateFunctionApp(functionDeployConfiguration.getConfig());
        if (target instanceof AzResource.Draft<?, ?> draft) {
            draft.reset();
        }
        functionDeployConfiguration.setAppSettings(target.getAppSettings()); // save app settings
        stagingFolder = FunctionUtils.getTempStagingFolder();
        prepareStagingFolder(stagingFolder, operation);
        // deploy function to Azure
        deployFunctionApp(target, stagingFolder, messenger);
        operation.trackProperties(OperationContext.action().getTelemetryProperties());
        return target;
    }

    private void applyResourceConnection() {
        if (functionDeployConfiguration.isConnectionEnabled()) {
            final DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask loadDotEnvBeforeRunTask = functionDeployConfiguration.getLoadDotEnvBeforeRunTask();
            final Map<String, String> appSettings = functionDeployConfiguration.getConfig().appSettings();
            loadDotEnvBeforeRunTask.loadEnv().stream()
                                   .filter(pair -> !(StringUtils.equalsIgnoreCase(pair.getKey(), "AzureWebJobsStorage") &&
                                       StringUtils.equalsIgnoreCase(pair.getValue(), BaseStorageAccountResourceDefinition.LOCAL_STORAGE_CONNECTION_STRING))) // workaround to remove local connections
                                   .forEach(env -> appSettings.put(env.getKey(), env.getValue()));
        }
    }

    public FunctionAppBase<?, ?, ?> createOrUpdateFunctionApp(final FunctionAppConfig config) {
        final CreateOrUpdateFunctionAppTask task = new CreateOrUpdateFunctionAppTask(config);
        final FunctionAppBase<?, ?, ?> result = task.execute();
        final AzureModule module = Optional.ofNullable(this.functionDeployConfiguration.getModule()).map(AzureModule::from).orElse(null);
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        if (Objects.nonNull(module)) {
            final AbstractAzResource<?, ?, ?> target = result instanceof FunctionAppDeploymentSlot ? result.getParent() : result;
            tm.runOnPooledThread(() -> tm.runLater(() -> tm.write(() -> module.initializeWithDefaultProfileIfNot().addApp(target).save())));
        }
        return result;
    }

    @AzureOperation(name = "boundary/function.prepare_staging_folder.folder|app", params = {"stagingFolder.getName()", "this.deployModel.getFunctionAppConfig().getName()"})
    private void prepareStagingFolder(File stagingFolder, final @NotNull Operation operation) {
        final Module module = functionDeployConfiguration.getModule();
        if (module == null) {
            throw new AzureToolkitRuntimeException("Module was not valid in function deploy configuration.");
        }
        final Path hostJsonPath = Optional.ofNullable(functionDeployConfiguration.getHostJsonPath())
                                          .filter(StringUtils::isNotEmpty).map(Paths::get)
                                          .orElseGet(() -> Paths.get(FunctionUtils.getDefaultHostJsonPath(functionDeployConfiguration.getModule())));
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

    public void deployFunctionApp(final FunctionAppBase<?, ?, ?> functionApp, final File stagingFolder, final IAzureMessager messenger) {
        final DeployFunctionAppTask deployFunctionAppTask = new DeployFunctionAppTask(functionApp, stagingFolder, null);
        deployFunctionAppTask.execute();
        listHttpTriggers(functionApp, messenger);
    }

    private static void listHttpTriggers(final FunctionAppBase<?, ?, ?> target, final IAzureMessager messenger) {
        AzureTaskManager.getInstance().runInBackground("list HTTPTrigger url", () -> {
            OperationContext.current().setMessager(AzureMessager.getDefaultMessager());
            try {
                if (target instanceof FunctionApp) {
                    ((FunctionApp) target).listHTTPTriggerUrls();
                }
            } catch (final Exception e) {
                messenger.warning("Failed to list http trigger urls.", null,
                                  AzureActionManager.getInstance().getAction(AppServiceActionsContributor.START_STREAM_LOG).bind(target));
            }
        });
    }

    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.FUNCTION, TelemetryConstants.DEPLOY_FUNCTION_APP);
    }

    @Override
    @AzureOperation(name = "boundary/function.complete_deployment.app", params = {"this.deployModel.getFunctionAppConfig().getName()"})
    protected void onSuccess(FunctionAppBase<?, ?, ?> result, @NotNull RunProcessHandler processHandler) {
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
        return OperationContext.action().getTelemetryProperties();
    }
}
