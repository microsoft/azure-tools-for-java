/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.action;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.favorite.Favorites;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Refreshable;
import com.microsoft.azure.toolkit.lib.common.model.Startable;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.microsoft.azure.toolkit.lib.common.operation.OperationBundle.description;

public class ResourceCommonActionsContributor implements IActionsContributor {

    public static final int INITIALIZE_ORDER = 0;

    public static final Action.Id<AzResource<?, ?, ?>> START = Action.Id.of("resource.start");
    public static final Action.Id<AzResource<?, ?, ?>> STOP = Action.Id.of("resource.stop");
    public static final Action.Id<AzResource<?, ?, ?>> RESTART = Action.Id.of("resource.restart");
    public static final Action.Id<Refreshable> REFRESH = Action.Id.of("resource.refresh");
    public static final Action.Id<AzResource<?, ?, ?>> DELETE = Action.Id.of("resource.delete");
    public static final Action.Id<AzResource<?, ?, ?>> OPEN_PORTAL_URL = Action.Id.of("resource.open_portal_url");
    public static final Action.Id<AzResourceBase> SHOW_PROPERTIES = Action.Id.of("resource.show_properties");
    public static final Action.Id<AzResource<?, ?, ?>> DEPLOY = Action.Id.of("resource.deploy");
    public static final Action.Id<AzResource<?, ?, ?>> CONNECT = Action.Id.of("resource.connect");
    public static final Action.Id<Object> CREATE = Action.Id.of("resource.create");
    public static final Action.Id<AbstractAzResource<?, ?, ?>> PIN = Action.Id.of("resource.pin");
    public static final Action.Id<String> OPEN_URL = Action.Id.of("common.open_url");
    public static final Action.Id<Object> OPEN_AZURE_SETTINGS = Action.Id.of("common.open_azure_settings");
    public static final Action.Id<Object> OPEN_AZURE_EXPLORER = Action.Id.of("common.open_azure_explorer");
    public static final Action.Id<Object> OPEN_AZURE_REFERENCE_BOOK = Action.Id.of("common.open_azure_reference_book");
    public static final Action.Id<Object> HIGHLIGHT_RESOURCE_IN_EXPLORER = Action.Id.of("common.highlight_resource_in_explorer");

    public static final String RESOURCE_GROUP_CREATE_ACTIONS = "actions.resource.create.group";

    @Override
    public void registerActions(AzureActionManager am) {
        final AzureActionManager.Shortcuts shortcuts = am.getIDEDefaultShortcuts();
        final ActionView.Builder startView = new ActionView.Builder("Start", AzureIcons.Action.START.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> description("resource.start_resource.resource", ((AzResourceBase) r).getName())).orElse(null))
            .enabled(s -> s instanceof AzResource);
        final Action<AzResource<?, ?, ?>> startAction = new Action<>(START, startView);
        startAction.setShortcuts(shortcuts.start());
        startAction.registerHandler((s) -> s instanceof Startable && ((Startable) s).isStartable(), s -> ((Startable) s).start());
        am.registerAction(START, startAction);

        final ActionView.Builder stopView = new ActionView.Builder("Stop", AzureIcons.Action.STOP.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> description("resource.stop_resource.resource", ((AzResource<?, ?, ?>) r).name())).orElse(null))
            .enabled(s -> s instanceof AzResource);
        final Action<AzResource<?, ?, ?>> stopAction = new Action<>(STOP, stopView);
        stopAction.setShortcuts(shortcuts.stop());
        stopAction.registerHandler((s) -> s instanceof Startable && ((Startable) s).isStoppable(), s -> ((Startable) s).stop());
        am.registerAction(STOP, stopAction);

        final ActionView.Builder restartView = new ActionView.Builder("Restart", AzureIcons.Action.RESTART.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> description("resource.restart_resource.resource", ((AzResource<?, ?, ?>) r).name())).orElse(null))
            .enabled(s -> s instanceof AzResource);
        final Action<AzResource<?, ?, ?>> restartAction = new Action<>(RESTART, restartView);
        restartAction.setShortcuts(shortcuts.restart());
        restartAction.registerHandler((s) -> s instanceof Startable && ((Startable) s).isRestartable(), s -> ((Startable) s).restart());
        am.registerAction(RESTART, restartAction);

        final Consumer<AzResource<?, ?, ?>> delete = s -> {
            if (AzureMessager.getMessager().confirm(String.format("Are you sure to delete %s \"%s\"", s.getResourceTypeName(), s.getName()))) {
                ((Deletable) s).delete();
            }
        };
        final ActionView.Builder deleteView = new ActionView.Builder("Delete", AzureIcons.Action.DELETE.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> description("resource.delete_resource.resource", ((AzResource<?, ?, ?>) r).name())).orElse(null))
            .enabled(s -> s instanceof Deletable && !((AzResourceBase) s).getFormalStatus().isDeleted() &&
                s instanceof AbstractAzResource && !((AbstractAzResource<?, ?, ?>) s).isDraftForCreating());
        final Action<AzResource<?, ?, ?>> deleteAction = new Action<>(DELETE, delete, deleteView);
        deleteAction.setShortcuts(shortcuts.delete());
        am.registerAction(DELETE, deleteAction);

        final Consumer<Refreshable> refresh = Refreshable::refresh;
        final ActionView.Builder refreshView = new ActionView.Builder("Refresh", AzureIcons.Action.REFRESH.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> {
                if (r instanceof AzResource) {
                    return description("resource.refresh.resource", ((AzResource<?, ?, ?>) r).name());
                } else if (r instanceof AbstractAzResourceModule) {
                    return description("resource.refresh.resource", ((AbstractAzResourceModule<?, ?, ?>) r).getResourceTypeName());
                } else {
                    return AzureString.fromString("refresh");
                }
            }).orElse(null))
            .enabled(s -> s instanceof Refreshable);
        final Action<Refreshable> refreshAction = new Action<>(REFRESH, refresh, refreshView);
        refreshAction.setShortcuts(shortcuts.refresh());
        am.registerAction(REFRESH, refreshAction);

        final Consumer<AzResource<?, ?, ?>> openPortalUrl = s -> am.getAction(OPEN_URL).handle(s.getPortalUrl());
        final ActionView.Builder openPortalUrlView = new ActionView.Builder("Open in Portal", AzureIcons.Action.PORTAL.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> description("resource.open_portal_url.resource", ((AzResource<?, ?, ?>) r).name())).orElse(null))
            .enabled(s -> s instanceof AzResource);
        final Action<AzResource<?, ?, ?>> openPortalUrlAction = new Action<>(OPEN_PORTAL_URL, openPortalUrl, openPortalUrlView);
        openPortalUrlAction.setShortcuts("control alt O");
        am.registerAction(OPEN_PORTAL_URL, openPortalUrlAction);

        // register commands
        final Action<String> action = new Action<>(OPEN_URL, (s) -> {
            throw new AzureToolkitRuntimeException(String.format("no matched handler for action %s.", s));
        });
        action.setAuthRequired(false);
        am.registerAction(OPEN_URL, action);

        final ActionView.Builder connectView = new ActionView.Builder("Connect to Project", AzureIcons.Connector.CONNECT.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> description("resource.connect_resource.resource", ((AzResource<?, ?, ?>) r).name())).orElse(null))
            .enabled(s -> s instanceof AzResourceBase && ((AzResourceBase) s).getFormalStatus().isRunning());
        am.registerAction(CONNECT, new Action<>(CONNECT, connectView));

        final ActionView.Builder showPropertiesView = new ActionView.Builder("Show Properties", AzureIcons.Action.PROPERTIES.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> description("resource.show_properties.resource", ((AzResource<?, ?, ?>) r).name())).orElse(null))
            .enabled(s -> s instanceof AzResourceBase && ((AzResourceBase) s).getFormalStatus().isConnected());
        final Action<AzResourceBase> showPropertiesAction = new Action<>(SHOW_PROPERTIES, showPropertiesView);
        showPropertiesAction.setShortcuts(shortcuts.edit());
        am.registerAction(SHOW_PROPERTIES, showPropertiesAction);

        final ActionView.Builder deployView = new ActionView.Builder("Deploy", AzureIcons.Action.DEPLOY.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> description("resource.deploy_resource.resource", ((AzResource<?, ?, ?>) r).name())).orElse(null))
            .enabled(s -> s instanceof AzResourceBase && ((AzResourceBase) s).getFormalStatus().isWritable());
        final Action<AzResource<?, ?, ?>> deployAction = new Action<>(DEPLOY, deployView);
        deployAction.setShortcuts(shortcuts.deploy());
        am.registerAction(DEPLOY, deployAction);

        final ActionView.Builder openSettingsView = new ActionView.Builder("Open Azure Settings")
            .title((s) -> OperationBundle.description("common.open_azure_settings"));
        am.registerAction(OPEN_AZURE_SETTINGS, new Action<>(OPEN_AZURE_SETTINGS, openSettingsView).setAuthRequired(false));

        final ActionView.Builder openAzureExplorer = new ActionView.Builder("Open Azure Explorer")
            .title((s) -> OperationBundle.description("common.open_azure_explorer"));
        am.registerAction(OPEN_AZURE_EXPLORER, new Action<>(OPEN_AZURE_EXPLORER, openAzureExplorer).setAuthRequired(false));

        final ActionView.Builder highlightResourceView = new ActionView.Builder("highlight resource in Azure Explorer")
            .title((s) -> OperationBundle.description("common.highlight_resource_in_explorer"));
        am.registerAction(HIGHLIGHT_RESOURCE_IN_EXPLORER, new Action<>(HIGHLIGHT_RESOURCE_IN_EXPLORER, highlightResourceView).setAuthRequired(false));

        final ActionView.Builder openReferenceBookView = new ActionView.Builder("Open in Azure SDK Reference Book")
                .title((s) -> OperationBundle.description("common.open_azure_reference_book"));
        am.registerAction(OPEN_AZURE_REFERENCE_BOOK, new Action<>(OPEN_AZURE_REFERENCE_BOOK, openReferenceBookView));

        final ActionView.Builder createView = new ActionView.Builder("Create", AzureIcons.Action.CREATE.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> {
                String name = r.getClass().getSimpleName();
                if (r instanceof AzResource) {
                    name = ((AzResource<?, ?, ?>) r).getName();
                } else if (r instanceof AzService) {
                    name = ((AzService) r).getName();
                } else if (r instanceof AzResourceModule) {
                    name = ((AzResourceModule<?, ?, ?>) r).getResourceTypeName();
                }
                return description("resource.create_resource.type", name);
            }).orElse(null)).enabled(s -> s instanceof AzService || s instanceof AzResourceModule ||
                (s instanceof AzResource && !StringUtils.equalsIgnoreCase(((AzResourceBase) s).getStatus(), AzResource.Status.CREATING)));
        final Action<Object> createAction = new Action<>(CREATE, createView);
        createAction.setShortcuts(shortcuts.add());
        am.registerAction(CREATE, createAction);

        final Favorites favorites = Favorites.getInstance();
        final Function<Object, String> title = s -> Objects.nonNull(s) && favorites.exists(((AbstractAzResource<?, ?, ?>) s).getId()) ?
            "Unmark As Favorite" : "Mark As Favorite";
        final ActionView.Builder pinView = new ActionView.Builder(title).enabled(s -> s instanceof AbstractAzResource);
        pinView.iconPath(s -> Objects.nonNull(s) && favorites.exists(((AbstractAzResource<?, ?, ?>) s).getId()) ?
            AzureIcons.Action.PIN.getIconPath() : AzureIcons.Action.UNPIN.getIconPath());
        final Action<AbstractAzResource<?, ?, ?>> pinAction = new Action<>(ResourceCommonActionsContributor.PIN, (r) -> {
            if (favorites.exists(r.getId())) {
                favorites.unpin(r.getId());
            } else {
                favorites.pin(r);
            }
        }, pinView);
        pinAction.setShortcuts("F11");
        am.registerAction(ResourceCommonActionsContributor.PIN, pinAction);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final IView.Label.Static view = new IView.Label.Static("Create", "/icons/action/create.svg");
        final ActionGroup resourceGroupCreateActions = new ActionGroup(new ArrayList<>(), view);
        am.registerGroup(RESOURCE_GROUP_CREATE_ACTIONS, resourceGroupCreateActions);
    }

    public int getOrder() {
        return INITIALIZE_ORDER; //after azure resource common actions registered
    }
}
