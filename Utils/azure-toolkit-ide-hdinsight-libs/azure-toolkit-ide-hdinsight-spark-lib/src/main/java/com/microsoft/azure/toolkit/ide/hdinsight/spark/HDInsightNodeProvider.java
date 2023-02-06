package com.microsoft.azure.toolkit.ide.hdinsight.spark;

import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.hdinsight.sdk.cluster.SDKAdditionalCluster;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzureServiceLabelView;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.hdinsight.spark.component.SparkClusterNodeView;
import com.microsoft.azure.toolkit.ide.hdinsight.spark.component.SparkJobNodeView;
import com.microsoft.azure.toolkit.ide.hdinsight.spark.component.StorageAccountModuleNodeView;
import com.microsoft.azure.toolkit.ide.hdinsight.spark.component.StorageAccountNodeView;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.hdinsight.AzureHDInsightService;
import com.microsoft.azure.toolkit.lib.hdinsight.SparkClusterNode;
import com.microsoft.azure.toolkit.lib.hdinsight.StorageAccountNode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class HDInsightNodeProvider implements IExplorerNodeProvider {

    private static final String NAME = "HDInsight";
    private static final String ICON = AzureIcons.HDInsight.MODULE.getIconPath();

    @Nullable
    @Override
    public Object getRoot() {
        return az(AzureHDInsightService.class);
    }

    @Override
    public boolean accept(@Nonnull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof AzureHDInsightService
                || data instanceof SparkClusterNode
                || data instanceof StorageAccountNode;
    }

    @Nullable
    @Override
    public Node<?> createNode(@Nonnull Object data,@Nullable Node<?> parent,@Nonnull Manager manager) {
        if (data instanceof AzureHDInsightService) {
            AzureHDInsightService service = (AzureHDInsightService)data;
            Function<AzureHDInsightService, List<SparkClusterNode>> clusters = s -> s.list().stream()
                    .flatMap(m -> m.clusters().list().stream()).collect(Collectors.toList());
            Function<AzureHDInsightService, List<SparkClusterNode>> additionalClusters = s -> s.listAdditionalCluster();
                return new Node<>(service).view(new AzureServiceLabelView(service, "HDInsight", ICON))
                        .actions(HDInsightActionsContributor.SERVICE_ACTIONS)
                        .addChildren(additionalClusters, (cluster, serviceNode) -> this.createNode(cluster, serviceNode, manager))
                        .addChildren(clusters, (cluster, serviceNode) -> this.createNode(cluster, serviceNode, manager));
        } else if (data instanceof SparkClusterNode) {
            final SparkClusterNode sparkClusterNode = (SparkClusterNode) data;
            Optional.ofNullable(JobViewManager.getCluster(sparkClusterNode.getName()))
                    .ifPresent(c->{sparkClusterNode.setClusterDetail(c);});
            Node<SparkClusterNode> jobsNode = new Node<>(sparkClusterNode)
                    .view(new SparkJobNodeView(sparkClusterNode))
                    .clickAction(HDInsightActionsContributor.OPEN_HDINSIGHT_JOB_VIEW);
            if (sparkClusterNode.getClusterDetail().getResourceGroup()==null)//.getRemote(true) instanceof SDKAdditionalCluster)
                return new Node<>(sparkClusterNode)
                        .view(new SparkClusterNodeView(sparkClusterNode))
                        .inlineAction(ResourceCommonActionsContributor.PIN)
                        .actions(HDInsightActionsContributor.SPARK_ADDITIONAL_CLUSTER_ACTIONS)
                        .addChild(jobsNode)
                        .addChildren(SparkClusterNode::getSubModules,(module, p) -> new Node<>(module)
                                .view(new StorageAccountModuleNodeView(module))
                                .addChildren(AbstractAzResourceModule::list, (d, mn) -> this.createNode(d, mn, manager))
                                .clickAction(HDInsightActionsContributor.OPEN_AZURE_STORAGE_EXPLORER_ON_MODULE));
            return new Node<>(sparkClusterNode)
                    .view(new SparkClusterNodeView(sparkClusterNode))
                    .inlineAction(ResourceCommonActionsContributor.PIN)
                    .actions(HDInsightActionsContributor.SPARK_CLUSTER_ACTIONS)
                    .addChild(jobsNode)
                    .addChildren(SparkClusterNode::getSubModules,(module, p) -> new Node<>(module)
                            .view(new StorageAccountModuleNodeView(module))
                            .addChildren(AbstractAzResourceModule::list, (d, mn) -> this.createNode(d, mn, manager)));
        } else if (data instanceof StorageAccountNode) {
            final StorageAccountNode storageAccountNode = (StorageAccountNode) data;
            return new Node<>(storageAccountNode)
                    .view(new StorageAccountNodeView(storageAccountNode))
                    .actions(HDInsightActionsContributor.HDINSIGHT_STORAGE_ACTIONS);
        } else {
            return null;
        }
    }

}