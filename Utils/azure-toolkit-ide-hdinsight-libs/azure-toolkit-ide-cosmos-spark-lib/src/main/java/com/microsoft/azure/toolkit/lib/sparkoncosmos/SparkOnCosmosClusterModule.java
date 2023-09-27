package com.microsoft.azure.toolkit.lib.sparkoncosmos;

import com.azure.core.util.paging.ContinuablePage;
import com.google.common.collect.ImmutableSortedSet;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkClusterNode;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosClusterManager;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkCompute;
import com.microsoft.azure.projectarcadia.common.ArcadiaWorkSpace;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Observable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.microsoft.azure.toolkit.lib.common.model.AzResource.RESOURCE_GROUP_PLACEHOLDER;

public class SparkOnCosmosClusterModule extends AbstractAzResourceModule<SparkOnCosmosClusterNode, SparkOnCosmosADLAccountNode, AzureSparkCosmosCluster> {

    public static final String NAME = "SparkOnCosmosClusters";

    private SparkOnCosmosADLAccountNode adlAccountNode;

    public SparkOnCosmosClusterModule(@NotNull SparkOnCosmosADLAccountNode parent) {
        super(NAME, parent);
        this.adlAccountNode = parent;
    }

    public SparkOnCosmosClusterModule(@NotNull String name, @NotNull SparkOnCosmosADLAccountNode parent) {
        super(name, parent);
        this.adlAccountNode = parent;
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, AzureSparkCosmosCluster>> loadResourcePagesFromAzure() {
        return Collections.singletonList(new ItemPage<>(this.loadResourcesFromAzure())).iterator();
    }

    @Nonnull
    @AzureOperation(name = "resource.load_resources_in_azure.type", params = {"this.getResourceTypeName()"})
    protected Stream<AzureSparkCosmosCluster> loadResourcesFromAzure() {
        AzureSparkServerlessAccount remote;
        try {
            remote = this.adlAccountNode.getRemote();
        } catch (Exception ingore) {
            AzureSparkCosmosClusterManager.getInstance().refresh();
            remote = AzureSparkCosmosClusterManager.getInstance()
                    .getAccountByName(this.adlAccountNode.getName());
        }
        List<AzureSparkCosmosCluster> list = new ArrayList<>();
        remote.get().subscribe(account -> account.getClusters().forEach(cluster -> {
            try {
                AzureSparkCosmosCluster serverlessCluster = (AzureSparkCosmosCluster) cluster;
                // refresh the cluster
                serverlessCluster.getConfigurationInfo();
                list.add(serverlessCluster);
            } catch (Exception ignore) {

            }}));


        return list.stream();
    }

    @NotNull
    @Override
    protected SparkOnCosmosClusterNode newResource(@NotNull AzureSparkCosmosCluster cluster) {
        return new SparkOnCosmosClusterNode(cluster,this);
    }

    @NotNull
    @Override
    protected SparkOnCosmosClusterNode newResource(@NotNull String name, @Nullable String resourceGroupName) {
        return new SparkOnCosmosClusterNode(name,resourceGroupName,this);
    }

    public SparkOnCosmosADLAccountNode getAdlAccountNode() {
        return this.adlAccountNode;
    }

    @Nullable
    @Override
    public SparkOnCosmosClusterNode get(@Nonnull String name, @Nullable String resourceGroup) {
        resourceGroup = StringUtils.firstNonBlank(resourceGroup, this.getParent().getResourceGroupName());
        if (StringUtils.isBlank(resourceGroup) || StringUtils.equalsIgnoreCase(resourceGroup, RESOURCE_GROUP_PLACEHOLDER)) {
            return this.list().stream().filter(c -> StringUtils.equalsIgnoreCase(name, c.getName())).findAny().orElse(null);
        }
        return super.get(name, resourceGroup);
    }

    public AzureSparkCosmosCluster getClient() {
        return null;
    }

}
