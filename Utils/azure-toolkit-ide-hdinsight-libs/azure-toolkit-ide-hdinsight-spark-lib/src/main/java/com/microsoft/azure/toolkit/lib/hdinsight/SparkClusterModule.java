package com.microsoft.azure.toolkit.lib.hdinsight;

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
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
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
    @AzureOperation(name = "resource.load_resources_in_azure.type", params = {"this.getResourceTypeName()"})
    protected Stream<Cluster> loadResourcesFromAzure() {
        //log.debug("[{}]:loadResourcesFromAzure()", this.getName());
        return Optional.ofNullable( this.getClient()).map((c) -> {
            if (JobViewManager.REGISTERED_JOBVIEW_MAP>=0)
                    try {
                        JobViewManager.REGISTERED_JOBVIEW_MAP = -1;
                        ClusterManagerEx clusterManagerEx = ClusterManagerEx.getInstance();
                        ImmutableList<IClusterDetail> clusterDetails = clusterManagerEx.getClusterDetails();
                        for (IClusterDetail detail : clusterDetails) {
                            JobViewManager.registerJovViewNode(detail.getName(), detail);
                        }
                    } finally {
                        JobViewManager.REGISTERED_JOBVIEW_MAP = 1;
                    }

            List<Cluster> sourceList = c.list().stream().collect(Collectors.toList());
            List<Cluster> resultList = new ArrayList<Cluster>();

            // Remove duplicate clusters that share the same cluster name
            HashSet<String> clusterIdSet = new HashSet<>();
            for (Cluster cluster : sourceList)
                if (clusterIdSet.add(cluster.id()) && isSparkCluster(cluster.properties().clusterDefinition().kind()))
                    resultList.add(cluster);

            return resultList.stream();
        }).orElse(Stream.empty());
    }

    @Nullable
    @Override
    public Clusters getClient() {
        if (!Azure.az(AzureAccount.class).isLoggedIn())
            return null;
        return Optional.ofNullable(this.parent.getRemote()).map(HDInsightManager::clusters).orElse(null);
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

    @Override
    public void refresh() {
        this.additionalClusterSet.clear();
        super.refresh();
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
        return clusterKind.equalsIgnoreCase("spark");
    }

    @Override
    @Nonnull
    public String getSubscriptionId() {
        if (!Azure.az(AzureAccount.class).isLoggedIn())
            return "[LinkedCluster]";
        return super.getSubscriptionId();
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "HDInsight Clusters";
    }

}