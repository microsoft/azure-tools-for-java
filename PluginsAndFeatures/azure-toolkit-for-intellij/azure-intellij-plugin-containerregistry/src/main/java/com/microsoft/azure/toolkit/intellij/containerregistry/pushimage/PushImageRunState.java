/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.pushimage;

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
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azuretools.core.mvp.model.container.pojo.PushImageRunModel;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.util.MavenRunTaskUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PushImageRunState extends AzureRunProfileState<String> {
    private final PushImageRunModel dataModel;
    private final PushImageRunConfiguration configuration;

    public PushImageRunState(Project project, PushImageRunConfiguration configuration) {
        super(project);
        this.configuration = configuration;
        this.dataModel = configuration.getModel();
    }

    @Override
    public String executeSteps(@Nonnull RunProcessHandler processHandler, @Nonnull Operation operation) throws Exception {
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
        return image.getImageName();
    }

    @Nonnull
    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.ACR, TelemetryConstants.ACR_PUSHIMAGE);
    }

    @Override
    protected void onSuccess(String image, @Nonnull RunProcessHandler processHandler) {
        processHandler.setText("pushed.");
        processHandler.notifyComplete();
    }

    @Override
    protected Map<String, String> getTelemetryMap() {
        final String fileType = dataModel.getTargetName() == null ? StringUtils.EMPTY : MavenRunTaskUtil.getFileType(dataModel.getTargetName());
        return Collections.singletonMap(TelemetryConstants.FILETYPE, fileType);
    }
}
