/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.action;

import com.microsoft.azure.toolkit.ide.common.favorite.FavoriteDraft;
import com.microsoft.azure.toolkit.ide.common.favorite.Favorites;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.common.entity.Removable;
import com.microsoft.azure.toolkit.lib.common.entity.Startable;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle.title;

public class ResourceCommonActionsContributor implements IActionsContributor {

    public static final int INITIALIZE_ORDER = 0;

    public static final Action.Id<IAzureBaseResource<?, ?>> START = Action.Id.of("action.resource.start");
    public static final Action.Id<IAzureBaseResource<?, ?>> STOP = Action.Id.of("action.resource.stop");
    public static final Action.Id<IAzureBaseResource<?, ?>> RESTART = Action.Id.of("action.resource.restart");
    public static final Action.Id<IAzureBaseResource<?, ?>> REFRESH = Action.Id.of("action.resource.refresh");
    public static final Action.Id<IAzureBaseResource<?, ?>> DELETE = Action.Id.of("action.resource.delete");
    public static final Action.Id<IAzureBaseResource<?, ?>> OPEN_PORTAL_URL = Action.Id.of("action.resource.open_portal_url");
    public static final Action.Id<AzResourceBase> SHOW_PROPERTIES = Action.Id.of("action.resource.show_properties");
    public static final Action.Id<IAzureBaseResource<?, ?>> DEPLOY = Action.Id.of("action.resource.deploy");
    public static final Action.Id<IAzureBaseResource<?, ?>> CONNECT = Action.Id.of("action.resource.connect");
    public static final Action.Id<Object> CREATE = Action.Id.of("action.resource.create");
    public static final Action.Id<AbstractAzResource<?, ?, ?>> PIN = Action.Id.of("action.resource.pin");
    public static final Action.Id<AzService> SERVICE_REFRESH = Action.Id.of("action.service.refresh");
    public static final Action.Id<String> OPEN_URL = Action.Id.of("action.open_url");
    public static final Action.Id<Object> OPEN_AZURE_SETTINGS = Action.Id.of("action.open_azure_settings");

    @Override
    public void registerActions(AzureActionManager am) {
        final AzureActionManager.Shortcuts shortcuts = am.getIDEDefaultShortcuts();
        final ActionView.Builder startView = new ActionView.Builder("Start", "/icons/action/start.svg")
            .title(s -> Optional.ofNullable(s).map(r -> title("resource.start_resource.resource", ((AzResourceBase) r).getName())).orElse(null))
            .enabled(s -> s instanceof IAzureBaseResource);
        final Action<IAzureBaseResource<?, ?>> startAction = new Action<>(startView);
        startAction.setShortcuts(shortcuts.start());
        startAction.registerHandler((s) -> s instanceof Startable && ((Startable) s).isStartable(), s -> ((Startable) s).start());
        am.registerAction(START, startAction);

        final ActionView.Builder stopView = new ActionView.Builder("Stop", "/icons/action/stop.svg")
            .title(s -> Optional.ofNullable(s).map(r -> title("resource.stop_resource.resource", ((IAzureBaseResource<?, ?>) r).name())).orElse(null))
            .enabled(s -> s instanceof IAzureBaseResource);
        final Action<IAzureBaseResource<?, ?>> stopAction = new Action<>(stopView);
        stopAction.setShortcuts(shortcuts.stop());
        stopAction.registerHandler((s) -> s instanceof Startable && ((Startable) s).isStoppable(), s -> ((Startable) s).stop());
        am.registerAction(STOP, stopAction);

        final ActionView.Builder restartView = new ActionView.Builder("Restart", "/icons/action/restart.svg")
            .title(s -> Optional.ofNullable(s).map(r -> title("resource.restart_resource.resource", ((IAzureBaseResource<?, ?>) r).name())).orElse(null))
            .enabled(s -> s instanceof IAzureBaseResource);
        final Action<IAzureBaseResource<?, ?>> restartAction = new Action<>(restartView);
        restartAction.setShortcuts(shortcuts.restart());
        restartAction.registerHandler((s) -> s instanceof Startable && ((Startable) s).isRestartable(), s -> ((Startable) s).restart());
        am.registerAction(RESTART, restartAction);

        final Consumer<IAzureBaseResource<?, ?>> delete = s -> {
            if (AzureMessager.getMessager().confirm(String.format("Are you sure to delete \"%s\"", s.getName()))) {
                ((Removable) s).remove();
            }
        };
        final ActionView.Builder deleteView = new ActionView.Builder("Delete", "/icons/action/delete.svg")
            .title(s -> Optional.ofNullable(s).map(r -> title("resource.delete_resource.resource", ((IAzureBaseResource<?, ?>) r).name())).orElse(null))
            .enabled(s -> s instanceof Removable && !((AzResourceBase) s).getFormalStatus().isWriting());
        final Action<IAzureBaseResource<?, ?>> deleteAction = new Action<>(delete, deleteView);
        deleteAction.setShortcuts(shortcuts.delete());
        am.registerAction(DELETE, deleteAction);

        final Consumer<IAzureBaseResource<?, ?>> refresh = IAzureBaseResource::refresh;
        final ActionView.Builder refreshView = new ActionView.Builder("Refresh", "/icons/action/refresh.svg")
            .title(s -> Optional.ofNullable(s).map(r -> title("resource.refresh.resource", ((IAzureBaseResource<?, ?>) r).name())).orElse(null))
            .enabled(s -> s instanceof IAzureBaseResource);
        final Action<IAzureBaseResource<?, ?>> refreshAction = new Action<>(refresh, refreshView);
        refreshAction.setShortcuts(shortcuts.refresh());
        am.registerAction(REFRESH, refreshAction);

        final Consumer<AzService> serviceRefresh = AzService::refresh;
        final ActionView.Builder serviceRefreshView = new ActionView.Builder("Refresh", "/icons/action/refresh.svg")
            .title(s -> Optional.ofNullable(s).map(r -> title("service.refresh.service", ((AzService) r).getName())).orElse(null))
            .enabled(s -> s instanceof AzService);
        final Action<AzService> serviceRefreshAction = new Action<>(serviceRefresh, serviceRefreshView);
        serviceRefreshAction.setShortcuts(shortcuts.refresh());
        am.registerAction(SERVICE_REFRESH, serviceRefreshAction);

        final Consumer<IAzureBaseResource<?, ?>> openPortalUrl = s -> am.getAction(OPEN_URL).handle(s.portalUrl());
        final ActionView.Builder openPortalUrlView = new ActionView.Builder("Open in Portal", "/icons/action/portal.svg")
            .title(s -> Optional.ofNullable(s).map(r -> title("resource.open_portal_url.resource", ((IAzureBaseResource<?, ?>) r).name())).orElse(null))
            .enabled(s -> s instanceof IAzureBaseResource);
        final Action<IAzureBaseResource<?, ?>> openPortalUrlAction = new Action<>(openPortalUrl, openPortalUrlView);
        openPortalUrlAction.setShortcuts("control alt O");
        am.registerAction(OPEN_PORTAL_URL, openPortalUrlAction);

        // register commands
        final Action<String> action = new Action<>((s) -> {
            throw new AzureToolkitRuntimeException(String.format("no matched handler for action %s.", s));
        });
        action.setAuthRequired(false);
        am.registerAction(OPEN_URL, action);

        final ActionView.Builder connectView = new ActionView.Builder("Connect to Project", "/icons/connector/connect.svg")
            .title(s -> Optional.ofNullable(s).map(r -> title("resource.connect_resource.resource", ((IAzureBaseResource<?, ?>) r).name())).orElse(null))
            .enabled(s -> s instanceof AzResourceBase && ((AzResourceBase) s).getFormalStatus().isRunning());
        am.registerAction(CONNECT, new Action<>(connectView));

        final ActionView.Builder showPropertiesView = new ActionView.Builder("Show Properties", "/icons/action/properties.svg")
            .title(s -> Optional.ofNullable(s).map(r -> title("resource.show_properties.resource", ((IAzureBaseResource<?, ?>) r).name())).orElse(null))
            .enabled(s -> s instanceof AzResourceBase && !StringUtils.equalsIgnoreCase(((AzResourceBase) s).getStatus(), IAzureBaseResource.Status.CREATING));
        final Action<AzResourceBase> showPropertiesAction = new Action<>(showPropertiesView);
        showPropertiesAction.setShortcuts(shortcuts.edit());
        am.registerAction(SHOW_PROPERTIES, showPropertiesAction);

        final ActionView.Builder deployView = new ActionView.Builder("Deploy", "/icons/action/deploy.svg")
            .title(s -> Optional.ofNullable(s).map(r -> title("resource.deploy_resource.resource", ((IAzureBaseResource<?, ?>) r).name())).orElse(null))
            .enabled(s -> s instanceof AzResourceBase && ((AzResourceBase) s).getFormalStatus().isRunning());
        final Action<IAzureBaseResource<?, ?>> deployAction = new Action<>(deployView);
        deployAction.setShortcuts(shortcuts.deploy());
        am.registerAction(DEPLOY, deployAction);

        final ActionView.Builder openSettingsView = new ActionView.Builder("Open Azure Settings")
            .title((s) -> AzureOperationBundle.title("common.open_azure_settings"));
        am.registerAction(OPEN_AZURE_SETTINGS, new Action<>(openSettingsView).setAuthRequired(false));

        final ActionView.Builder createView = new ActionView.Builder("Create", "/icons/action/create.svg")
            .title(s -> Optional.ofNullable(s).map(r -> {
                String name = r.getClass().getSimpleName();
                if (r instanceof IAzureBaseResource) {
                    name = ((IAzureBaseResource<?, ?>) r).name();
                } else if (r instanceof AzService) {
                    name = ((AzService) r).getName();
                }
                return title("resource.create_resource.service", name);
            }).orElse(null)).enabled(s -> s instanceof AzService ||
                (s instanceof IAzureBaseResource && !StringUtils.equalsIgnoreCase(((AzResourceBase) s).getStatus(), IAzureBaseResource.Status.CREATING)));
        final Action<Object> createAction = new Action<>(createView);
        createAction.setShortcuts(shortcuts.add());
        am.registerAction(CREATE, createAction);

        final Favorites favorites = Favorites.getInstance();
        final Function<Object, String> title = s -> favorites.exists(((AbstractAzResource<?, ?, ?>) s).getId(), null) ?
            "Unmark As Favorite" : "Mark As Favorite";
        final ActionView.Builder pinView = new ActionView.Builder(title).enabled(s -> s instanceof AbstractAzResource);
        pinView.iconPath(s -> favorites.exists(((AbstractAzResource<?, ?, ?>) s).getId(), null) ?
            "/icons/Common/pin.svg" : "/icons/Common/unpin.svg");
        final Action<AbstractAzResource<?, ?, ?>> pinAction = new Action<>((r) -> {
            if (favorites.exists(r.getId(), null)) {
                favorites.delete(r.getId(), null);
            } else {
                final FavoriteDraft draft = favorites.create(r.getId(), null);
                draft.setResource(r);
                draft.commit();
            }
        }, pinView);
        am.registerAction(PIN, pinAction);
    }

    public int getOrder() {
        return INITIALIZE_ORDER; //after azure resource common actions registered
    }
}
