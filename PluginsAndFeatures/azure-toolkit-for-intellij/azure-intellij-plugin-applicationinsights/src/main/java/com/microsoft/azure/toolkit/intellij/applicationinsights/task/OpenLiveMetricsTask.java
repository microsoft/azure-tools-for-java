package com.microsoft.azure.toolkit.intellij.applicationinsights.task;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.AzureApplicationInsights;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

import java.util.List;

import static com.microsoft.azure.toolkit.ide.applicationinsights.ApplicationInsightsActionsContributor.LIVE_METRICS;

public class OpenLiveMetricsTask implements Task {

    private final ComponentContext context;

    public OpenLiveMetricsTask(@Nonnull final ComponentContext context) {
        this.context = context;
    }

    @Override
    @AzureOperation(name = "guidance.open_live_metrics", type = AzureOperation.Type.SERVICE)
    public void execute() throws Exception {
        final String applicationInsightsId = (String) context.getParameter("applicationInsightsId");
        final String instrumentKey = (String) context.getParameter("instrumentKey");
        final ApplicationInsight applicationInsight = StringUtils.isNoneEmpty(applicationInsightsId) ?
                getInsightsById(applicationInsightsId) : getInsightsByInstrumentKey(instrumentKey);
        AzureActionManager.getInstance().getAction(LIVE_METRICS).handle(applicationInsight);
    }

    private ApplicationInsight getInsightsById(@Nonnull final String id) {
        final ResourceId resourceId = ResourceId.fromString(id);
        return Azure.az(AzureApplicationInsights.class).applicationInsights(resourceId.subscriptionId()).get(id);
    }

    private ApplicationInsight getInsightsByInstrumentKey(@Nonnull final String instrumentKey) {
        final List<Subscription> subscriptions = Azure.az(AzureAccount.class).account().getSelectedSubscriptions();
        return subscriptions.stream().map(subscription -> Azure.az(AzureApplicationInsights.class).applicationInsights(subscription.getId()).list())
                .flatMap(List::stream)
                .filter(insight -> StringUtils.equalsIgnoreCase(insight.getInstrumentationKey(), instrumentKey))
                .findFirst().orElseThrow(() ->
                        new AzureToolkitRuntimeException(String.format("Failed to find application insights instance with instrument key %s", instrumentKey)));
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.application_insights.live_metrics";
    }
}
