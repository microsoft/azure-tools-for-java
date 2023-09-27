package com.microsoft.azure.toolkit.ide.sqlserver.spark;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail;
import com.microsoft.azure.sqlbigdata.serverexplore.SqlBigDataClusterModule;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.AzModuleNode;
import com.microsoft.azure.toolkit.ide.common.component.AzResourceNode;
import com.microsoft.azure.toolkit.ide.common.component.AzServiceNode;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.favorite.Favorites;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.sqlserverbigdata.SqlserverBigDataModule;
import com.microsoft.azure.toolkit.lib.sqlserverbigdata.SqlserverBigDataNode;
import com.microsoft.azure.toolkit.lib.sqlserverbigdata.SqlserverBigDataService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class SqlserverBigDataNodeProvider implements IExplorerNodeProvider {

    @Nullable
    @Override
    public Object getRoot() {
        return az(SqlserverBigDataService.class);
    }

    @Override
    public boolean accept(@NotNull Object data, @Nullable Node<?> parent, ViewType type) {
        return data instanceof SqlserverBigDataService
                || data instanceof SqlserverBigDataNode;
    }

    @Nullable
    @Override
    public Node<?> createNode(@NotNull Object data, @Nullable Node<?> parent, @NotNull IExplorerNodeProvider.Manager manager) {
        if (data instanceof SqlserverBigDataService) {
            final Function<SqlserverBigDataService, List<SqlserverBigDataNode>> additionalClusters = s -> s.listCluster();

            return new AzServiceNode<>((SqlserverBigDataService) data)
                    .withIcon(AzureIcons.SQLServerBigDataCluster.MODULE)
                    .withLabel("SQL Server Big Data Cluster")
                    .withActions(new ActionGroup(SqlserverBigDataActionsContributor.REFRESH,
                                                SqlserverBigDataActionsContributor.LINK_CLUSTER))
                    .addChildren(additionalClusters,(a,b)->createNode(a,b,manager));
        } else if (data instanceof SqlserverBigDataNode) {
            return new AzResourceNode<>((SqlserverBigDataNode) data)
                    .withIcon(AzureIcon.builder().iconPath("/icons/Cluster.png").build())
                    .withActions(SqlserverBigDataActionsContributor.SQLSERVER_CLUSTER_ACTIONS);
        }
        return null;
    }

    @Override
    public boolean isAzureService() {
        return false;
    }

}
