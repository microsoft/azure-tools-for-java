package com.microsoft.azure.toolkit.lib.synapse;

import com.azure.resourcemanager.hdinsight.HDInsightManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import org.apache.commons.lang3.StringUtils;
import com.azure.resourcemanager.synapse.SynapseManager;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SynapseServiceSubscription extends AbstractAzServiceSubscription<SynapseServiceSubscription, SynapseManager> {
    @Nonnull
    private final String subscriptionId;
    @Nonnull
    private final WorkspaceModule workspaceModule;

    protected SynapseServiceSubscription(@Nonnull String subscriptionId, @Nonnull AzureSynapseService service) {
        super(subscriptionId, service);
        this.subscriptionId = subscriptionId;
        this.workspaceModule = new WorkspaceModule(this);
    }

    @Nonnull
    public WorkspaceModule workspaces() {
        return this.workspaceModule;
    }

    protected SynapseServiceSubscription(@Nonnull SynapseManager manager, @Nonnull AzureSynapseService service) {
        this(StringUtils.EMPTY, service);
    }

    @Override
    @Nonnull
    protected Optional<SynapseManager> remoteOptional() {
        return Optional.ofNullable(this.getRemote());
    }

    @Nonnull
    @Override
    public List<AbstractAzResourceModule<?, ?, ?>> getSubModules() {
        return Collections.singletonList(workspaceModule);
    }

    @Override
    @Nonnull
    public String getSubscriptionId() {
        return subscriptionId;
    }
}
