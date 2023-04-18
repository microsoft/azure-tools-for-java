/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.core.command.PushImageResultCallback;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.appservice.webapp.model.WebAppConfig;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler;
import com.microsoft.azure.toolkit.intellij.container.DockerUtil;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunProfileState;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.task.CreateOrUpdateWebAppTask;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.legacy.webapp.WebAppService;
import com.microsoft.azuretools.core.mvp.model.webapp.WebAppOnLinuxDeployModel;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WebAppOnLinuxDeployState extends AzureRunProfileState<AppServiceAppBase<?, ?, ?>> {
    private final WebAppOnLinuxDeployModel deployModel;
    private final WebAppOnLinuxDeployConfiguration configuration;

    public WebAppOnLinuxDeployState(Project project, WebAppOnLinuxDeployConfiguration configuration) {
        super(project);
        this.configuration = configuration;
        this.deployModel = configuration.getModel();
    }

    // todo: @hanli Remove duplicates with push image run state
    @Override
    @AzureOperation(name = "platform/docker.deploy_image")
    public AppServiceAppBase<?, ?, ?> executeSteps(@Nonnull RunProcessHandler processHandler, @Nonnull Operation operation) throws Exception {
        OperationContext.current().setMessager(getProcessHandlerMessenger());
        final DockerImage image = configuration.getDockerImageConfiguration();
        final DockerClient dockerClient = DockerUtil.getDockerClient(Objects.requireNonNull(configuration.getDockerHostConfiguration()));
        final ContainerRegistry registry = Azure.az(AzureContainerRegistry.class).getById(configuration.getContainerRegistryId());
        final String loginServerUrl = Objects.requireNonNull(registry).getLoginServerUrl();
        final String imageAndTag = StringUtils.startsWith(Objects.requireNonNull(image).getImageName(), loginServerUrl) ? image.getImageName() : loginServerUrl + "/" + image.getImageName();
        // tag image with ACR url
        final DockerImage localImage = DockerUtil.getImageWithName(dockerClient, image.getImageName());
        final DockerImage taggedImage = DockerUtil.getImageWithName(dockerClient, imageAndTag);
        if (ObjectUtils.allNull(localImage, taggedImage)) {
            throw new AzureToolkitRuntimeException(String.format("Image %s was not found locally.", image.getImageName()));
        } else if (Objects.isNull(taggedImage)) {
            // tag image
            DockerUtil.tagImage(dockerClient, image.getImageName(), imageAndTag, image.getTagName());
        }
        // push to ACR
        processHandler.setText(String.format("Pushing to ACR ... [%s] ", loginServerUrl));
        final PushImageResultCallback callBack = new PushImageResultCallback() {
            @Override
            public void onNext(PushResponseItem item) {
                final String status = item.getStatus();
                final String id = item.getId();
                final String progress = item.getProgress();
                final String message = Stream.of(status, id, progress).filter(StringUtils::isNoneBlank).collect(Collectors.joining(" "));
                processHandler.println(message, ProcessOutputTypes.SYSTEM);
                super.onNext(item);
            }
        };
        DockerUtil.pushImage(dockerClient, Objects.requireNonNull(loginServerUrl), registry.getUserName(), registry.getPrimaryCredential(), imageAndTag, callBack);
        // deploy
        final WebAppConfig webAppConfig = configuration.getWebAppConfig();
        final AppServiceConfig appServiceConfig = WebAppService.convertToTaskConfig(webAppConfig);
        // update image configuration
        final RuntimeConfig runtime = appServiceConfig.runtime();
        final DockerImage dockerImageConfiguration = configuration.getDockerImageConfiguration();
        runtime.registryUrl(loginServerUrl).image(imageAndTag).username(registry.getUserName()).password(registry.getPrimaryCredential());
        final CreateOrUpdateWebAppTask task = new CreateOrUpdateWebAppTask(appServiceConfig);
        final WebAppBase<?, ?, ?> result = task.execute();
        if (result != null) {
            processHandler.setText(String.format("WebApp [%s] is created/updated successfully.", result.getName()));
            processHandler.setText(String.format("URL:  https://%s.azurewebsites.net/", result.getName()));
            updateConfigurationDataModel(result);
        }
        return result;
    }

    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.WEBAPP, TelemetryConstants.DEPLOY_WEBAPP_CONTAINER);
    }

    @Override
    @AzureOperation(name = "boundary/webapp.complete_deployment.app", params = {"this.deployModel.getWebAppName()"})
    protected void onSuccess(AppServiceAppBase<?, ?, ?> result, @Nonnull RunProcessHandler processHandler) {
        processHandler.notifyComplete();
    }

    protected Map<String, String> getTelemetryMap() {
        final Map<String, String> telemetryMap = new HashMap<>();
        telemetryMap.put("SubscriptionId", deployModel.getSubscriptionId());
        telemetryMap.put("CreateNewApp", String.valueOf(deployModel.isCreatingNewWebAppOnLinux()));
        telemetryMap.put("CreateNewSP", String.valueOf(deployModel.isCreatingNewAppServicePlan()));
        telemetryMap.put("CreateNewRGP", String.valueOf(deployModel.isCreatingNewResourceGroup()));
        return telemetryMap;
    }

    private void updateConfigurationDataModel(WebAppBase<?, ?, ?> app) {
        deployModel.setCreatingNewWebAppOnLinux(false);
        deployModel.setWebAppId(app.getId());
        deployModel.setResourceGroupName(app.getResourceGroupName());
    }
}
