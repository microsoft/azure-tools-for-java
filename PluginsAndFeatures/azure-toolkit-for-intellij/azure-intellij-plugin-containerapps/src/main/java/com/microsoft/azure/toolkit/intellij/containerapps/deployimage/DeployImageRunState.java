/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.deployimage;

import com.azure.resourcemanager.appcontainers.models.EnvironmentVar;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.core.command.PushImageResultCallback;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler;
import com.microsoft.azure.toolkit.intellij.container.DockerUtil;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunProfileState;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DeployImageRunState extends AzureRunProfileState<ContainerApp> {
    private final DeployImageModel dataModel;
    private final DeployImageRunConfiguration configuration;

    public DeployImageRunState(Project project, DeployImageRunConfiguration configuration) {
        super(project);
        this.configuration = configuration;
        this.dataModel = configuration.getDataModel();
    }

    // todo: remove duplicate codes with PushImageRunState
    @Override
    public ContainerApp executeSteps(@Nonnull RunProcessHandler processHandler, @Nonnull Operation operation) throws Exception {
        OperationContext.current().setMessager(getProcessHandlerMessenger());
        final DockerImage image = configuration.getDockerImageConfiguration();
        final DockerClient dockerClient = DockerUtil.getDockerClient(Objects.requireNonNull(configuration.getDockerHostConfiguration()));
        final ContainerRegistry registry = Azure.az(AzureContainerRegistry.class).getById(dataModel.getContainerRegistryId());
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
        // update Image
        final String containerAppId = dataModel.getContainerAppId();
        final ContainerApp containerApp = Objects.requireNonNull(Azure.az(AzureContainerApps.class).getById(containerAppId), String.format("Container app %s was not found", dataModel.getContainerAppId()));
        final ContainerAppDraft draft = (ContainerAppDraft) containerApp.update();
        final ContainerAppDraft.Config config = new ContainerAppDraft.Config();
        final List<EnvironmentVar> vars = dataModel.getEnvironmentVariables().entrySet().stream()
                .map(e -> new EnvironmentVar().withName(e.getKey()).withValue(e.getValue()))
                .collect(Collectors.toList());
        config.setImageConfig(ContainerAppDraft.ImageConfig.builder().containerRegistry(registry).fullImageName(imageAndTag).environmentVariables(vars).build());
        config.setIngressConfig(dataModel.getIngressConfig());
        draft.setConfig(config);
        draft.updateIfExist();
        return containerApp;
    }

    @Nonnull
    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.ACR, TelemetryConstants.ACR_PUSHIMAGE);
    }

    @Override
    protected void onSuccess(@Nonnull final ContainerApp image, @Nonnull RunProcessHandler processHandler) {
        processHandler.setText(image.isIngressEnabled() ? "Deployment succeed" : String.format("Deployment succeed, you may access your app with https://%s", image.getIngressFqdn()));
        processHandler.notifyComplete();
    }

    @Override
    protected Map<String, String> getTelemetryMap() {
        return Collections.emptyMap();
    }
}
