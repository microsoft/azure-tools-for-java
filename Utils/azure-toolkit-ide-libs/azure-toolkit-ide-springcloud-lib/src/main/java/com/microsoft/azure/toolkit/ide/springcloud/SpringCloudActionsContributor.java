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
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;

import java.util.Optional;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.lib.common.operation.OperationBundle.description;

public class SpringCloudActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String APP_ACTIONS = "actions.springcloud.app";
    public static final String CLUSTER_ACTIONS = "actions.springcloud.cluster";
    public static final String SERVICE_ACTIONS = "actions.springcloud.service";
    public static final String APP_INSTANCE_ACTIONS = "actions.springcould.appInstance";
    public static final Action.Id<SpringCloudApp> OPEN_PUBLIC_URL = Action.Id.of("springcloud.open_public_url");
    public static final Action.Id<SpringCloudApp> OPEN_TEST_URL = Action.Id.of("springcloud.open_test_url");
    public static final Action.Id<SpringCloudApp> STREAM_LOG = Action.Id.of("springcloud.stream_log");
    public static final Action.Id<SpringCloudApp> ENABLE_REMOTE_DEBUGGING = Action.Id.of("springcloud.enable_remote_debugging");
    public static final Action.Id<SpringCloudApp> DISABLE_REMOTE_DEBUGGING = Action.Id.of("springcloud.disable_remote_debugging");
    public static final Action.Id<SpringCloudAppInstance> ATTACH_DEBUGGER = Action.Id.of("springcloud.attach_debugger");
    public static final Action.Id<Object> GROUP_CREATE_CLUSTER = Action.Id.of("group.create_spring_cluster");

    @Override
    public void registerActions(AzureActionManager am) {
        final Consumer<SpringCloudApp> openPublicUrl = s -> am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(s.getApplicationUrl());
        final ActionView.Builder openPublicUrlView = new ActionView.Builder("Access Public Endpoint", AzureIcons.Action.BROWSER.getIconPath())
                .title(s -> Optional.ofNullable(s).map(r -> description("springcloud.open_public_url.app", ((SpringCloudApp) r).getName())).orElse(null))
                .enabled(s -> s instanceof SpringCloudApp && ((SpringCloudApp) s).getFormalStatus().isConnected() && ((SpringCloudApp) s).isPublicEndpointEnabled());
        final Action<SpringCloudApp> openPublicUrlAction = new Action<>(OPEN_PUBLIC_URL, openPublicUrl, openPublicUrlView);
        openPublicUrlAction.setShortcuts("control alt P");
        am.registerAction(OPEN_PUBLIC_URL, openPublicUrlAction);

        final Consumer<SpringCloudApp> openTestUrl = s -> am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(s.getTestUrl());
        final ActionView.Builder openTestUrlView = new ActionView.Builder("Access Test Endpoint", AzureIcons.Action.BROWSER.getIconPath())
                .title(s -> Optional.ofNullable(s).map(r -> description("springcloud.open_test_url.app", ((SpringCloudApp) r).getName())).orElse(null))
                .enabled(s -> s instanceof SpringCloudApp && ((SpringCloudApp) s).getFormalStatus().isConnected());
        final Action<SpringCloudApp> openTestUrlAction = new Action<>(OPEN_TEST_URL, openTestUrl, openTestUrlView);
        openTestUrlAction.setShortcuts("control alt T");
        am.registerAction(OPEN_TEST_URL, openTestUrlAction);

        final ActionView.Builder streamLogView = new ActionView.Builder("Streaming Log", AzureIcons.Action.LOG.getIconPath())
                .title(s -> Optional.ofNullable(s).map(r -> description("springcloud.open_stream_log.app", ((SpringCloudApp) r).getName())).orElse(null))
                .enabled(s -> s instanceof SpringCloudApp && ((AzResourceBase) s).getFormalStatus().isRunning());
        am.registerAction(STREAM_LOG, new Action<>(STREAM_LOG, streamLogView));

        final ActionView.Builder createClusterView = new ActionView.Builder("Spring Apps")
            .title(s -> Optional.ofNullable(s).map(r -> description("springcloud.create_cluster.group", ((ResourceGroup) r).getName())).orElse(null))
            .enabled(s -> s instanceof ResourceGroup && ((ResourceGroup) s).getFormalStatus().isConnected());
        am.registerAction(GROUP_CREATE_CLUSTER, new Action<>(GROUP_CREATE_CLUSTER, createClusterView));

        final ActionView.Builder enableRemoteDebuggingView = new ActionView.Builder("Enable Remote Debugging")
                .title(s -> Optional.ofNullable(s).map(r -> description("springcloud.enable_remote_debugging.app", ((SpringCloudApp) r).getName())).orElse(null))
                .enabled(s -> s instanceof SpringCloudApp);
        am.registerAction(ENABLE_REMOTE_DEBUGGING, new Action<>(ENABLE_REMOTE_DEBUGGING, enableRemoteDebuggingView));

        final ActionView.Builder disableRemoteDebuggingView = new ActionView.Builder("Disable Remote Debugging")
                .title(s -> Optional.ofNullable(s).map(r -> description("springcloud.disable_remote_debugging.app", ((SpringCloudApp) r).getName())).orElse(null))
                .enabled(s -> s instanceof SpringCloudApp);
        am.registerAction(DISABLE_REMOTE_DEBUGGING, new Action<>(DISABLE_REMOTE_DEBUGGING, disableRemoteDebuggingView));

        final ActionView.Builder attachDebuggerView = new ActionView.Builder("Attach Debugger", AzureIcons.Action.DEBUG.getIconPath())
                .title(s -> Optional.ofNullable(s).map(r -> description("springcloud.attach_debugger.instance", ((SpringCloudAppInstance) r).getName())).orElse(null))
                .enabled(s -> s instanceof SpringCloudAppInstance);
        am.registerAction(ATTACH_DEBUGGER, new Action<>(ATTACH_DEBUGGER, attachDebuggerView));
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

        final ActionGroup clusterActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
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
            SpringCloudActionsContributor.STREAM_LOG,
            SpringCloudActionsContributor.ENABLE_REMOTE_DEBUGGING,
            SpringCloudActionsContributor.DISABLE_REMOTE_DEBUGGING
        );
        am.registerGroup(APP_ACTIONS, appActionGroup);

        final ActionGroup appInstanceGroup = new ActionGroup(
                ResourceCommonActionsContributor.PIN,
                "---",
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_PORTAL_URL,
                SpringCloudActionsContributor.ATTACH_DEBUGGER
        );
        am.registerGroup(APP_INSTANCE_ACTIONS, appInstanceGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_CLUSTER);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
