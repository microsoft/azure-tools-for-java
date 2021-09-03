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
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

import java.util.Optional;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle.title;

public class ResourceCommonActionsContributor implements IActionsContributor {

    public static final Action.Id<IAzureResource<?>> START = Action.Id.of("action.resource.start");
    public static final Action.Id<IAzureResource<?>> STOP = Action.Id.of("action.resource.stop");
    public static final Action.Id<IAzureResource<?>> RESTART = Action.Id.of("action.resource.restart");
    public static final Action.Id<IAzureResource<?>> REFRESH = Action.Id.of("action.resource.refresh");
    public static final Action.Id<IAzureResource<?>> DELETE = Action.Id.of("action.resource.delete");
    public static final Action.Id<IAzureResource<?>> OPEN_PORTAL_URL = Action.Id.of("action.resource.open_portal_url");
    public static final Action.Id<IAzureResource<?>> SHOW_PROPERTIES = Action.Id.of("action.resource.show_properties");
    public static final Action.Id<IAzureResource<?>> DEPLOY = Action.Id.of("action.resource.deploy");
    public static final Action.Id<Object> CREATE = Action.Id.of("action.resource.create");
    public static final Action.Id<AzureService> SERVICE_REFRESH = Action.Id.of("action.service.refresh");
    public static final Action.Id<String> OPEN_URL = Action.Id.of("action.open_url");

    @Override
    public void registerActions(AzureActionManager am) {
        final AzureTaskManager tm = AzureTaskManager.getInstance();

        final Consumer<IAzureResource<?>> start = s -> ((Startable<?>) s).start();
        final ActionView.Builder startView = new ActionView.Builder("Start", "/icons/action/start.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("common|resource.start", ((IAzureResource<?>) r).name())).orElse(null))
                .enabled(s -> s instanceof Startable && ((Startable<?>) s).isStartable());
        am.registerAction(START, new Action<>(start, startView));

        final Consumer<IAzureResource<?>> stop = s -> ((Startable<?>) s).stop();
        final ActionView.Builder stopView = new ActionView.Builder("Stop", "/icons/action/stop.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("common|resource.stop", ((IAzureResource<?>) r).name())).orElse(null))
                .enabled(s -> s instanceof Startable && ((Startable<?>) s).isStoppable());
        am.registerAction(STOP, new Action<>(stop, stopView));

        final Consumer<IAzureResource<?>> restart = s -> ((Startable<?>) s).restart();
        final ActionView.Builder restartView = new ActionView.Builder("Restart", "/icons/action/restart.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("common|resource.restart", ((IAzureResource<?>) r).name())).orElse(null))
                .enabled(s -> s instanceof Startable && ((Startable<?>) s).isRestartable());
        am.registerAction(RESTART, new Action<>(restart, restartView));

        final Consumer<IAzureResource<?>> delete = s -> ((Removable) s).remove();
        final ActionView.Builder deleteView = new ActionView.Builder("Delete", "/icons/action/delete.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("common|resource.delete", ((IAzureResource<?>) r).name())).orElse(null))
                .enabled(s -> s instanceof Removable);
        am.registerAction(DELETE, new Action<>(delete, deleteView));

        final Consumer<IAzureResource<?>> refresh = IAzureResource::refresh;
        final ActionView.Builder refreshView = new ActionView.Builder("Refresh", "/icons/action/refresh.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("common|resource.refresh", ((IAzureResource<?>) r).name())).orElse(null))
                .enabled(s -> s instanceof IAzureResource);
        am.registerAction(REFRESH, new Action<>(refresh, refreshView));

        final Consumer<AzureService> serviceRefresh = AzureService::refresh;
        final ActionView.Builder serviceRefreshView = new ActionView.Builder("Refresh", "/icons/action/refresh.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("common|service.refresh", ((AzureService) r).name())).orElse(null))
                .enabled(s -> s instanceof AzureService);
        am.registerAction(SERVICE_REFRESH, new Action<>(serviceRefresh, serviceRefreshView));

        final Consumer<IAzureResource<?>> openPortalUrl = s -> am.getAction(OPEN_URL).handle(s.portalUrl());
        final ActionView.Builder openPortalUrlView = new ActionView.Builder("Open In Portal", "/icons/action/portal.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("common|resource.open_portal_url", ((IAzureResource<?>) r).name())).orElse(null))
                .enabled(s -> s instanceof IAzureResource);
        am.registerAction(OPEN_PORTAL_URL, new Action<>(openPortalUrl, openPortalUrlView));

        // register commands
        am.registerAction(OPEN_URL, (s) -> {
            throw new AzureToolkitRuntimeException(String.format("no matched handler for action %s.", s));
        });

        final ActionView.Builder showPropertiesView = new ActionView.Builder("Show Properties", "/icons/action/properties.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("common|resource.show_properties", ((IAzureResource<?>) r).name())).orElse(null));
        am.registerAction(SHOW_PROPERTIES, new Action<>(showPropertiesView));

        final ActionView.Builder deployView = new ActionView.Builder("Deploy", "/icons/action/deploy.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("common|resource.deploy", ((IAzureResource<?>) r).name())).orElse(null));
        am.registerAction(DEPLOY, new Action<>(deployView));

        final ActionView.Builder createView = new ActionView.Builder("Create", "/icons/action/create.svg")
                .title(s -> Optional.ofNullable(s).map(r -> {
                    String name = r.getClass().getSimpleName();
                    if (r instanceof IAzureResource) {
                        name = ((IAzureResource<?>) r).name();
                    } else if (r instanceof AzureService) {
                        name = ((AzureService) r).name();
                    }
                    return title("common|resource.create", name);
                }).orElse(null))
                .enabled(s -> s instanceof IAzureResource || s instanceof AzureService);
        am.registerAction(CREATE, new Action<>(createView));
    }
}
