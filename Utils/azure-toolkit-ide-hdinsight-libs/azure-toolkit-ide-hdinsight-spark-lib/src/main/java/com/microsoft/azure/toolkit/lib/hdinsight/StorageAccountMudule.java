package com.microsoft.azure.toolkit.lib.hdinsight;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.hdinsight.models.StorageAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.page.ItemPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class StorageAccountMudule extends AbstractAzResourceModule <StorageAccountNode,SparkClusterNode, com.azure.resourcemanager.hdinsight.models.StorageAccount>{

    public static final String NAME = "Storage Accounts";
    private SparkClusterNode sparkClusterNode;

    public StorageAccountMudule(@NotNull SparkClusterNode parent) {
        super(NAME, parent);
        this.sparkClusterNode = parent;
    }

    @Override
    public List<StorageAccountNode> list() {
        if (sparkClusterNode.getSubscription().getId().equals("[LinkedCluster]")) {
            return Collections.emptyList();
        } else {
            return super.list();
        }
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, com.azure.resourcemanager.hdinsight.models.StorageAccount>> loadResourcePagesFromAzure() {
        final Stream<StorageAccount> resources = Optional.ofNullable(sparkClusterNode.getRemote(true))
            .map(r -> r.properties().storageProfile().storageaccounts().stream()).orElse(Stream.empty());
        return Collections.singletonList(new ItemPage<>(resources)).iterator();
    }

    @NotNull
    @Override
    protected StorageAccountNode newResource(@NotNull com.azure.resourcemanager.hdinsight.models.StorageAccount storageAccount) {
        return new StorageAccountNode(NAME,this.sparkClusterNode.getResourceGroupName(),this);
    }

    @NotNull
    @Override
    protected StorageAccountNode newResource(@NotNull String name, @Nullable String resourceGroupName) {
        return new StorageAccountNode(NAME,this);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Storage Accounts";
    }
}
