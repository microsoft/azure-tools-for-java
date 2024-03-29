package com.microsoft.azure.toolkit.ide.synapse.spark;

import com.google.common.collect.ImmutableSortedSet;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkComputeManager;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.synapse.ArcadiaSparkComputeNode;
import com.microsoft.azure.toolkit.lib.synapse.AzureSynapseService;
import com.microsoft.azure.toolkit.lib.synapse.WorkspaceNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class SynapseNodeProvider implements IExplorerNodeProvider {

    private static final String NAME = "Synapse";
    private static final String ICON = AzureIcons.ApacheSparkOnAzureSynapse.MODULE.getIconPath();

    @Nullable
    @Override
    public Object getRoot() {
        return az(AzureSynapseService.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureSynapseService
                || data instanceof WorkspaceNode
                || data instanceof ArcadiaSparkComputeNode;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data,@Nullable Node<?> parent,@Nonnull Manager manager) {
        if (data instanceof AzureSynapseService) {
            final Function<AzureSynapseService, List<WorkspaceNode>> workspaces = s -> s.list().stream()
                    .flatMap(m -> m.workspaces().list().stream()).collect(Collectors.toList());
            return new AzServiceNode<>((AzureSynapseService) data)
                    .withIcon(ICON)
                    .withLabel("Apache Spark on Azure Synapse")
                    .withActions(SynapseActionsContributor.SERVICE_ACTIONS)
                    .addChildren(workspaces, (workspace, serviceNode) -> this.createNode(workspace, serviceNode, manager));
        } else if (data instanceof WorkspaceNode) {
            final WorkspaceNode workspaceNode = (WorkspaceNode) data;

            return new AzResourceNode<>(workspaceNode)
                    .withIcon(AzureIcon.builder().iconPath("/icons/Workspace_13x.png").build())
                    .withActions(SynapseActionsContributor.WORKSPACES_NODE_ACTIONS)
                    .withChildrenLoadLazily(false)
                    .addChildren(s->s.getArcadiaSparkComputeModule().list(), (d, mn) -> this.createNode(d, mn, manager));
        } else if (data instanceof ArcadiaSparkComputeNode) {
            final ArcadiaSparkComputeNode arcadiaSparkComputeNode = (ArcadiaSparkComputeNode) data;

            return new AzResourceNode<>(arcadiaSparkComputeNode)
                    .withIcon(AzureIcon.builder().iconPath("/icons/Cluster.png").build());
        } else {
            return null;
        }
    }

}
