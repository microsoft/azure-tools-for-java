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
    public static final String OPEN_PUBLIC_URL = "action.springcloud.app.open_public_url";
    public static final String OPEN_TEST_URL = "action.springcloud.app.open_test_url";

    @Override
    public void registerActions(AzureActionManager am) {
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        final Consumer<SpringCloudApp> openPublicUrl = s -> {
            final Runnable runnable = () -> am.getAction(ResourceCommonActions.OPEN_URL).handle(s.publicUrl());
            tm.runInBackground(AzureOperationBundle.title("springcloud|app.open_public_url", s.name()), runnable);
        };
        final ActionView.Builder<SpringCloudApp> openPublicUrlView = new ActionView.Builder<SpringCloudApp>("Access Public Endpoint", "/icons/action/browser.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("springcloud|app.open_public_url", r.name()).toString()).orElse(null))
                .enabled(s -> s instanceof SpringCloudApp && ((SpringCloudApp) s).entity().isPublic());
        am.registerAction(OPEN_PUBLIC_URL, new Action<>(openPublicUrl, openPublicUrlView));

        final Consumer<SpringCloudApp> openTestUrl = s -> {
            final Runnable runnable = () -> am.getAction(ResourceCommonActions.OPEN_URL).handle(s.testUrl());
            tm.runInBackground(AzureOperationBundle.title("springcloud|app.open_test_url", s.name()), runnable);
        };
        final ActionView.Builder<SpringCloudApp> openTestUrlView = new ActionView.Builder<SpringCloudApp>("Access Test Endpoint", "/icons/action/browser.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("springcloud|app.open_test_url", r.name()).toString()).orElse(null))
                .enabled(Objects::nonNull);
        am.registerAction(OPEN_TEST_URL, new Action<>(openTestUrl, openTestUrlView));
    }

    @Override
    public void registerHandlers(AzureActionManager am) {
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup("",
                ResourceCommonActions.SERVICE_REFRESH
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup clusterActionGroup = new ActionGroup("",
                ResourceCommonActions.OPEN_PORTAL_URL,
                "---",
                ResourceCommonActions.CREATE,
                "---",
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
                ResourceCommonActions.REFRESH
        );
        am.registerGroup(APP_ACTIONS, appActionGroup);
    }
}
