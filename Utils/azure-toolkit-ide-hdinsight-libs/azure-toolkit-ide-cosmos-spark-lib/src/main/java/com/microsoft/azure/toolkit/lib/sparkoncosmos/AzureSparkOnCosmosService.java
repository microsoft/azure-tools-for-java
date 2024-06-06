package com.microsoft.azure.toolkit.lib.sparkoncosmos;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.cosmos.CosmosManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class AzureSparkOnCosmosService extends AbstractAzService<SparkOnCosmosServiceSubscription, CosmosManager> {
    public AzureSparkOnCosmosService() {
        super("Microsoft.SparkOnCosmos");
    }
    @NotNull
    @Override
    protected SparkOnCosmosServiceSubscription newResource(@NotNull CosmosManager cosmosManager) {
        return new SparkOnCosmosServiceSubscription(cosmosManager, this);
    }

    @Nullable
    @Override
    protected CosmosManager loadResourceFromAzure(@Nonnull String subscriptionId, String resourceGroup) {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        final AzureProfile azureProfile = new AzureProfile(null, subscriptionId, account.getEnvironment());
        return CosmosManager.configure()
                .withHttpClient(AbstractAzServiceSubscription.getDefaultHttpClient())
                .withLogOptions(new HttpLogOptions().setLogLevel(logLevel))
                .withPolicy(AbstractAzServiceSubscription.getUserAgentPolicy(userAgent))
                .authenticate(account.getTokenCredential(subscriptionId), azureProfile);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "sparkoncosmosclusters";
    }

    @Override
    public String getServiceNameForTelemetry() {
        return "sparkoncosmos";
    }

}
