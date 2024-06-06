/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.containerapps.deployimage;

import com.azure.resourcemanager.appcontainers.models.EnvironmentVar;
import com.microsoft.azure.toolkit.intellij.container.model.DockerPushConfiguration;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerapps.model.IngressConfig;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import lombok.*;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DeployImageModel extends DockerPushConfiguration {
    private String moduleName;
    @Builder.Default
    private DeploymentType deploymentType = DeploymentType.Image;
    private Map<String, String> environmentVariables;
    private IngressConfig ingressConfig;
    private String containerAppId;
    // properties for remote build
    private String path;
    private String fullImageName;
    private Map<String, String> sourceBuildEnv;

    public boolean isRemoteBuild() {
        return deploymentType == DeploymentType.Code || deploymentType == DeploymentType.Artifact;
    }

    public ContainerAppDraft.Config getContainerAppConfig() {
        final ContainerAppDraft.Config config = new ContainerAppDraft.Config();
        config.setImageConfig(getImageConfig());
        config.setIngressConfig(getIngressConfig());
        return config;
    }

    public ContainerAppDraft.ImageConfig getImageConfig() {
        final String imageName = isRemoteBuild() ? fullImageName : getFinalRepositoryName() + ":" + getFinalTagName();
        final ContainerAppDraft.ImageConfig imageConfig = new ContainerAppDraft.ImageConfig(imageName);
        if (Azure.az(AzureAccount.class).isLoggedIn() && StringUtils.isNoneBlank(getContainerRegistryId())) {
            final ContainerRegistry registry = Azure.az(AzureContainerRegistry.class).getById(getContainerRegistryId());
            imageConfig.setContainerRegistry(registry);
        }
        final List<EnvironmentVar> vars = Objects.isNull(getEnvironmentVariables()) ? Collections.emptyList() :
                getEnvironmentVariables().entrySet().stream().map(e ->
                        new EnvironmentVar().withName(e.getKey()).withValue(e.getValue())).collect(Collectors.toList());
        imageConfig.setEnvironmentVariables(vars);
        final ContainerAppDraft.BuildImageConfig buildImageConfig = new ContainerAppDraft.BuildImageConfig();
        Optional.ofNullable(path).filter(StringUtils::isNotBlank).map(Path::of).ifPresent(buildImageConfig::setSource);
        buildImageConfig.setSourceBuildEnv(sourceBuildEnv);
        imageConfig.setBuildImageConfig(buildImageConfig);
        return imageConfig;
    }

    public void setImageConfig(ContainerAppDraft.ImageConfig imageConfig) {
        this.fullImageName = imageConfig.getFullImageName();
        Optional.ofNullable(imageConfig.getContainerRegistry()).map(ContainerRegistry::getId)
                .ifPresent(this::setContainerRegistryId);
        Optional.ofNullable(imageConfig.getBuildImageConfig()).ifPresent(buildImageConfig -> {
            this.setSourceBuildEnv(buildImageConfig.getSourceBuildEnv());
            Optional.ofNullable(buildImageConfig.getSource()).map(Path::toString).ifPresent(this::setPath);
        });
    }
}
