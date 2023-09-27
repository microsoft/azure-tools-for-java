package com.microsoft.azure.toolkit.lib.sparkoncosmos;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkCompute;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SparkOnCosmosClusterNode extends AbstractAzResource<SparkOnCosmosClusterNode, SparkOnCosmosADLAccountNode, AzureSparkCosmosCluster> {
    protected SparkOnCosmosClusterNode(@NotNull String name, @NotNull String resourceGroupName, @NotNull AbstractAzResourceModule<SparkOnCosmosClusterNode, SparkOnCosmosADLAccountNode, AzureSparkCosmosCluster> module) {
        super(name, resourceGroupName, module);
    }

    protected SparkOnCosmosClusterNode(@Nonnull AzureSparkCosmosCluster remote, @Nonnull SparkOnCosmosClusterModule module) {
        super(remote.getName(), module.getAdlAccountNode().getResourceGroupName(), module);
    }
    @NotNull
    @Override
    protected String loadStatus(@NotNull AzureSparkCosmosCluster remote) {
        return "[" + Optional.ofNullable(remote.getMasterState())
                .orElse(remote.getState()).toUpperCase() + "]";
    }

    @Nullable
    public ResourceGroup getResourceGroup() {
        try {
            return super.getResourceGroup();
        } catch (Exception ignore) {
            return null;
        }
    }

    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.EMPTY_LIST;
    }
}
