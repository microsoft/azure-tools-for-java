/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.springcloud;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.Action;
import com.microsoft.azure.toolkit.ide.common.action.ActionGroup;
import com.microsoft.azure.toolkit.ide.common.action.ActionView;
import com.microsoft.azure.toolkit.ide.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActions;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle.title;

public class SpringCloudActions implements IActionsContributor {

    public static final String APP_ACTIONS = "actions.springcloud.app";
    public static final String CLUSTER_ACTIONS = "actions.springcloud.cluster";
    public static final String SERVICE_ACTIONS = "actions.springcloud.service";
    public static final Action.Id<SpringCloudApp> OPEN_PUBLIC_URL = Action.Id.of("action.springcloud.app.open_public_url");
    public static final Action.Id<SpringCloudApp> OPEN_TEST_URL = Action.Id.of("action.springcloud.app.open_test_url");
    public static final Action.Id<SpringCloudApp> STREAM_LOG = Action.Id.of("action.springcloud.app.stream_log");

    @Override
    public void registerActions(AzureActionManager am) {
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        final Consumer<SpringCloudApp> openPublicUrl = s -> {
            final Runnable runnable = () -> am.getAction(ResourceCommonActions.OPEN_URL).handle(s.publicUrl());
            tm.runInBackground(AzureOperationBundle.title("springcloud|app.open_public_url", s.name()), runnable);
        };
        final ActionView.Builder openPublicUrlView = new ActionView.Builder("Access Public Endpoint", "/icons/action/browser.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("springcloud|app.open_public_url", ((SpringCloudApp) r).name()).toString()).orElse(null))
                .enabled(s -> s instanceof SpringCloudApp && ((SpringCloudApp) s).entity().isPublic());
        am.registerAction(OPEN_PUBLIC_URL, new Action<>(openPublicUrl, openPublicUrlView));

        final Consumer<SpringCloudApp> openTestUrl = s -> {
            final Runnable runnable = () -> am.getAction(ResourceCommonActions.OPEN_URL).handle(s.testUrl());
            tm.runInBackground(AzureOperationBundle.title("springcloud|app.open_test_url", s.name()), runnable);
        };
        final ActionView.Builder openTestUrlView = new ActionView.Builder("Access Test Endpoint", "/icons/action/browser.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("springcloud|app.open_test_url", ((SpringCloudApp) r).name()).toString()).orElse(null))
                .enabled(Objects::nonNull);
        am.registerAction(OPEN_TEST_URL, new Action<>(openTestUrl, openTestUrlView));

        final ActionView.Builder streamLogView = new ActionView.Builder("Streaming Log", "/icons/action/log.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("springcloud|app.stream_log", ((SpringCloudApp) r).name()).toString()).orElse(null))
                .enabled(s -> s instanceof SpringCloudApp);
        am.registerAction(STREAM_LOG, new Action<>(Action.emptyHandler(), streamLogView));
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup("",
                ResourceCommonActions.SERVICE_REFRESH
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup clusterActionGroup = new ActionGroup("",
                ResourceCommonActions.OPEN_PORTAL_URL,
                ResourceCommonActions.CREATE,
                ResourceCommonActions.REFRESH
        );
        am.registerGroup(CLUSTER_ACTIONS, clusterActionGroup);

        final ActionGroup appActionGroup = new ActionGroup("",
                ResourceCommonActions.OPEN_PORTAL_URL,
                SpringCloudActions.OPEN_PUBLIC_URL,
                SpringCloudActions.OPEN_TEST_URL,
                "---",
                ResourceCommonActions.START,
                ResourceCommonActions.STOP,
                ResourceCommonActions.RESTART,
                ResourceCommonActions.DELETE,
                "---",
                ResourceCommonActions.DEPLOY,
                "---",
                ResourceCommonActions.SHOW_PROPERTIES,
                ResourceCommonActions.REFRESH,
                "---",
                SpringCloudActions.STREAM_LOG
        );
        am.registerGroup(APP_ACTIONS, appActionGroup);
    }
}
