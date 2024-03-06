/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerregistry.servicesview;

import com.intellij.docker.agent.registry.model.DockerRegistry;
import com.intellij.docker.agent.registry.model.DockerV2RegistryKt;
import com.intellij.docker.registry.DockerRegistryConfiguration;
import com.intellij.docker.view.registry.DockerRegistryProvider;
import com.intellij.docker.view.registry.node.DockerRegistryRoot;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Getter
@SuppressWarnings("UnstableApiUsage")
public class AzureContainerRegistryProvider extends DockerRegistryProvider {
    public static final String ID = "AzureContainerRegistryProvider";
    private final String registryName = "Azure Container Registry";
    private final String id = ID;

    @Nullable
    @Override
    public Configurator createConfigurator() {
        return new AzureContainerRegistryConfigurator();
    }

    @Nullable
    @Override
    protected DockerRegistryRoot doGetRegistryRoot(@Nonnull final DockerRegistryConfiguration configuration) {
        return new DockerRegistryRoot(configuration, executor -> {
            final String address = configuration.getAddress();
            final DockerRegistry.Userdata userdata = new DockerRegistry.Userdata(configuration.getUsername(), configuration.getPasswordSafe());
            return DockerV2RegistryKt.dockerV2Registry(address, executor, userdata, 0);
        });
    }

    @Nullable
    @Override
    @AzureOperation("user/acr.connect_acr_instance_in_services_view")
    public Object checkCredentials(@Nonnull final DockerRegistryConfiguration registry, @Nonnull final Continuation<? super Unit> $completion) {
        return super.checkCredentials(registry, $completion);
    }
}
