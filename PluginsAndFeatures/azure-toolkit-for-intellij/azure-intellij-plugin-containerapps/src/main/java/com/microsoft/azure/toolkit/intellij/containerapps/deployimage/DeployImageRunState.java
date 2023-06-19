/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.deployimage;

import com.azure.resourcemanager.appcontainers.models.EnvironmentVar;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.microsoft.azure.toolkit.intellij.common.RunProcessHandler;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.intellij.containerregistry.ContainerService;
import com.microsoft.azure.toolkit.intellij.facet.AzureProjectFacet;
import com.microsoft.azure.toolkit.intellij.legacy.common.AzureRunProfileState;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class DeployImageRunState extends AzureRunProfileState<ContainerApp> {
    private final DeployImageModel dataModel;
    private final DeployImageRunConfiguration configuration;

    public DeployImageRunState(Project project, DeployImageRunConfiguration configuration) {
        super(project);
        this.configuration = configuration;
        this.dataModel = configuration.getDataModel();
    }

    @Override
    @AzureOperation(name = "platform/containerapps.deploy_image.app", params = {"nameFromResourceId(this.dataModel.getContainerAppId())"})
    public ContainerApp executeSteps(@Nonnull RunProcessHandler processHandler, @Nonnull Operation operation) throws Exception {
        OperationContext.current().setMessager(getProcessHandlerMessenger());
        final DockerImage image = configuration.getDockerImageConfiguration();
        final ContainerRegistry registry = Azure.az(AzureContainerRegistry.class).getById(dataModel.getContainerRegistryId());
        ContainerService.getInstance().pushDockerImage(configuration);
        // update Image
        final String containerAppId = dataModel.getContainerAppId();
        final ContainerApp containerApp = Objects.requireNonNull(Azure.az(AzureContainerApps.class).getById(containerAppId), String.format("Container app %s was not found", dataModel.getContainerAppId()));
        final ContainerAppDraft draft = (ContainerAppDraft) containerApp.update();
        final ContainerAppDraft.Config config = new ContainerAppDraft.Config();
        final List<EnvironmentVar> vars = dataModel.getEnvironmentVariables().entrySet().stream()
            .map(e -> new EnvironmentVar().withName(e.getKey()).withValue(e.getValue()))
            .collect(Collectors.toList());
        final ContainerAppDraft.ImageConfig imageConfig = new ContainerAppDraft.ImageConfig(configuration.getFinalImageName());
        imageConfig.setContainerRegistry(registry);
        imageConfig.setEnvironmentVariables(vars);
        config.setImageConfig(imageConfig);
        config.setIngressConfig(dataModel.getIngressConfig());
        draft.setConfig(config);
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        Optional.ofNullable(image)
            .map(DockerImage::getDockerFile)
            .map(f -> VfsUtil.findFileByIoFile(f, true))
            .flatMap(f -> AzureProjectFacet.getAzureModule(f, this.project))
            .ifPresent(module -> tm.runLater(() -> tm.write(() -> {
                final Profile p = module.initializeWithDefaultProfileIfNot();
                Optional.ofNullable(registry).ifPresent(p::addApp);
                Optional.of(containerApp).ifPresent(p::addApp);
                p.save();
            })));
        draft.updateIfExist();
        return containerApp;
    }

    @Nonnull
    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.ACR, TelemetryConstants.ACR_PUSHIMAGE);
    }

    @Override
    protected void onSuccess(@Nonnull final ContainerApp app, @Nonnull RunProcessHandler processHandler) {
        final String image = Optional.ofNullable(configuration.getDockerImageConfiguration()).map(DockerImage::getImageName).orElse(null);
        processHandler.setText(String.format("Image (%s) has been deployed to Container App (%s).", image, app.getName()));
        if (app.isIngressEnabled()) {
            processHandler.setText(String.format("URL: https://%s", app.getIngressFqdn()));
        }
        processHandler.notifyComplete();
    }

    @Override
    protected Map<String, String> getTelemetryMap() {
        return Collections.emptyMap();
    }
}
