/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.containerregistry;

import com.microsoft.azure.toolkit.ide.common.IExplorerContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class ContainerRegistryExplorerContributor implements IExplorerContributor {
    private static final String NAME = "Container Registries";
    private static final String ICON = "/icons/ContainerRegistry/ContainerRegistry.svg";

    @Override
    public Node<?> getModuleNode() {
        final AzureContainerRegistry service = az(AzureContainerRegistry.class);
        final Function<AzureContainerRegistry, List<ContainerRegistry>> registries = asc -> asc.list().stream().flatMap(m -> m.registry().list().stream())
                .collect(Collectors.toList());
        return new Node<>(service).view(new AzureServiceLabelView<>(service, NAME, ICON))
                .actions(ContainerRegistryActionsContributor.SERVICE_ACTIONS)
                .addChildren(registries, (registry, serviceNode) -> new Node<>(registry)
                        .view(new AzureResourceLabelView<>(registry))
                        .actions(ContainerRegistryActionsContributor.REGISTRY_ACTIONS));
    }
}
