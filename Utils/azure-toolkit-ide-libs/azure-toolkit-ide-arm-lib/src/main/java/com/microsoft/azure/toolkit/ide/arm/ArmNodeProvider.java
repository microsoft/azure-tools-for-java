/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.arm;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzureResourceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceDeployment;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ArmNodeProvider implements IExplorerNodeProvider {
    private static final String NAME = "Resource Management";
    private static final String ICON = "/icons/Microsoft.Resources/default.svg";

    @Nullable
    @Override
    public Object getRoot() {
        return Azure.az(AzureResources.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent) {
        return data instanceof AzureResources ||
            data instanceof ResourceGroup ||
            data instanceof ResourceDeployment;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data, @Nullable Node<?> parent, @Nonnull Manager manager) {
        if (data instanceof AzureResources) {
            final AzureResources service = (AzureResources) data;
            final Function<AzureResources, List<ResourceGroup>> groupsLoader = s -> s.list().stream()
                .flatMap(m -> m.resourceGroups().list().stream()).collect(Collectors.toList());
            return new Node<>(service)
                .view(new AzureServiceLabelView<>(service, NAME, ICON))
                .actions(ArmActionsContributor.RESOURCE_MANAGEMENT_ACTIONS)
                .addChildren(groupsLoader, (d, p) -> this.createNode(d, p, manager));
        } else if (data instanceof ResourceGroup) {
            final ResourceGroup rg = (ResourceGroup) data;
            return new Node<>(rg)
                .view(new AzureResourceLabelView<>(rg))
                .inlineAction(ResourceCommonActionsContributor.PIN)
                .actions(ArmActionsContributor.RESOURCE_GROUP_ACTIONS)
                .addChildren(g -> g.deployments().list(), (d, p) -> this.createNode(d, p, manager));
        } else if (data instanceof ResourceDeployment) {
            final ResourceDeployment deployment = (ResourceDeployment) data;
            return new Node<>(deployment)
                .view(new AzureResourceLabelView<>(deployment))
                .inlineAction(ResourceCommonActionsContributor.PIN)
                .actions(ArmActionsContributor.RESOURCE_DEPLOYMENT_ACTIONS);
        }
        return null;
    }
}
