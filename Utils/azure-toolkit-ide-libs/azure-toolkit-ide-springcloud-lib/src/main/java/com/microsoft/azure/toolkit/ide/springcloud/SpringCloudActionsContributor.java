/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.springcloud;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;

import java.util.Optional;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.lib.common.operation.OperationBundle.description;

public class SpringCloudActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String APP_ACTIONS = "actions.springcloud.app";
    public static final String CLUSTER_ACTIONS = "actions.springcloud.cluster";
    public static final String SERVICE_ACTIONS = "actions.springcloud.service";
    public static final Action.Id<SpringCloudApp> OPEN_PUBLIC_URL = Action.Id.of("springcloud.open_public_url");
    public static final Action.Id<SpringCloudApp> OPEN_TEST_URL = Action.Id.of("springcloud.open_test_url");
    public static final Action.Id<SpringCloudApp> STREAM_LOG = Action.Id.of("springcloud.stream_log");

    public static final Action.Id<Object> GROUP_CREATE_CLUSTER = Action.Id.of("group.create_spring_cluster");

    @Override
    public void registerActions(AzureActionManager am) {
        final Consumer<SpringCloudApp> openPublicUrl = s -> am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(s.getApplicationUrl());
        final ActionView.Builder openPublicUrlView = new ActionView.Builder("Access Public Endpoint", AzureIcons.Action.BROWSER.getIconPath())
                .title(s -> Optional.ofNullable(s).map(r -> description("springcloud.open_public_url.app", ((SpringCloudApp) r).name())).orElse(null))
                .enabled(s -> s instanceof SpringCloudApp && ((SpringCloudApp) s).isPublicEndpointEnabled());
        final Action<SpringCloudApp> openPublicUrlAction = new Action<>(OPEN_PUBLIC_URL, openPublicUrl, openPublicUrlView);
        openPublicUrlAction.setShortcuts("control alt P");
        am.registerAction(OPEN_PUBLIC_URL, openPublicUrlAction);

        final Consumer<SpringCloudApp> openTestUrl = s -> am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(s.getTestUrl());
        final ActionView.Builder openTestUrlView = new ActionView.Builder("Access Test Endpoint", AzureIcons.Action.BROWSER.getIconPath())
                .title(s -> Optional.ofNullable(s).map(r -> description("springcloud.open_test_url.app", ((SpringCloudApp) r).name())).orElse(null))
                .enabled(s -> s instanceof SpringCloudApp && ((SpringCloudApp) s).getFormalStatus().isConnected());
        final Action<SpringCloudApp> openTestUrlAction = new Action<>(OPEN_TEST_URL, openTestUrl, openTestUrlView);
        openTestUrlAction.setShortcuts("control alt T");
        am.registerAction(OPEN_TEST_URL, openTestUrlAction);

        final ActionView.Builder streamLogView = new ActionView.Builder("Streaming Log", AzureIcons.Action.LOG.getIconPath())
                .title(s -> Optional.ofNullable(s).map(r -> description("springcloud.open_stream_log.app", ((SpringCloudApp) r).name())).orElse(null))
                .enabled(s -> s instanceof SpringCloudApp && ((AzResourceBase) s).getFormalStatus().isRunning());
        am.registerAction(STREAM_LOG, new Action<>(STREAM_LOG, streamLogView));

        final ActionView.Builder createClusterView = new ActionView.Builder("Spring Apps")
            .title(s -> Optional.ofNullable(s).map(r -> description("springcloud.create_cluster.group", ((ResourceGroup) r).getName())).orElse(null))
            .enabled(s -> s instanceof ResourceGroup);
        am.registerAction(GROUP_CREATE_CLUSTER, new Action<>(GROUP_CREATE_CLUSTER, createClusterView));
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            "---",
            ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup clusterActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            "---",
            ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(CLUSTER_ACTIONS, clusterActionGroup);

        final ActionGroup appActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            SpringCloudActionsContributor.OPEN_PUBLIC_URL,
            SpringCloudActionsContributor.OPEN_TEST_URL,
            ResourceCommonActionsContributor.SHOW_PROPERTIES,
            "---",
            ResourceCommonActionsContributor.DEPLOY,
            "---",
            ResourceCommonActionsContributor.START,
            ResourceCommonActionsContributor.STOP,
            ResourceCommonActionsContributor.RESTART,
            ResourceCommonActionsContributor.DELETE,
            "---",
            SpringCloudActionsContributor.STREAM_LOG
        );
        am.registerGroup(APP_ACTIONS, appActionGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_CLUSTER);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
