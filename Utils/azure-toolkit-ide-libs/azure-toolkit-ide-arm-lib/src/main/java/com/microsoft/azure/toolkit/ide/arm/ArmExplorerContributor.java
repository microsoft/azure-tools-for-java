/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.arm;

import com.microsoft.azure.toolkit.ide.common.IExplorerContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ArmExplorerContributor implements IExplorerContributor {
    private static final String NAME = "Resource Management";
    private static final String ICON = "/icons/Microsoft.Resources/default.svg";

    @Override
    public Node<?> getModuleNode() {
        final AzureResources service = Azure.az(AzureResources.class);
        final Function<AzureResources, List<ResourceGroup>> groups = s -> s.list().stream()
            .flatMap(m -> m.resourceGroups().list().stream()).collect(Collectors.toList());
        return new Node<>(service).view(new AzureServiceLabelView<>(service, NAME, ICON))
            .actions(ArmActionsContributor.RESOURCE_MANAGEMENT_ACTIONS)
            .addChildren(groups, (group, mNode) -> new Node<>(group)
                .view(new AzureResourceLabelView<>(group))
                .actions(ArmActionsContributor.RESOURCE_GROUP_ACTIONS)
                .addChildren(g -> g.deployments().list(), (deployment, gNode) -> new Node<>(deployment)
                    .view(new AzureResourceLabelView<>(deployment))
                    .actions(ArmActionsContributor.RESOURCE_DEPLOYMENT_ACTIONS)));
    }
}
