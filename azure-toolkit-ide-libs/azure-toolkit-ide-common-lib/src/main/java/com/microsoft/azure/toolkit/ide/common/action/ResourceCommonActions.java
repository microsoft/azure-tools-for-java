/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.action;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.component.IView;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureResource;
import com.microsoft.azure.toolkit.lib.common.entity.Removable;
import com.microsoft.azure.toolkit.lib.common.entity.Startable;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import java.util.function.Consumer;
import java.util.function.Function;

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

        final Consumer<IAzureResource<?>> start = s -> tm.runInBackground(AzureOperationBundle.title("common|resource.start", s.name()), ((Startable<?>) s)::start);
        final Function<IAzureResource<?>, IView.Label> startActionView = s -> {
            final String description = AzureOperationBundle.title("common|resource.start", s.name()).toString();
            final boolean enabled = s instanceof Startable && ((Startable<?>) s).isStartable();
            return new ActionView(new IView.Label.Static("Start", "/icons/action/start.svg", description), enabled);
        };
        am.registerAction(START, new Action<>(start, startActionView));

        final Consumer<IAzureResource<?>> stop = s -> tm.runInBackground(AzureOperationBundle.title("common|resource.stop", s.name()), ((Startable<?>) s)::stop);
        final Function<IAzureResource<?>, IView.Label> stopActionView = s -> {
            final String description = AzureOperationBundle.title("common|resource.stop", s.name()).toString();
            final boolean enabled = s instanceof Startable && ((Startable<?>) s).isStoppable();
            return new ActionView(new IView.Label.Static("Stop", "/icons/action/stop.svg", description), enabled);
        };
        am.registerAction(STOP, new Action<>(stop, stopActionView));

        final Consumer<IAzureResource<?>> restart = s -> tm.runInBackground(AzureOperationBundle.title("common|resource.restart", s.name()), ((Startable<?>) s)::restart);
        final Function<IAzureResource<?>, IView.Label> restartActionView = s -> {
            final String description = AzureOperationBundle.title("common|resource.restart", s.name()).toString();
            final boolean enabled = s instanceof Startable && ((Startable<?>) s).isRestartable();
            return new ActionView(new IView.Label.Static("Restart", "/icons/action/restart.svg", description), enabled);
        };
        am.registerAction(RESTART, new Action<>(restart, restartActionView));

        final Consumer<IAzureResource<?>> refresh = s -> tm.runInBackground(AzureOperationBundle.title("common|resource.refresh", s.name()), s::refresh);
        final Function<IAzureResource<?>, IView.Label> refreshActionView = s -> {
            final String description = AzureOperationBundle.title("common|resource.refresh", s.name()).toString();
            return new IView.Label.Static("Refresh", "/icons/action/refresh.svg", description);
        };
        am.registerAction(REFRESH, new Action<>(refresh, refreshActionView));

        final Consumer<AzureService> serviceRefresh = s -> tm.runInBackground(AzureOperationBundle.title("common|service.refresh", s.name()), s::refresh);
        final Function<AzureService, IView.Label> serviceRefreshView = s -> {
            final String description = AzureOperationBundle.title("common|service.refresh", s.name()).toString();
            return new IView.Label.Static("Refresh", "/icons/action/refresh.svg", description);
        };
        am.registerAction(SERVICE_REFRESH, new Action<>(serviceRefresh, serviceRefreshView));

        final Consumer<IAzureResource<?>> delete = s -> tm.runInBackground(AzureOperationBundle.title("common|resource.delete", s.name()), ((Removable) s)::remove);
        final Function<IAzureResource<?>, IView.Label> deleteActionView = s -> {
            final String description = AzureOperationBundle.title("common|resource.delete", s.name()).toString();
            return new ActionView(new IView.Label.Static("Delete", "/icons/action/delete.svg", description), s instanceof Removable);
        };
        am.registerAction(DELETE, new Action<>(delete, deleteActionView));

        final Consumer<IAzureResource<?>> openPortalUrl = s -> {
            final Runnable runnable = () -> am.getAction(OPEN_URL).handle(s.portalUrl());
            tm.runInBackground(AzureOperationBundle.title("common|resource.open_portal_url", s.name()), runnable);
        };
        final Function<IAzureResource<?>, IView.Label> openPortalUrlView = s -> {
            final String description = AzureOperationBundle.title("common|resource.open_portal_url", s.name()).toString();
            return new IView.Label.Static("Open In Portal", "/icons/action/portal.svg", description);
        };
        am.registerAction(OPEN_PORTAL_URL, new Action<>(openPortalUrl, openPortalUrlView));
    }

    private void registerCommand(AzureActionManager am) {
        am.registerAction(OPEN_URL, Action.emptyHandler());

        final Function<IAzureResource<?>, IView.Label> showPropertiesView = s -> {
            final String description = AzureOperationBundle.title("common|action.show_properties.resource", s.name()).toString();
            return new IView.Label.Static("Show Properties", "/icons/action/properties.svg", description);
        };
        am.registerAction(SHOW_PROPERTIES, new Action<>(Action.emptyHandler(), showPropertiesView));

        final Function<IAzureResource<?>, IView.Label> deployView = s -> {
            final String description = AzureOperationBundle.title("common|action.deploy.resource", s.name()).toString();
            return new IView.Label.Static("Deploy", "/icons/action/deploy.svg", description);
        };
        am.registerAction(DEPLOY, new Action<>(Action.emptyHandler(), deployView));

        final Function<IAzureResource<?>, IView.Label> createView = s -> {
            final String description = AzureOperationBundle.title("common|action.create.resource", s.name()).toString();
            return new IView.Label.Static("Create", "/icons/action/create.svg", description);
        };
        am.registerAction(CREATE, new Action<>(Action.emptyHandler(), createView));
    }
}
