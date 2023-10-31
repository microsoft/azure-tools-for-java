package com.microsoft.azure.toolkit.lib.hdinsight;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.hdinsight.HDInsightManager;
import com.azure.resourcemanager.hdinsight.models.Cluster;
import com.azure.resourcemanager.hdinsight.models.Clusters;
import com.google.common.collect.ImmutableList;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.hdinsight.sdk.cluster.*;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Stream;

import static com.microsoft.azure.toolkit.lib.common.model.AzResource.RESOURCE_GROUP_PLACEHOLDER;

public class SparkClusterModule extends AbstractAzResourceModule<SparkClusterNode, HDInsightServiceSubscription, Cluster> {

    public static final String NAME = "clusters";

    public static HashSet<String> additionalClusterSet = new HashSet<>();

    public SparkClusterModule(@Nonnull HDInsightServiceSubscription parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, Cluster>> loadResourcePagesFromAzure() {
        return Collections.singletonList(new ItemPage<>(this.loadResourcesFromAzure())).iterator();
    }

    @Nonnull
    @AzureOperation(name = "resource.load_resources_in_azure.type", params = {"this.getResourceTypeName()"})
    protected Stream<Cluster> loadResourcesFromAzure() {
        return Optional.ofNullable( this.getClient()).map((c) -> {
            // remote SDK request
            List<Cluster> sourceList = c.list().iterableByPage().iterator().next().getValue();
            List<Cluster> resultList = new ArrayList<Cluster>();
            if(sourceList.size() == 0) {
                return resultList.stream();
            }

            // local SDK request
            ClusterManagerEx clusterManagerEx = ClusterManagerEx.getInstance();
            ImmutableList<IClusterDetail> clusterDetails = clusterManagerEx.getClusterDetails();
            for (IClusterDetail detail : clusterDetails) {
                JobViewManager.registerJovViewNode(detail.getName(), detail);
            }

            // Remove duplicate clusters that share the same cluster name
            List<IClusterDetail> additionalClusterDetails = ClusterManagerEx.getInstance().getAdditionalClusterDetails();
            HashSet<String> clusterIdSet = new HashSet<>();
            for (IClusterDetail additionalCluster : additionalClusterDetails) {
                clusterIdSet.add(additionalCluster.getName());
            }
            for (Cluster cluster : sourceList) {
                if (clusterIdSet.add(cluster.id()) && isSparkCluster(cluster.properties().clusterDefinition().kind()))
                    resultList.add(cluster);
            }

            return resultList.stream();
        }).orElse(Stream.empty());
    }

    @Nullable
    @Override
    public Clusters getClient() {
        if (Azure.az(AzureAccount.class).isLoggedIn()) {
            return Optional.ofNullable(this.parent.getRemote()).map(HDInsightManager::clusters).orElse(null);
        } else {
            return null;
        }
    }

    public SparkClusterModule(@NotNull String name, @NotNull HDInsightServiceSubscription parent) {
        super(name, parent);
    }

    @Nullable
    @Override
    public SparkClusterNode get(@Nonnull String name, @Nullable String resourceGroup) {
        resourceGroup = StringUtils.firstNonBlank(resourceGroup, this.getParent().getResourceGroupName());
        if (StringUtils.isBlank(resourceGroup) || StringUtils.equalsIgnoreCase(resourceGroup, RESOURCE_GROUP_PLACEHOLDER)) {
            return this.list().stream().filter(c -> StringUtils.equalsIgnoreCase(name, c.getName())).findAny().orElse(null);
        }
        return super.get(name, resourceGroup);
    }

    @NotNull
    @Override
    protected SparkClusterNode newResource(@NotNull Cluster cluster) {
        return new SparkClusterNode(cluster,this);
    }

    @NotNull
    @Override
    protected SparkClusterNode newResource(@NotNull String name, @Nullable String resourceGroupName) {
        return new SparkClusterNode(name, Objects.requireNonNull(resourceGroupName),this);
    }

    private boolean isSparkCluster(String clusterKind) {
        return "spark".equalsIgnoreCase(clusterKind);
    }

    @Override
    @Nonnull
    public String getSubscriptionId() {
        if (Azure.az(AzureAccount.class).isLoggedIn()) {
            return super.getSubscriptionId();
        } else {
            return "[LinkedCluster]";
        }
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "HDInsight Clusters";
    }

}