package com.microsoft.azure.toolkit.lib.sparkoncosmos;

import com.azure.core.util.paging.ContinuablePage;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosClusterManager;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosServiceSubscription;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosADLAccountNode;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.microsoft.azure.toolkit.lib.common.model.AzResource.RESOURCE_GROUP_PLACEHOLDER;

public class SparkOnCosmosADLAccountModule extends AbstractAzResourceModule<SparkOnCosmosADLAccountNode, SparkOnCosmosServiceSubscription, AzureSparkServerlessAccount> {

    public static final String NAME = "adlaccounts";

    public SparkOnCosmosADLAccountModule(@Nonnull SparkOnCosmosServiceSubscription parent) {
        super(NAME, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, AzureSparkServerlessAccount>> loadResourcePagesFromAzure() {
        return Collections.singletonList(new ItemPage<>(this.loadResourcesFromAzure())).iterator();
    }

    @Nonnull
    @AzureOperation(name = "resource.load_resources_in_azure.type", params = {"this.getResourceTypeName()"})
    protected Stream<AzureSparkServerlessAccount> loadResourcesFromAzure() {
        AzureSparkCosmosClusterManager.getInstance().refresh();
        List<AzureSparkServerlessAccount> collect = AzureSparkCosmosClusterManager.getInstance().getAccounts().stream().collect(Collectors.toList());
        return collect.stream();
    }

    public SparkOnCosmosADLAccountModule(@NotNull String name, @NotNull SparkOnCosmosServiceSubscription parent) {
        super(name, parent);
    }

    @Nullable
    @Override
    public SparkOnCosmosADLAccountNode get(@Nonnull String name, @Nullable String resourceGroup) {
        resourceGroup = StringUtils.firstNonBlank(resourceGroup, this.getParent().getResourceGroupName());
        if (StringUtils.isBlank(resourceGroup) || StringUtils.equalsIgnoreCase(resourceGroup, RESOURCE_GROUP_PLACEHOLDER)) {
            return this.list().stream().filter(c -> StringUtils.equalsIgnoreCase(name, c.getName())).findAny().orElse(null);
        }
        return super.get(name, resourceGroup);
    }

    @NotNull
    @Override
    protected SparkOnCosmosADLAccountNode newResource(@NotNull AzureSparkServerlessAccount account) {
        return new SparkOnCosmosADLAccountNode(account,this);
    }

    @NotNull
    @Override
    protected SparkOnCosmosADLAccountNode newResource(@NotNull String name, @Nullable String resourceGroupName) {
        return new SparkOnCosmosADLAccountNode(name, Objects.requireNonNull(resourceGroupName),this);
    }


    @Override
    @Nonnull
    public String getSubscriptionId() {
        if (Azure.az(AzureAccount.class).isLoggedIn()) {
            return super.getSubscriptionId();
        } else {
            return StringUtils.EMPTY;
        }
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "SparkOnCosmos ADLAs";
    }

}