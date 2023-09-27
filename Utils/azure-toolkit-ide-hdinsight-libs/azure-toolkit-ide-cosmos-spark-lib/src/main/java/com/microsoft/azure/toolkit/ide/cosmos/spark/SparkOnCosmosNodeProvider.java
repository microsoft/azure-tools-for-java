package com.microsoft.azure.toolkit.ide.cosmos.spark;

import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.AzureSparkOnCosmosService;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosADLAccountNode;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosClusterNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class SparkOnCosmosNodeProvider implements IExplorerNodeProvider {

    private static final String NAME = "SparkOnCosmos";
    private static final String ICON = AzureIcons.ApacheSparkOnCosmos.MODULE.getIconPath();

    @javax.annotation.Nullable
    @Override
    public Object getRoot() {
        return az(AzureSparkOnCosmosService.class);
    }
    @Override
    public boolean accept(@NotNull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureSparkOnCosmosService
                || data instanceof SparkOnCosmosADLAccountNode
                || data instanceof SparkOnCosmosClusterNode;
    }

    @Nullable
    @Override
    public Node<?> createNode(@NotNull Object data, @Nullable Node<?> parent, @NotNull IExplorerNodeProvider.Manager manager) {
        if (data instanceof AzureSparkOnCosmosService) {
            final Function<AzureSparkOnCosmosService, List<SparkOnCosmosADLAccountNode>> adlas = s -> s.list().stream()
                    .flatMap(m -> m.adlas().list().stream()).collect(Collectors.toList());
            return new AzServiceNode<>((AzureSparkOnCosmosService) data)
                    .withIcon(ICON)
                    .withLabel("Apache Spark on Cosmos")
                    .withActions(SparkOnCosmosActionsContributor.SERVICE_ACTIONS)
                    .addChildren(adlas, (adla, serviceNode) -> this.createNode(adla, serviceNode, manager));
        } else if(data instanceof SparkOnCosmosADLAccountNode) {
            final SparkOnCosmosADLAccountNode adlAccountNode = (SparkOnCosmosADLAccountNode) data;

            return new AzResourceNode<>(adlAccountNode)
                    .withIcon(AzureIcon.builder().iconPath("/icons/AzureServerlessSparkAccount.png").build())
                    .withActions(SparkOnCosmosActionsContributor.ADLA_NODE_ACTIONS)
                    .addChildren(s -> s.getSparkOnCosmosClusterModule().list(), (d, mn) -> this.createNode(d, mn, manager));
        } else if(data instanceof SparkOnCosmosClusterNode) {
            final SparkOnCosmosClusterNode clusterNode = (SparkOnCosmosClusterNode) data;

            return new AzResourceNode<>(clusterNode)
                    .withIcon(AzureIcon.builder().iconPath("/icons/Cluster.png").build())
                    .withActions(SparkOnCosmosActionsContributor.CLUSTER_NODE_ACTIONS);
        } else {
            return null;
        }
    }
}
