/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.containerregistry.ContainerService;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunProfileState;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateWebAppTask;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppOnLinuxDeployModel;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class WebAppOnLinuxDeployState extends AzureRunProfileState<AppServiceAppBase<?, ?, ?>> {
    public static final String WEBSITES_PORT = "WEBSITES_PORT";
    private final WebAppOnLinuxDeployModel deployModel;
    private final WebAppOnLinuxDeployConfiguration configuration;

    public WebAppOnLinuxDeployState(Project project, WebAppOnLinuxDeployConfiguration configuration) {
        super(project);
        this.configuration = configuration;
        this.deployModel = configuration.getModel();
    }

    // todo: @hanli Remove duplicates with push image run state
    @Override
    @AzureOperation(name = "platform/webapp.deploy_image.app", params = {"this.configuration.getWebAppConfig().getName()"})
    public AppServiceAppBase<?, ?, ?> executeSteps(@Nonnull RunProcessHandler processHandler, @Nonnull Operation operation) throws Exception {
        OperationContext.current().setMessager(getProcessHandlerMessenger());
        final DockerImage image = configuration.getDockerImageConfiguration();
        final ContainerRegistry registry = Objects.requireNonNull(Azure.az(AzureContainerRegistry.class).getById(configuration.getContainerRegistryId()),
                String.format("Registry (%s) was not found", configuration.getContainerRegistryId()));
        ContainerService.getInstance().pushDockerImage(configuration);
        // workaround: update image data, as we need to get password by api
        final AppServiceConfig webAppConfig = configuration.getAppServiceConfig();
        final RuntimeConfig runtime = webAppConfig.getRuntime();
        runtime.registryUrl(registry.getLoginServerUrl()).image(configuration.getFinalImageName()).username(registry.getUserName()).password(registry.getPrimaryCredential());
        // update port configuration
        final Map<String, String> appSettings = ObjectUtils.firstNonNull(webAppConfig.getAppSettings(), new HashMap<>());
        appSettings.put(WEBSITES_PORT, String.valueOf(configuration.getPort()));
        webAppConfig.setAppSettings(appSettings);
        // update image configuration
        final CreateOrUpdateWebAppTask task = new CreateOrUpdateWebAppTask(webAppConfig);
        final WebAppBase<?,?,?> webapp = task.execute();
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        tm.runOnPooledThread(() -> Optional.ofNullable(image)
                                           .map(DockerImage::getDockerFile)
                                           .map(f -> VfsUtil.findFileByIoFile(new File(f), true))
                                           .map(f -> AzureModule.from(f, this.project))
                                           .ifPresent(module -> tm.runLater(() -> tm.write(() -> {
                                               final Profile p = module.initializeWithDefaultProfileIfNot();
                                               Optional.of(registry).ifPresent(p::addApp);
                                               Optional.of(webapp).ifPresent(p::addApp);
                                               p.save();
                                           }))));
        return webapp;
    }

    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.WEBAPP, TelemetryConstants.DEPLOY_WEBAPP_CONTAINER);
    }

    @Override
    @AzureOperation(name = "boundary/webapp.complete_deployment.app", params = {"this.deployModel.getWebAppName()"})
    protected void onSuccess(@Nonnull AppServiceAppBase<?, ?, ?> result, @Nonnull RunProcessHandler processHandler) {
        final String image = Optional.ofNullable(configuration.getDockerImageConfiguration()).map(DockerImage::getImageName).orElse(null);
        processHandler.setText(String.format("Image (%s) has been deployed to Web App (%s).", image, result.getName()));
        processHandler.setText(String.format("URL:  https://%s.azurewebsites.net/", result.getName()));
        processHandler.notifyComplete();
        updateConfigurationDataModel(result);
    }

    protected Map<String, String> getTelemetryMap() {
        final Map<String, String> telemetryMap = new HashMap<>();
        final AppServiceConfig appServiceConfig = configuration.getAppServiceConfig();
        telemetryMap.put("SubscriptionId", StringUtils.firstNonBlank(appServiceConfig.subscriptionId(), deployModel.getSubscriptionId()));
        telemetryMap.put("CreateNewApp", String.valueOf(deployModel.isCreatingNewWebAppOnLinux()));
        telemetryMap.put("CreateNewSP", String.valueOf(deployModel.isCreatingNewAppServicePlan()));
        telemetryMap.put("CreateNewRGP", String.valueOf(deployModel.isCreatingNewResourceGroup()));
        telemetryMap.putAll(OperationContext.current().getTelemetryProperties());
        return telemetryMap;
    }

    private void updateConfigurationDataModel(AppServiceAppBase<?, ?, ?> app) {
        deployModel.setCreatingNewWebAppOnLinux(false);
        deployModel.setWebAppId(app.getId());
        deployModel.setResourceGroupName(app.getResourceGroupName());
    }
}
