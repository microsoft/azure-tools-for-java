package com.microsoft.azure.toolkit.lib.hdinsight;

import com.azure.resourcemanager.hdinsight.models.Cluster;
import com.azure.resourcemanager.hdinsight.models.ClusterGetProperties;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.SDKAdditionalCluster;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SparkClusterNode extends AbstractAzResource<SparkClusterNode, HDInsightServiceSubscription, Cluster> {

    private IClusterDetail clusterDetail;
    private StorageAccountMudule storageAccountMudule;
    /**
     * copy constructor
     */
    protected SparkClusterNode(@Nonnull SparkClusterNode origin) {
        super(origin);
    }

    protected SparkClusterNode(@Nonnull String name, @Nonnull String resourceGroup, @Nonnull SparkClusterModule module) {
        super(name, resourceGroup, module);
        this.storageAccountMudule = new StorageAccountMudule(this);
    }

    protected SparkClusterNode(@Nonnull Cluster remote, @Nonnull SparkClusterModule module) {
        super(remote.name(), ResourceId.fromString(remote.id()).resourceGroupName(), module);
        this.storageAccountMudule = new StorageAccountMudule(this);
    }


    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        final ArrayList<AbstractAzResourceModule<?, ?, ?>> modules = new ArrayList<>();
        modules.add(this.storageAccountMudule);
        return modules;
    }

    @Override
    public void reloadStatus() {
        if (!Azure.az(AzureAccount.class).isLoggedIn() || this.getSubscriptionId().equals("[LinkedCluster]"))
            this.setStatus("Linked");
        else
            super.reloadStatus();
    }

    @Override
    @Nonnull
    public String getStatus() {
        if (!Azure.az(AzureAccount.class).isLoggedIn() || this.getSubscriptionId().equals("[LinkedCluster]"))
            return "Linked";
        else
            return super.getStatus();
    }

    @Override
    @Nonnull
    protected Optional<Cluster> remoteOptional(boolean... sync) {
        if (!Azure.az(AzureAccount.class).isLoggedIn() || this.getSubscriptionId().equals("[LinkedCluster]"))
            return null;
        else
            return super.remoteOptional(sync);
    }

    @Override
    @Nullable
    protected Cluster refreshRemoteFromAzure(@Nonnull Cluster remote) {
        if (!Azure.az(AzureAccount.class).isLoggedIn() || this.getSubscriptionId().equals("[LinkedCluster]"))
            return null;
        else
            return super.refreshRemoteFromAzure(remote);
    }

    @NotNull
    @Override
    public String loadStatus(@NotNull Cluster remote) {
        if (remote instanceof SDKAdditionalCluster)
            return "Linked";

        ClusterGetProperties p = remote.properties();
        return new StringBuffer().append("(Spark:")
                .append(p.clusterDefinition().componentVersion().get("Spark")).append(")")
                .append(p.clusterState()).toString();
    }

    @NotNull
    @Override
    public Subscription getSubscription() {
        if (!Azure.az(AzureAccount.class).isLoggedIn() || this.getSubscriptionId().equals("[LinkedCluster]"))
            return new Subscription("[LinkedCluster]");
        return super.getSubscription();
    }

    public IClusterDetail getClusterDetail() {
        return clusterDetail;
    }

    public void setClusterDetail(IClusterDetail clusterDetail) {
        this.clusterDetail = clusterDetail;
    }

}
