package com.microsoft.azure.toolkit.lib.sqlserverbigdata;

import com.azure.resourcemanager.hdinsight.models.Cluster;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SqlserverBigDataNode extends AbstractAzResource<SqlserverBigDataNode, SqlserverBigDataServiceSubscription, IClusterDetail> {

    private IClusterDetail iClusterDetail;

    protected SqlserverBigDataNode(@NotNull String name, @NotNull String resourceGroupName, @NotNull AbstractAzResourceModule<SqlserverBigDataNode, SqlserverBigDataServiceSubscription, IClusterDetail> module) {
        super(name, resourceGroupName, module);
    }

    @NotNull
    @Override
    protected String loadStatus(@NotNull IClusterDetail remote) {
        return StringUtils.EMPTY;
    }

    @NotNull
    @Override
    public String getStatus() {
        return StringUtils.EMPTY;
    }

    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.emptyList();
    }

    @Override
    @Nonnull
    protected Optional<IClusterDetail> remoteOptional() {
        return Optional.empty();
    }

    @Override
    public String getResourceGroupName(){
        return "[LinkedCluster]";
    }

    @Nonnull
    @Override
    public Subscription getSubscription() {
        return new Subscription("[LinkedCluster]");
    }

    public void setiClusterDetail(IClusterDetail iClusterDetail) {
        this.iClusterDetail = iClusterDetail;
    }

    public IClusterDetail getiClusterDetail() {
        return this.iClusterDetail;
    }

}
