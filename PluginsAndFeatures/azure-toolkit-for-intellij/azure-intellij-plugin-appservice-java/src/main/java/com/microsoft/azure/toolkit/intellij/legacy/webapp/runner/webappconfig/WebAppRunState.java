/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webappconfig;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.microsoft.azure.toolkit.ide.appservice.AppServiceActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactManager;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.DotEnvBeforeRunTaskProvider;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunProfileState;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.model.DeployType;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppArtifact;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateWebAppTask;
import com.microsoft.azure.toolkit.lib.appservice.task.DeployWebAppTask;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.azuretools.utils.WebAppUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenConstants;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class WebAppRunState extends AzureRunProfileState<WebAppBase<?, ?, ?>> {
    private static final String LIBS_ROOT = "/home/site/wwwroot/libs/";
    private static final String JAVA_OPTS = "JAVA_OPTS";
    private static final String CATALINA_OPTS = "CATALINA_OPTS";
    private static final int DEFAULT_DEPLOYMENT_STATUS_REFRESH_INTERVAL = 10;
    private static final int DEFAULT_DEPLOYMENT_STATUS_MAX_REFRESH_TIMES = 6;
    private static final String GET_DEPLOYMENT_STATUS_TIMEOUT = "The app is still starting, " +
        "you could start streaming log to check if something wrong in server side.";

    private final WebAppConfiguration webAppConfiguration;
    private final IntelliJWebAppSettingModel webAppSettingModel;

    private final Map<String, String> appSettingsForResourceConnection = new HashMap<>();
    private static final String DEPLOYMENT_SUCCEED = "Deployment was successful but the app may still be starting.";

    /**
     * Place to execute the Web App deployment task.
     */
    public WebAppRunState(Project project, WebAppConfiguration webAppConfiguration) {
        super(project);
        this.webAppConfiguration = webAppConfiguration;
        this.webAppSettingModel = webAppConfiguration.getModel();
    }

    @Nullable
    @Override
    @AzureOperation(name = "user/webapp.deploy_artifact.app", params = {"this.webAppConfiguration.getAppServiceConfig().getAppName()"})
    public WebAppBase<?, ?, ?> executeSteps(@NotNull RunProcessHandler processHandler, @NotNull Operation operation) throws Exception {
        OperationContext.current().setMessager(getProcessHandlerMessenger());
        final File artifact = new File(getTargetPath());
        if (!artifact.exists()) {
            throw new FileNotFoundException(message("webapp.deploy.error.noTargetFile", artifact.getAbsolutePath()));
        }
        final WebAppBase<?, ?, ?> deployTarget = getOrCreateDeployTarget();
        // todo: remove workaround after fix reset issue in toolkit lib
        if (deployTarget instanceof AzResource.Draft<?, ?> draft) {
            draft.reset();
        }
        webAppConfiguration.setWebApp(deployTarget);
        deployArtifactsToWebApp(deployTarget, artifact, webAppSettingModel.isDeployToRoot());
        return deployTarget;
    }

    @NotNull
    private WebAppBase<?, ?, ?> getOrCreateDeployTarget() {
        final AppServiceConfig appServiceConfig = webAppConfiguration.getAppServiceConfig();
        applyResourceConnections(webAppConfiguration, appServiceConfig);
        cleanupRemovedAppSettings(appServiceConfig);
        final WebAppBase<?, ?, ?> result = new CreateOrUpdateWebAppTask(appServiceConfig).execute();
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        final AzureModule module = Optional.ofNullable(this.webAppConfiguration.getModule()).map(AzureModule::from)
                                           .or(() -> Optional.ofNullable(getTargetPath())
                                                             .map(f -> VfsUtil.findFile(Path.of(f), true))
                                                             .map(f -> AzureModule.from(f, this.project))).orElse(null);
        if (Objects.nonNull(module)) {
            final AbstractAzResource<?, ?, ?> target = result instanceof WebAppDeploymentSlot ? result.getParent() : result;
            tm.runOnPooledThread(() -> tm.runLater(() -> tm.write(() -> module.initializeWithDefaultProfileIfNot().addApp(target).save())));
        }
        return result;
    }

    private void applyResourceConnections(@Nonnull final WebAppConfiguration webAppConfiguration, @Nonnull final AppServiceConfig appServiceConfig) {
        final Map<String, String> appSettings = appServiceConfig.getAppSettings();
        if (webAppConfiguration.isConnectionEnabled()) {
            final DotEnvBeforeRunTaskProvider.LoadDotEnvBeforeRunTask loadDotEnvBeforeRunTask = webAppConfiguration.getLoadDotEnvBeforeRunTask();
            loadDotEnvBeforeRunTask.loadEnv().forEach(env -> appSettings.put(env.getKey(), env.getValue()));
        }
    }

    private void cleanupRemovedAppSettings(@Nonnull final AppServiceConfig config) {
        final Map<String, String> appSettings = Optional.ofNullable(config.getAppSettings()).orElse(Collections.emptyMap());
        final FunctionApp app = Azure.az(AzureFunctions.class).functionApps(config.getSubscriptionId())
                .get(config.getAppName(), config.getResourceGroup());
        final FunctionAppBase<?, ?, ?> target = Objects.isNull(app) || Objects.isNull(config.getSlotConfig()) ?
                app : app.slots().get(config.getSlotConfig().getName(), app.getResourceGroupName());
        final Map<String, String> existingAppSettings = Optional.ofNullable(target).map(FunctionAppBase::getAppSettings).orElse(Collections.emptyMap());
        final Set<String> appSettingsToRemove = MapUtils.isEmpty(existingAppSettings) ? Collections.emptySet() :
                existingAppSettings.keySet().stream().filter(key -> !appSettings.containsKey(key)).collect(Collectors.toSet());
        config.appSettingsToRemove(appSettingsToRemove);
    }

    private void deployArtifactsToWebApp(final WebAppBase<?,?,?> deployTarget, final File file, final boolean isDeployToRoot) {
        final Action<Void> retry = Action.retryFromFailure(() -> deployArtifactsToWebApp(deployTarget, file, isDeployToRoot));
        final DeployType deployType = Optional.ofNullable(DeployType.fromString(FilenameUtils.getExtension(file.getName()))).orElse(DeployType.ZIP);
        final String path = isDeployToRoot || Objects.requireNonNull(deployTarget.getRuntime()).isJavaSE() ?
                            null : String.format("webapps/%s", FilenameUtils.getBaseName(file.getName()).replaceAll("#", StringUtils.EMPTY));
        final WebAppArtifact build = WebAppArtifact.builder().deployType(deployType).path(path).file(file).build();
        final DeployWebAppTask deployWebAppTask = new DeployWebAppTask(deployTarget, Collections.singletonList(build), true, false, false);
        deployWebAppTask.doExecute();
        AzureTaskManager.getInstance().runInBackground("get deployment status", () -> {
            OperationContext.current().setMessager(AzureMessager.getDefaultMessager());
            if (!deployWebAppTask.waitUntilDeploymentReady(false, DEFAULT_DEPLOYMENT_STATUS_REFRESH_INTERVAL, DEFAULT_DEPLOYMENT_STATUS_MAX_REFRESH_TIMES)) {
                AzureMessager.getMessager().warning(GET_DEPLOYMENT_STATUS_TIMEOUT, null,
                                                    AzureActionManager.getInstance().getAction(AppServiceActionsContributor.START_STREAM_LOG).bind(deployTarget));
            } else {
                AzureMessager.getMessager().success(AzureString.format("App({0}) started successfully.", deployTarget.getName()), null,
                                                    AzureActionManager.getInstance().getAction(AppServiceActionsContributor.OPEN_IN_BROWSER).bind(deployTarget));
            }
        });
    }

    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.WEBAPP, TelemetryConstants.DEPLOY_WEBAPP);
    }

    @Override
    protected void onSuccess(WebAppBase<?, ?, ?> result, @NotNull RunProcessHandler processHandler) {
        final String targetPath = getTargetPath();
        final String fileName = FileNameUtils.getBaseName(targetPath);
        final String fileType = FileNameUtils.getExtension(targetPath);
        final String url = getUrl(result, fileName, fileType);
        processHandler.setText(DEPLOYMENT_SUCCEED);
        processHandler.setText("URL: " + url);
        if (webAppSettingModel.isOpenBrowserAfterDeployment()) {
            AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle(url);
        }
        processHandler.notifyComplete();
    }

    @Override
    protected Map<String, String> getTelemetryMap() {
        final Map<String, String> properties = new HashMap<>(OperationContext.action().getTelemetryProperties());
        properties.put("artifactType", webAppConfiguration.getAzureArtifactType() == null ? null : webAppConfiguration.getAzureArtifactType().name());
        return properties;
    }

    @AzureOperation(name = "internal/webapp.get_artifact.app", params = {"this.webAppConfiguration.getName()"})
    private String getTargetPath() {
        final AzureArtifact azureArtifact =
                AzureArtifactManager.getInstance(project).getAzureArtifactById(webAppConfiguration.getAzureArtifactType(),
                                                                               webAppConfiguration.getArtifactIdentifier());
        if (Objects.isNull(azureArtifact)) {
            final String error = String.format("selected artifact[%s] not found", webAppConfiguration.getArtifactIdentifier());
            throw new AzureToolkitRuntimeException(error);
        }
        return azureArtifact.getFileForDeployment();
    }

    @NotNull
    private String getUrl(@NotNull WebAppBase<?, ?, ?> webApp, @NotNull String fileName, @NotNull String fileType) {
        String url = "https://" + webApp.getHostName();
        if (Objects.equals(fileType, MavenConstants.TYPE_WAR) && !webAppSettingModel.isDeployToRoot()) {
            url += "/" + WebAppUtils.encodeURL(fileName.replaceAll("#", StringUtils.EMPTY)).replaceAll("\\+", "%20");
        }
        return url;
    }
}
