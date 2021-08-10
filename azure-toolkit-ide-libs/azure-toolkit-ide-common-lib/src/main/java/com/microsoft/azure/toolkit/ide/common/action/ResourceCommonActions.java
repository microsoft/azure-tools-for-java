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
        final ActionView.Builder<IAzureResource<?>> startView = new ActionView.Builder<IAzureResource<?>>("Start", "/icons/action/start.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("common|resource.start", r.name()).toString()).orElse(null))
                .enabled(s -> s instanceof Startable && ((Startable<?>) s).isStartable());
        am.registerAction(START, new Action<>(start, startView));

        final Consumer<IAzureResource<?>> stop = s -> tm.runInBackground(title("common|resource.stop", s.name()), ((Startable<?>) s)::stop);
        final ActionView.Builder<IAzureResource<?>> stopView = new ActionView.Builder<IAzureResource<?>>("Stop", "/icons/action/stop.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("common|resource.stop", r.name()).toString()).orElse(null))
                .enabled(s -> s instanceof Startable && ((Startable<?>) s).isStoppable());
        am.registerAction(STOP, new Action<>(stop, stopView));

        final Consumer<IAzureResource<?>> restart = s -> tm.runInBackground(title("common|resource.restart", s.name()), ((Startable<?>) s)::restart);
        final ActionView.Builder<IAzureResource<?>> restartView = new ActionView.Builder<IAzureResource<?>>("Restart", "/icons/action/restart.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("common|resource.restart", r.name()).toString()).orElse(null))
                .enabled(s -> s instanceof Startable && ((Startable<?>) s).isRestartable());
        am.registerAction(RESTART, new Action<>(restart, restartView));

        final Consumer<IAzureResource<?>> delete = s -> tm.runInBackground(title("common|resource.delete", s.name()), ((Removable) s)::remove);
        final ActionView.Builder<IAzureResource<?>> deleteView = new ActionView.Builder<IAzureResource<?>>("Delete", "/icons/action/delete.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("common|resource.delete", r.name()).toString()).orElse(null))
                .enabled(s -> s instanceof Removable);
        am.registerAction(DELETE, new Action<>(delete, deleteView));

        final Consumer<IAzureResource<?>> refresh = s -> tm.runInBackground(title("common|resource.refresh", s.name()), s::refresh);
        final ActionView.Builder<IAzureResource<?>> refreshView = new ActionView.Builder<IAzureResource<?>>("Refresh", "/icons/action/refresh.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("common|resource.refresh", r.name()).toString()).orElse(null));
        am.registerAction(REFRESH, new Action<>(refresh, refreshView));

        final Consumer<AzureService> serviceRefresh = s -> tm.runInBackground(title("common|service.refresh", s.name()), s::refresh);
        final ActionView.Builder<AzureService> serviceRefreshView = new ActionView.Builder<AzureService>("Refresh", "/icons/action/refresh.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("common|service.refresh", r.name()).toString()).orElse(null));
        am.registerAction(SERVICE_REFRESH, new Action<>(serviceRefresh, serviceRefreshView));

        final Consumer<IAzureResource<?>> openPortalUrl = s -> tm.runInBackground(title("common|resource.open_portal_url", s.name()), () -> am.getAction(OPEN_URL).handle(s.portalUrl()));
        final ActionView.Builder<IAzureResource<?>> openPortalUrlView = new ActionView.Builder<IAzureResource<?>>("Open In Portal", "/icons/action/portal.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("common|resource.open_portal_url", r.name()).toString()).orElse(null));
        am.registerAction(OPEN_PORTAL_URL, new Action<>(openPortalUrl, openPortalUrlView));
    }

    private void registerCommand(AzureActionManager am) {
        am.registerAction(OPEN_URL, Action.emptyHandler());

        final ActionView.Builder<IAzureResource<?>> showPropertiesView = new ActionView.Builder<IAzureResource<?>>("Show Properties", "/icons/action/properties.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("common|resource.show_properties", r.name()).toString()).orElse(null));
        am.registerAction(SHOW_PROPERTIES, new Action<>(Action.emptyHandler(), showPropertiesView));

        final ActionView.Builder<IAzureResource<?>> deployView = new ActionView.Builder<IAzureResource<?>>("Deploy", "/icons/action/deploy.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("common|resource.deploy", r.name()).toString()).orElse(null));
        am.registerAction(DEPLOY, new Action<>(Action.emptyHandler(), deployView));

        final ActionView.Builder<IAzureResource<?>> createView = new ActionView.Builder<IAzureResource<?>>("Create", "/icons/action/create.svg")
                .description(s -> Optional.ofNullable(s).map(r -> title("common|resource.create", r.name()).toString()).orElse(null));
        am.registerAction(CREATE, new Action<>(Action.emptyHandler(), createView));
    }
}
