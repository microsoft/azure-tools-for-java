/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.action;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import com.microsoft.azure.toolkit.lib.common.entity.Removable;
import com.microsoft.azure.toolkit.lib.common.entity.Startable;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import java.util.Optional;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle.title;

public class ResourceCommonActions implements IActionsContributor {

    public static final String START = "action.resource.start";
    public static final String STOP = "action.resource.stop";
    public static final String RESTART = "action.resource.restart";
    public static final String REFRESH = "action.resource.refresh";
    public static final String SERVICE_REFRESH = "action.service.refresh";
    public static final String DELETE = "action.resource.delete";
    public static final String OPEN_PORTAL_URL = "action.resource.open_portal_url";
    public static final String SHOW_PROPERTIES = "action.resource.show_properties";
    public static final String DEPLOY = "action.resource.deploy";
    public static final String CREATE = "action.resource.create";
    public static final String OPEN_URL = "action.open_url";

    @Override
    public void registerActions(AzureActionManager am) {
        this.registerCommand(am);
        final AzureTaskManager tm = AzureTaskManager.getInstance();

        final Consumer<IAzureResource<?>> start = s -> tm.runInBackground(title("common|resource.start", s.name()), ((Startable<?>) s)::start);
        final ActionView.Builder startView = new ActionView.Builder("Start", "/icons/action/start.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("common|resource.start", ((IAzureResource<?>) r).name()).toString()).orElse(null))
                .enabled(s -> s instanceof Startable && ((Startable<?>) s).isStartable());
        am.registerAction(START, new Action<>(start, startView));

        final Consumer<IAzureResource<?>> stop = s -> tm.runInBackground(title("common|resource.stop", s.name()), ((Startable<?>) s)::stop);
        final ActionView.Builder stopView = new ActionView.Builder("Stop", "/icons/action/stop.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("common|resource.stop", ((IAzureResource<?>) r).name()).toString()).orElse(null))
                .enabled(s -> s instanceof Startable && ((Startable<?>) s).isStoppable());
        am.registerAction(STOP, new Action<>(stop, stopView));

        final Consumer<IAzureResource<?>> restart = s -> tm.runInBackground(title("common|resource.restart", s.name()), ((Startable<?>) s)::restart);
        final ActionView.Builder restartView = new ActionView.Builder("Restart", "/icons/action/restart.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("common|resource.restart", ((IAzureResource<?>) r).name()).toString()).orElse(null))
                .enabled(s -> s instanceof Startable && ((Startable<?>) s).isRestartable());
        am.registerAction(RESTART, new Action<>(restart, restartView));

        final Consumer<IAzureResource<?>> delete = s -> tm.runInBackground(title("common|resource.delete", s.name()), ((Removable) s)::remove);
        final ActionView.Builder deleteView = new ActionView.Builder("Delete", "/icons/action/delete.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("common|resource.delete", ((IAzureResource<?>) r).name()).toString()).orElse(null))
                .enabled(s -> s instanceof Removable);
        am.registerAction(DELETE, new Action<>(delete, deleteView));

        final Consumer<IAzureResource<?>> refresh = s -> tm.runInBackground(title("common|resource.refresh", s.name()), s::refresh);
        final ActionView.Builder refreshView = new ActionView.Builder("Refresh", "/icons/action/refresh.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("common|resource.refresh", ((IAzureResource<?>) r).name()).toString()).orElse(null))
                .enabled(s -> s instanceof IAzureResource);
        am.registerAction(REFRESH, new Action<>(refresh, refreshView));

        final Consumer<AzureService> serviceRefresh = s -> tm.runInBackground(title("common|service.refresh", s.name()), s::refresh);
        final ActionView.Builder serviceRefreshView = new ActionView.Builder("Refresh", "/icons/action/refresh.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("common|service.refresh", ((AzureService) r).name()).toString()).orElse(null))
                .enabled(s -> s instanceof AzureService);
        am.registerAction(SERVICE_REFRESH, new Action<>(serviceRefresh, serviceRefreshView));

        final Consumer<IAzureResource<?>> openPortalUrl = s -> tm.runInBackground(title("common|resource.open_portal_url", s.name()), () -> am.getAction(OPEN_URL).handle(s.portalUrl()));
        final ActionView.Builder openPortalUrlView = new ActionView.Builder("Open In Portal", "/icons/action/portal.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("common|resource.open_portal_url", ((IAzureResource<?>) r).name()).toString()).orElse(null))
                .enabled(s -> s instanceof IAzureResource);
        am.registerAction(OPEN_PORTAL_URL, new Action<>(openPortalUrl, openPortalUrlView));
    }

    private void registerCommand(AzureActionManager am) {
        am.registerAction(OPEN_URL, Action.emptyHandler());

        final ActionView.Builder showPropertiesView = new ActionView.Builder("Show Properties", "/icons/action/properties.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("common|resource.show_properties", ((IAzureResource<?>) r).name()).toString()).orElse(null));
        am.registerAction(SHOW_PROPERTIES, new Action<>(Action.emptyHandler(), showPropertiesView));

        final ActionView.Builder deployView = new ActionView.Builder("Deploy", "/icons/action/deploy.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("common|resource.deploy", ((IAzureResource<?>) r).name()).toString()).orElse(null));
        am.registerAction(DEPLOY, new Action<>(Action.emptyHandler(), deployView));

        final ActionView.Builder createView = new ActionView.Builder("Create", "/icons/action/create.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("common|resource.create", ((IAzureResource<?>) r).name()).toString()).orElse(null));
        am.registerAction(CREATE, new Action<>(Action.emptyHandler(), createView));
    }
}
