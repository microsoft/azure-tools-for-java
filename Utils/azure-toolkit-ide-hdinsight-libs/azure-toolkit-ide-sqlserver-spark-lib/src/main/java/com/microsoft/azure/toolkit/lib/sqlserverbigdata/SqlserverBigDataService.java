package com.microsoft.azure.toolkit.lib.sqlserverbigdata;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SqlserverBigDataService extends AbstractAzService<SqlserverBigDataServiceSubscription, SqlserverBigDataManager> {

    public SqlserverBigDataService() {
        super("Microsoft.SQLServer");
    }

    @NotNull
    @Override
    protected SqlserverBigDataServiceSubscription newResource(@NotNull SqlserverBigDataManager sqlserverBigDataManager) {
        return new SqlserverBigDataServiceSubscription();
    }

    public List<SqlserverBigDataNode> listCluster() {
        final List<SqlserverBigDataNode> list = new ArrayList<>();

        final List<IClusterDetail> clusterDetailList = ClusterManagerEx.getInstance().getClusterDetails().stream()
                .filter(clusterDetail -> clusterDetail instanceof SqlBigDataLivyLinkClusterDetail)
                .toList();
        for (final IClusterDetail iClusterDetail : clusterDetailList) {
            final SqlserverBigDataNode node = new SqlserverBigDataNode(iClusterDetail.getName(),"",SqlserverBigDataModule.getInstance());
            node.setiClusterDetail(iClusterDetail);
            list.add(node);
        }
        return list;
    }

    public boolean isAuthRequiredForListing() {
        return false;
    }

    @Override
    public boolean isAuthRequiredForCreating() {
        return false;
    }
}
