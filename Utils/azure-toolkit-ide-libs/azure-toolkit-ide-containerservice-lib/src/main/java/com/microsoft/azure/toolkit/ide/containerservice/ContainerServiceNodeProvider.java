/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.containerservice;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.ActionNode;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.containerservice.AzureContainerService;
import com.microsoft.azure.toolkit.lib.containerservice.KubernetesCluster;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ContainerServiceNodeProvider implements IExplorerNodeProvider {
    private static final String NAME = "Kubernetes services";
    private static final String ICON = AzureIcons.Kubernetes.MODULE.getIconPath();

    @Nullable
    @Override
    public Object getRoot() {
        return Azure.az(AzureContainerService.class);
    }

    @Override
    public boolean accept(@NotNull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureContainerService || data instanceof KubernetesCluster;
    }

    @Nullable
    @Override
    public Node<?> createNode(@NotNull Object data, @Nullable Node<?> parent, @NotNull IExplorerNodeProvider.Manager manager) {
        if (data instanceof AzureContainerService) {
            final Function<AzureContainerService, List<KubernetesCluster>> clusters = acs -> acs.list().stream().flatMap(m -> m.kubernetes().list().stream())
                .collect(Collectors.toList());
            return new AzServiceNode<>((AzureContainerService) data)
                .withIcon(ICON)
                .withLabel(NAME)
                .withActions(ContainerServiceActionsContributor.SERVICE_ACTIONS)
                .addChildren(clusters, (cluster, serviceNode) -> this.createNode(cluster, serviceNode, manager));
        } else if (data instanceof KubernetesCluster) {
            final ActionNode<KubernetesCluster> actionNode = new ActionNode<>(ContainerServiceActionsContributor.OPEN_KUBERNETES_PLUGIN, (KubernetesCluster) data);
            final Node<KubernetesCluster> clusterNode = new AzResourceNode<>((KubernetesCluster) data)
                    .addInlineAction(ResourceCommonActionsContributor.PIN)
                    .addInlineAction(ContainerServiceActionsContributor.OPEN_KUBERNETES_PLUGIN)
                    .onDoubleClicked(ResourceCommonActionsContributor.SHOW_PROPERTIES)
                    .withActions(ContainerServiceActionsContributor.CLUSTER_ACTIONS);
            return actionNode.isEnabled() ? clusterNode.addChild(actionNode) : clusterNode;
//                    .addChildren(cluster -> cluster.agentPools().list(), (agentPool, clusterNode) -> this.createNode(agentPool, clusterNode, manager));
        }
//        else if (data instanceof KubernetesClusterAgentPool) {
//            final KubernetesClusterAgentPool server = (KubernetesClusterAgentPool) data;
//            return new Node<>(server)
//                    .view(new AzureResourceLabelView<>(server))
//                    .addInlineAction(ResourceCommonActionsContributor.PIN)
//                    .doubleClickAction(ResourceCommonActionsContributor.SHOW_PROPERTIES)
//                    .withActions(ContainerServiceActionsContributor.AGENT_POOL_ACTIONS);
//        }
        return null;
    }
}
