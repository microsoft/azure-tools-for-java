package com.microsoft.azure.toolkit.lib.synapse;

import com.azure.core.util.paging.ContinuablePage;
import com.azure.resourcemanager.hdinsight.HDInsightManager;
import com.azure.resourcemanager.hdinsight.models.Cluster;
import com.azure.resourcemanager.hdinsight.models.Clusters;
import com.azure.resourcemanager.synapse.SynapseManager;
import com.azure.resourcemanager.synapse.models.Workspace;
import com.azure.resourcemanager.synapse.models.Workspaces;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.hdinsight.sdk.cluster.*;
import com.microsoft.azure.projectarcadia.common.ArcadiaSparkComputeManager;
import com.microsoft.azure.projectarcadia.common.ArcadiaWorkSpace;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.microsoft.azure.toolkit.lib.common.model.AzResource.RESOURCE_GROUP_PLACEHOLDER;

public class WorkspaceModule extends AbstractAzResourceModule<WorkspaceNode, SynapseServiceSubscription, ArcadiaWorkSpace> {

    public static final String NAME = "workspaces";

    public WorkspaceModule(@Nonnull SynapseServiceSubscription parent) {
        super(NAME, parent);
    }

    public WorkspaceModule(@NotNull String name, @NotNull SynapseServiceSubscription parent) {
        super(name, parent);
    }

    @Nonnull
    @Override
    protected Iterator<? extends ContinuablePage<String, ArcadiaWorkSpace>> loadResourcePagesFromAzure() {
        return Collections.singletonList(new ItemPage<>(this.loadResourcesFromAzure())).iterator();
    }

    @Nonnull
    @AzureOperation(name = "resource.load_resources_in_azure.type", params = {"this.getResourceTypeName()"})
    protected Stream<ArcadiaWorkSpace> loadResourcesFromAzure() {
            ArcadiaSparkComputeManager.getInstance().refresh();
            List<ArcadiaWorkSpace> workspaces = ArcadiaSparkComputeManager.getInstance().getWorkspaces().stream().collect(Collectors.toList());
            return workspaces.stream();
    }


    @Nullable
    @Override
    public WorkspaceNode get(@Nonnull String name, @Nullable String resourceGroup) {
        resourceGroup = StringUtils.firstNonBlank(resourceGroup, this.getParent().getResourceGroupName());
        if (StringUtils.isBlank(resourceGroup) || StringUtils.equalsIgnoreCase(resourceGroup, RESOURCE_GROUP_PLACEHOLDER)) {
            return this.list().stream().filter(c -> StringUtils.equalsIgnoreCase(name, c.getName())).findAny().orElse(null);
        }
        return super.get(name, resourceGroup);
    }

    @NotNull
    @Override
    protected WorkspaceNode newResource(@NotNull ArcadiaWorkSpace workspace) {
        return new WorkspaceNode(workspace,this);
    }

    @NotNull
    @Override
    protected WorkspaceNode newResource(@NotNull String name, @Nullable String resourceGroupName) {
        return new WorkspaceNode(name, Objects.requireNonNull(resourceGroupName),this);
    }


    @Override
    @Nonnull
    public String getSubscriptionId() {
        return super.getSubscriptionId();
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "Synapse Workspaces";
    }

    @Override
    public ArcadiaWorkSpace getClient() {
        return null;
    }

}