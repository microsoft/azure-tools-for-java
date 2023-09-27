package com.microsoft.azure.toolkit.lib.synapse;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.synapse.SynapseManager;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.SDKAdditionalCluster;
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AzureSynapseService extends AbstractAzService<SynapseServiceSubscription, SynapseManager> {

    public AzureSynapseService() {
        super("Microsoft.Synapse");
    }

    @Nonnull
    @Override
    protected SynapseServiceSubscription newResource(@Nonnull SynapseManager manager) {
        return new SynapseServiceSubscription(manager.serviceClient().getSubscriptionId(), this);
    }

    @Nullable
    @Override
    protected SynapseManager loadResourceFromAzure(@Nonnull String subscriptionId, String resourceGroup) {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        final AzureProfile azureProfile = new AzureProfile(null, subscriptionId, account.getEnvironment());
        return SynapseManager.configure()
                .withHttpClient(AbstractAzServiceSubscription.getDefaultHttpClient())
                .withLogOptions(new HttpLogOptions().setLogLevel(logLevel))
                .withPolicy(AbstractAzServiceSubscription.getUserAgentPolicy(userAgent))
                .authenticate(account.getTokenCredential(subscriptionId), azureProfile);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "synapseclusters";
    }

    @Override
    public String getServiceNameForTelemetry() {
        return "synapse";
    }

}