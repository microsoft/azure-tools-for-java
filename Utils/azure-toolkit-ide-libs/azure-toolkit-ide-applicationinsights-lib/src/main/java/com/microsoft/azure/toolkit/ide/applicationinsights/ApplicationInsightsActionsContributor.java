/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.applicationinsights;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.common.action.*;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Optional;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.lib.common.operation.OperationBundle.description;

public class ApplicationInsightsActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.applicationinsights.service";
    public static final String INSIGHT_ACTIONS = "actions.applicationinsights.management";

    public static final Action.Id<ApplicationInsight> APPLICATION_MAP = Action.Id.of("applicationinsights.application_map");
    public static final Action.Id<ApplicationInsight> LIVE_METRICS = Action.Id.of("applicationinsights.live_metrics");
    public static final Action.Id<ApplicationInsight> TRANSACTION_SEARCH = Action.Id.of("applicationinsights.transaction_search");
    public static final Action.Id<ApplicationInsight> INSTRUMENTATION_KEY = Action.Id.of("applicationinsights.instrumentation_key");
    public static final Action.Id<ApplicationInsight> CONNECTION_STRING = Action.Id.of("applicationinsights.copy_connection_string");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_APPLICATIONINSIGHT = Action.Id.of("group.create_applicationinsights");

    @Override
    public void registerActions(AzureActionManager am) {
        final Consumer<ApplicationInsight> copyConnectionStringConsumer = insight -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(insight.getConnectionString()), null);
            AzureMessager.getMessager().info("Connection string copied");
        };
        final ActionView.Builder copyConnectionStringView = new ActionView.Builder("Copy Connection String")
                .title(s -> Optional.ofNullable(s).map(r -> description("applicationinsights.instrumentation_key.applicationinsights",
                        ((ApplicationInsight) r).getName())).orElse(null))
                .enabled(s -> s instanceof ApplicationInsight && ((ApplicationInsight) s).getFormalStatus().isConnected());
        am.registerAction(CONNECTION_STRING, new Action<>(CONNECTION_STRING, copyConnectionStringConsumer, copyConnectionStringView));

        final Consumer<ApplicationInsight> copyInstrumentationKeyConsumer = insight -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(insight.getInstrumentationKey()), null);
            AzureMessager.getMessager().info("Instrumentation key copied");
        };
        final ActionView.Builder copyInstrumentationKeyView = new ActionView.Builder("Copy Instrumentation Key")
                .title(s -> Optional.ofNullable(s).map(r -> description("applicationinsights.instrumentation_key.applicationinsights",
                        ((ApplicationInsight) r).getName())).orElse(null))
                .enabled(s -> s instanceof ApplicationInsight && ((ApplicationInsight) s).getFormalStatus().isConnected());
        am.registerAction(INSTRUMENTATION_KEY, new Action<>(INSTRUMENTATION_KEY, copyInstrumentationKeyConsumer, copyInstrumentationKeyView));

        final Consumer<ApplicationInsight> applicationMapConsumer = insight -> am.getAction(ResourceCommonActionsContributor.OPEN_URL)
                .handle(insight.getPortalUrl() + "/applicationMap");
        final ActionView.Builder applicationMapView = new ActionView.Builder("Open Application Map")
                .title(s -> Optional.ofNullable(s).map(r -> description("applicationinsights.application_map.applicationinsights",
                        ((ApplicationInsight) r).getName())).orElse(null))
                .enabled(s -> s instanceof ApplicationInsight && ((ApplicationInsight) s).getFormalStatus().isConnected());
        am.registerAction(APPLICATION_MAP, new Action<>(APPLICATION_MAP, applicationMapConsumer, applicationMapView));

        final Consumer<ApplicationInsight> liveMetricsConsumer = insight -> am.getAction(ResourceCommonActionsContributor.OPEN_URL)
                .handle(insight.getPortalUrl() + "/quickPulse");
        final ActionView.Builder liveMetricsView = new ActionView.Builder("Open Live Metrics")
                .title(s -> Optional.ofNullable(s).map(r -> description("applicationinsights.live_metrics.applicationinsights",
                        ((ApplicationInsight) r).getName())).orElse(null))
                .enabled(s -> s instanceof ApplicationInsight && ((ApplicationInsight) s).getFormalStatus().isConnected());
        am.registerAction(LIVE_METRICS, new Action<>(LIVE_METRICS, liveMetricsConsumer, liveMetricsView));

        final Consumer<ApplicationInsight> transactionSearchConsumer = insight -> am.getAction(ResourceCommonActionsContributor.OPEN_URL)
                .handle(insight.getPortalUrl() + "/searchV1");
        final ActionView.Builder transactionSearchView = new ActionView.Builder("Open Transaction Search")
                .title(s -> Optional.ofNullable(s).map(r -> description("applicationinsights.transaction_search.applicationinsights",
                        ((ApplicationInsight) r).getName())).orElse(null))
                .enabled(s -> s instanceof ApplicationInsight && ((ApplicationInsight) s).getFormalStatus().isConnected());
        am.registerAction(TRANSACTION_SEARCH, new Action<>(TRANSACTION_SEARCH, transactionSearchConsumer, transactionSearchView));

        final ActionView.Builder createInsightView = new ActionView.Builder("Application Insights")
                .title(s -> Optional.ofNullable(s).map(r ->
                        description("group.create_applicationinsights.group", ((ResourceGroup) r).getName())).orElse(null))
                .enabled(s -> s instanceof ResourceGroup);
        am.registerAction(GROUP_CREATE_APPLICATIONINSIGHT, new Action<>(GROUP_CREATE_APPLICATIONINSIGHT, createInsightView));
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
                "---",
                ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup accountActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.PIN,
                "---",
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_PORTAL_URL,
                "---",
                ApplicationInsightsActionsContributor.CONNECTION_STRING,
                ApplicationInsightsActionsContributor.INSTRUMENTATION_KEY,
                "---",
                ResourceCommonActionsContributor.CONNECT,
                ResourceCommonActionsContributor.DELETE,
                "---",
                ApplicationInsightsActionsContributor.APPLICATION_MAP,
                ApplicationInsightsActionsContributor.LIVE_METRICS,
                ApplicationInsightsActionsContributor.TRANSACTION_SEARCH
        );
        am.registerGroup(INSIGHT_ACTIONS, accountActionGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_APPLICATIONINSIGHT);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
