/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.action;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.entity.Removable;
import com.microsoft.azure.toolkit.lib.common.entity.Startable;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;

import java.util.Optional;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle.title;

public class ResourceCommonActionsContributor implements IActionsContributor {

    public static final int INITIALIZE_ORDER = 0;

    public static final Action.Id<IAzureBaseResource<?, ?>> START = Action.Id.of("action.resource.start");
    public static final Action.Id<IAzureBaseResource<?, ?>> STOP = Action.Id.of("action.resource.stop");
    public static final Action.Id<IAzureBaseResource<?, ?>> RESTART = Action.Id.of("action.resource.restart");
    public static final Action.Id<IAzureBaseResource<?, ?>> REFRESH = Action.Id.of("action.resource.refresh");
    public static final Action.Id<IAzureBaseResource<?, ?>> DELETE = Action.Id.of("action.resource.delete");
    public static final Action.Id<IAzureBaseResource<?, ?>> OPEN_PORTAL_URL = Action.Id.of("action.resource.open_portal_url");
    public static final Action.Id<IAzureBaseResource<?, ?>> SHOW_PROPERTIES = Action.Id.of("action.resource.show_properties");
    public static final Action.Id<IAzureBaseResource<?, ?>> DEPLOY = Action.Id.of("action.resource.deploy");
    public static final Action.Id<IAzureBaseResource<?, ?>> CONNECT = Action.Id.of("action.resource.connect");
    public static final Action.Id<Object> CREATE = Action.Id.of("action.resource.create");
    public static final Action.Id<AzService> SERVICE_REFRESH = Action.Id.of("action.service.refresh");
    public static final Action.Id<String> OPEN_URL = Action.Id.of("action.open_url");
    public static final Action.Id<Void> OPEN_AZURE_SETTINGS = Action.Id.of("action.open_azure_settings");

    @Override
    public void registerActions(AzureActionManager am) {
        final ActionView.Builder startView = new ActionView.Builder("Start", "/icons/action/start.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("resource.start_resource.resource", ((IAzureBaseResource<?, ?>) r).name())).orElse(null))
                .enabled(s -> s instanceof IAzureBaseResource);
        final Action<IAzureBaseResource<?, ?>> startAction = new Action<>(startView);
        startAction.registerHandler((s) -> s instanceof Startable && ((Startable) s).isStartable(), s -> ((Startable) s).start());
        am.registerAction(START, startAction);

        final ActionView.Builder stopView = new ActionView.Builder("Stop", "/icons/action/stop.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("resource.stop_resource.resource", ((IAzureBaseResource<?, ?>) r).name())).orElse(null))
                .enabled(s -> s instanceof IAzureBaseResource);
        final Action<IAzureBaseResource<?, ?>> stopAction = new Action<>(stopView);
        stopAction.registerHandler((s) -> s instanceof Startable && ((Startable) s).isStoppable(), s -> ((Startable) s).stop());
        am.registerAction(STOP, stopAction);

        final ActionView.Builder restartView = new ActionView.Builder("Restart", "/icons/action/restart.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("resource.restart_resource.resource", ((IAzureBaseResource<?, ?>) r).name())).orElse(null))
                .enabled(s -> s instanceof IAzureBaseResource);
        final Action<IAzureBaseResource<?, ?>> restartAction = new Action<>(restartView);
        restartAction.registerHandler((s) -> s instanceof Startable && ((Startable) s).isRestartable(), s -> ((Startable) s).restart());
        am.registerAction(RESTART, restartAction);

        final Consumer<IAzureBaseResource<?, ?>> delete = s -> ((Removable) s).remove();
        final ActionView.Builder deleteView = new ActionView.Builder("Delete", "/icons/action/delete.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("resource.delete_resource.resource", ((IAzureBaseResource<?, ?>) r).name())).orElse(null))
                .enabled(s -> s instanceof Removable);
        am.registerAction(DELETE, new Action<>(delete, deleteView));

        final Consumer<IAzureBaseResource<?, ?>> refresh = IAzureBaseResource::refresh;
        final ActionView.Builder refreshView = new ActionView.Builder("Refresh", "/icons/action/refresh.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("resource.refresh.resource", ((IAzureBaseResource<?, ?>) r).name())).orElse(null))
                .enabled(s -> s instanceof IAzureBaseResource);
        am.registerAction(REFRESH, new Action<>(refresh, refreshView));

        final Consumer<AzService> serviceRefresh = AzService::refresh;
        final ActionView.Builder serviceRefreshView = new ActionView.Builder("Refresh", "/icons/action/refresh.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("service.refresh.service", ((AzService) r).getName())).orElse(null))
                .enabled(s -> s instanceof AzService);
        am.registerAction(SERVICE_REFRESH, new Action<>(serviceRefresh, serviceRefreshView));

        final Consumer<IAzureBaseResource<?, ?>> openPortalUrl = s -> am.getAction(OPEN_URL).handle(s.portalUrl());
        final ActionView.Builder openPortalUrlView = new ActionView.Builder("Open in Portal", "/icons/action/portal.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("resource.open_portal_url.resource", ((IAzureBaseResource<?, ?>) r).name())).orElse(null))
                .enabled(s -> s instanceof IAzureBaseResource);
        am.registerAction(OPEN_PORTAL_URL, new Action<>(openPortalUrl, openPortalUrlView));

        // register commands
        final Action<String> action = new Action<>((s) -> {
            throw new AzureToolkitRuntimeException(String.format("no matched handler for action %s.", s));
        });
        action.authRequired(false);
        am.registerAction(OPEN_URL, action);

        final ActionView.Builder connectView = new ActionView.Builder("Connect to Project", "/icons/connector/connect.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("resource.connect_resource.resource", ((IAzureBaseResource<?, ?>) r).name()))
                .orElse(null));
        am.registerAction(CONNECT, new Action<>(connectView));

        final ActionView.Builder showPropertiesView = new ActionView.Builder("Show Properties", "/icons/action/properties.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("resource.show_properties.resource", ((IAzureBaseResource<?, ?>) r).name())).orElse(null));
        am.registerAction(SHOW_PROPERTIES, new Action<>(showPropertiesView));

        final ActionView.Builder deployView = new ActionView.Builder("Deploy", "/icons/action/deploy.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("resource.deploy_resource.resource", ((IAzureBaseResource<?, ?>) r).name())).orElse(null));
        am.registerAction(DEPLOY, new Action<>(deployView));

        final ActionView.Builder openSettingsView = new ActionView.Builder("Open Azure Settings")
                .title((s) -> AzureOperationBundle.title("common.open_azure_settings"));
        am.registerAction(OPEN_AZURE_SETTINGS, new Action<Void>(openSettingsView).authRequired(false));

        final ActionView.Builder createView = new ActionView.Builder("Create", "/icons/action/create.svg")
                .title(s -> Optional.ofNullable(s).map(r -> {
                    String name = r.getClass().getSimpleName();
                    if (r instanceof IAzureBaseResource) {
                        name = ((IAzureBaseResource<?, ?>) r).name();
                    } else if (r instanceof AzureService) {
                        name = ((AzureService<?>) r).name();
                    }
                    return title("resource.create_resource.service", name);
                }).orElse(null))
                .enabled(s -> s instanceof IAzureBaseResource || s instanceof AzureService);
        am.registerAction(CREATE, new Action<>(createView));
    }

    public int getOrder() {
        return INITIALIZE_ORDER; //after azure resource common actions registered
    }
}
