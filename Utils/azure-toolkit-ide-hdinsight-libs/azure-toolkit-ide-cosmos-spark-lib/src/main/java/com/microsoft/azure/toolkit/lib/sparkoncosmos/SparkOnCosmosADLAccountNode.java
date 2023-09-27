package com.microsoft.azure.toolkit.lib.sparkoncosmos;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.projectarcadia.common.ArcadiaWorkSpace;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosADLAccountModule;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosServiceSubscription;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SparkOnCosmosADLAccountNode extends AbstractAzResource<SparkOnCosmosADLAccountNode, SparkOnCosmosServiceSubscription, AzureSparkServerlessAccount> {

    private final SparkOnCosmosClusterModule sparkOnCosmosClusterModule;
    /**
     * copy constructor
     */
    protected SparkOnCosmosADLAccountNode(@Nonnull SparkOnCosmosADLAccountNode origin) {
        super(origin);
        this.sparkOnCosmosClusterModule = new SparkOnCosmosClusterModule(this);
    }


    protected SparkOnCosmosADLAccountNode(@Nonnull String name, @Nonnull String resourceGroup, @Nonnull SparkOnCosmosADLAccountModule module) {
        super(name, resourceGroup, module);
        this.sparkOnCosmosClusterModule = new SparkOnCosmosClusterModule(this);
    }

    protected SparkOnCosmosADLAccountNode(@Nonnull AzureSparkServerlessAccount remote, @Nonnull SparkOnCosmosADLAccountModule module) {
        super(remote.getName(), ResourceId.fromString(remote.getId()).resourceGroupName(), module);
        this.sparkOnCosmosClusterModule = new SparkOnCosmosClusterModule(this);
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        final ArrayList<AbstractAzResourceModule<?, ?, ?>> modules = new ArrayList<>();
        modules.add(this.sparkOnCosmosClusterModule);
        return modules;
    }

    public SparkOnCosmosClusterModule getSparkOnCosmosClusterModule() {
        return sparkOnCosmosClusterModule;
    }

    @Override
    public String getStatus() {
        return StringUtils.EMPTY;
    }

    @NotNull
    @Override
    protected String loadStatus(@NotNull AzureSparkServerlessAccount remote) {
        return null;
    }

    @Nonnull
    @Override
    public Subscription getSubscription() {
        return super.getSubscription();
    }

    @Nullable
    public ResourceGroup getResourceGroup() {
        try {
            return super.getResourceGroup();
        } catch (Exception e) {
            return null;
        }
    }

}
