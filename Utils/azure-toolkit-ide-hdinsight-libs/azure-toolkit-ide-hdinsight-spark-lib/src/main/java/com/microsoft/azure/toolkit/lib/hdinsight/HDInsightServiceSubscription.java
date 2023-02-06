package com.microsoft.azure.toolkit.lib.hdinsight;

import com.azure.resourcemanager.hdinsight.HDInsightManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class HDInsightServiceSubscription extends AbstractAzServiceSubscription<HDInsightServiceSubscription, HDInsightManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final SparkClusterModule sparkClusterModule;

    protected HDInsightServiceSubscription(@NotNull String subscriptionId, @NotNull AzureHDInsightService service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.sparkClusterModule = new SparkClusterModule(this);
    }

    @Nonnull
    public SparkClusterModule clusters() {
        return this.sparkClusterModule;
    }

    protected HDInsightServiceSubscription(@Nonnull HDInsightManager manager, @Nonnull AzureHDInsightService service) {
        this(manager.serviceClient().getSubscriptionId(), service);
    }

    @Override
    public void reloadStatus() {
        if (Azure.az(AzureAccount.class).isLoggedIn())
            super.reloadStatus();
        else
            this.setStatus("Linked");
    }

    @Override
    @Nonnull
    public String getStatus() {
        if (Azure.az(AzureAccount.class).isLoggedIn())
            return super.getStatus();
        else
            return "Linked";
    }

    @Override
    @Nonnull
    protected Optional<HDInsightManager> remoteOptional(boolean... sync) {
        if (Azure.az(AzureAccount.class).isLoggedIn())
            return Optional.ofNullable(this.getRemote(sync));
        else
            return null;
    }

    @Override
    @Nullable
    protected HDInsightManager refreshRemoteFromAzure(@Nonnull HDInsightManager remote) {
        if (Azure.az(AzureAccount.class).isLoggedIn())
            return super.refreshRemoteFromAzure(remote);
        else
            return null;
    }

    @NotNull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(sparkClusterModule);
    }

    @Override
    @Nonnull
    public String getSubscriptionId() {
        return subscriptionId;
    }
}
