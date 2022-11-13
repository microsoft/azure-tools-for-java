/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.applicationinsights;

import com.google.common.base.Preconditions;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.applicationinsights.ApplicationInsightsActionsContributor;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.applicationinsights.connection.ApplicationInsightsResourceDefinition;
import com.microsoft.azure.toolkit.intellij.applicationinsights.creation.CreateApplicationInsightsAction;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsightDraft;
import com.microsoft.azure.toolkit.lib.applicationinsights.AzureApplicationInsights;
import com.microsoft.azure.toolkit.lib.applicationinsights.workspace.LogAnalyticsWorkspaceConfig;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class IntelliJApplicationInsightsActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<Object, AnActionEvent> condition = (r, e) -> r instanceof AzureApplicationInsights;
        final BiConsumer<Object, AnActionEvent> handler = (c, e) ->
                CreateApplicationInsightsAction.create(e.getProject(), getDraftApplicationInsight(null));
        am.registerHandler(ResourceCommonActionsContributor.CREATE, condition, handler);

        final BiConsumer<ResourceGroup, AnActionEvent> groupCreateAccountHandler = (r, e) ->
                CreateApplicationInsightsAction.create(e.getProject(), getDraftApplicationInsight(r));
        am.registerHandler(ApplicationInsightsActionsContributor.GROUP_CREATE_APPLICATIONINSIGHT, (r, e) -> true, groupCreateAccountHandler);

        final BiPredicate<AzResource, AnActionEvent> connectCondition = (r, e) -> r instanceof ApplicationInsight;
        final BiConsumer<AzResource, AnActionEvent> connectHandler = (r, e) -> AzureTaskManager.getInstance().runLater(
                OperationBundle.description("resource.connect_resource.resource", r.getName()), () -> {
                    final ConnectorDialog dialog = new ConnectorDialog(e.getProject());
                    dialog.setResource(new AzureServiceResource<>(((ApplicationInsight) r), ApplicationInsightsResourceDefinition.INSTANCE));
                    dialog.show();
                });
        am.registerHandler(ResourceCommonActionsContributor.CONNECT, connectCondition, connectHandler);
    }

    private static ApplicationInsightDraft getDraftApplicationInsight(@Nullable final ResourceGroup resourceGroup) {
        final List<Subscription> subs = Azure.az(AzureAccount.class).account().getSelectedSubscriptions();
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(subs), "There are no subscriptions in your account.");

        final Subscription historySub = CacheManager.getUsageHistory(Subscription.class).peek(subs::contains);
        final ResourceGroup historyRg = CacheManager.getUsageHistory(ResourceGroup.class)
            .peek(r -> Objects.isNull(historySub) ? subs.stream().anyMatch(s -> s.getId().equals(r.getSubscriptionId())) : r.getSubscriptionId().equals(historySub.getId()));

        final String timestamp = Utils.getTimestamp();
        final ResourceGroup rg = Optional.ofNullable(resourceGroup)
            .or(() -> Optional.ofNullable(historyRg))
            .orElse(null);
        final Subscription subscription = Optional.ofNullable(rg).map(AzResource::getSubscription)
            .or(() -> Optional.ofNullable(historySub).filter(subs::contains))
            .orElse(subs.get(0));
        final List<Region> regions = az(AzureAccount.class).listRegions(subscription.getId());
        final Region historyRegion = CacheManager.getUsageHistory(Region.class).peek(regions::contains);
        final Region region = Optional.ofNullable(rg).map(ResourceGroup::getRegion)
            .or(() -> Optional.ofNullable(historyRegion))
            .orElse(null);
        final String resourceGroupName = Optional.ofNullable(rg)
            .map(AbstractAzResource::getResourceGroupName)
            .orElse(String.format("rg-%s", timestamp));
        final ApplicationInsightDraft applicationInsightDraft = Azure.az(AzureApplicationInsights.class).applicationInsights(subscription.getId())
            .create(String.format("ai-%s", timestamp), resourceGroupName);
        applicationInsightDraft.setRegion(region);
        applicationInsightDraft.setWorkspaceConfig(LogAnalyticsWorkspaceConfig.builder().newCreate(true).subscriptionId(subscription.getId())
                .resourceGroupName(resourceGroupName).name(String.format("workspace-%s", timestamp)).build());
        return applicationInsightDraft;
    }

    @Override
    public int getOrder() {
        return ApplicationInsightsActionsContributor.INITIALIZE_ORDER + 1;
    }
}
