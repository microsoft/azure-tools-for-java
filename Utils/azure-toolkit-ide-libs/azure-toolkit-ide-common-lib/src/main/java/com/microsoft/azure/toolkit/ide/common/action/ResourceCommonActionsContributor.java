/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.action;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.favorite.Favorites;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceModule;
import com.microsoft.azure.toolkit.lib.common.model.Deletable;
import com.microsoft.azure.toolkit.lib.common.model.Refreshable;
import com.microsoft.azure.toolkit.lib.common.model.Startable;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public class ResourceCommonActionsContributor implements IActionsContributor {

    public static final int INITIALIZE_ORDER = 0;

    public static final Action.Id<AzResource> START = Action.Id.of("user/resource.start_resource.resource");
    public static final Action.Id<AzResource> STOP = Action.Id.of("user/resource.stop_resource.resource");
    public static final Action.Id<AzResource> RESTART = Action.Id.of("user/resource.restart_resource.resource");
    public static final Action.Id<Refreshable> REFRESH = Action.Id.of("user/resource.refresh_resource.resource");
    public static final Action.Id<AzResource> DELETE = Action.Id.of("user/resource.delete_resource.resource");
    public static final Action.Id<AzResource> OPEN_PORTAL_URL = Action.Id.of("user/resource.open_portal_url.resource");
    public static final Action.Id<AzResourceBase> SHOW_PROPERTIES = Action.Id.of("user/resource.show_properties.resource");
    public static final Action.Id<AzResource> DEPLOY = Action.Id.of("user/resource.deploy_resource.resource");
    public static final Action.Id<AzResource> CONNECT = Action.Id.of("user/resource.connect_resource.resource");
    public static final Action.Id<Object> CREATE = Action.Id.of("user/resource.create_resource.type");
    public static final Action.Id<AzService> CREATE_IN_PORTAL = Action.Id.of("user/resource.create_resource_in_portal.type");
    public static final Action.Id<AbstractAzResource<?, ?, ?>> PIN = Action.Id.of("user/resource.pin");
    public static final Action.Id<String> OPEN_URL = Action.Id.of("user/common.open_url.url");
    public static final Action.Id<String> COPY_STRING = Action.Id.of("user/common.copy_string");
    public static final Action.Id<Object> OPEN_AZURE_SETTINGS = Action.Id.of("user/common.open_azure_settings");
    public static final Action.Id<Object> OPEN_AZURE_EXPLORER = Action.Id.of("user/common.open_azure_explorer");
    public static final Action.Id<Object> OPEN_AZURE_REFERENCE_BOOK = Action.Id.of("user/common.open_azure_reference_book");
    public static final Action.Id<Object> HIGHLIGHT_RESOURCE_IN_EXPLORER = Action.Id.of("internal/common.highlight_resource_in_explorer");
    public static final Action.Id<Object> INSTALL_DOTNET_RUNTIME = Action.Id.of("user/bicep.install_dotnet_runtime");
    public static final Action.Id<Object> RESTART_IDE = Action.Id.of("user/common.restart_ide");
    public static final Action.Id<File> REVEAL_FILE = Action.Id.of("user/common.reveal_file_in_explorer");
    public static final Action.Id<File> OPEN_FILE = Action.Id.of("user/common.open_file_in_editor");

    public static final String RESOURCE_GROUP_CREATE_ACTIONS = "actions.resource.create.group";

    @Override
    public void registerActions(AzureActionManager am) {
        final AzureActionManager.Shortcuts shortcuts = am.getIDEDefaultShortcuts();
        new Action<>(START)
            .withLabel("Start")
            .withIcon(AzureIcons.Action.START.getIconPath())
            .withIdParam(AzResource::getName)
            .withShortcut(shortcuts.start())
            .visibleWhen(s -> s instanceof AzResource && s instanceof Startable && ((Startable) s).isStartable())
            .withHandler(s -> ((Startable) s).start())
            .register(am);

        new Action<>(STOP)
            .withLabel("Stop")
            .withIcon(AzureIcons.Action.STOP.getIconPath())
            .withIdParam(AzResource::getName)
            .withShortcut(shortcuts.stop())
            .visibleWhen(s -> s instanceof AzResource && s instanceof Startable && ((Startable) s).isStoppable())
            .withHandler(s -> ((Startable) s).stop())
            .register(am);

        new Action<>(RESTART)
            .withLabel("Restart")
            .withIcon(AzureIcons.Action.RESTART.getIconPath())
            .withIdParam(AzResource::getName)
            .withShortcut(shortcuts.restart())
            .visibleWhen(s -> s instanceof AzResource && s instanceof Startable && ((Startable) s).isRestartable())
            .withHandler(s -> ((Startable) s).restart())
            .register(am);

        new Action<>(DELETE)
            .withLabel("Delete")
            .withIcon(AzureIcons.Action.DELETE.getIconPath())
            .withIdParam(AzResource::getName)
            .withShortcut(shortcuts.delete())
            .visibleWhen(s -> s instanceof AzResource && s instanceof Deletable)
            .enableWhen(s -> {
                if (s instanceof AbstractAzResource) {
                    final AbstractAzResource<?, ?, ?> r = (AbstractAzResource<?, ?, ?>) s;
                    return !r.getFormalStatus().isDeleted() && !r.isDraftForCreating();
                }
                return true;
            })
            .withHandler((s) -> {
                if (AzureMessager.getMessager().confirm(String.format("Are you sure to delete %s \"%s\"", s.getResourceTypeName(), s.getName()))) {
                    ((Deletable) s).delete();
                }
            }).register(am);

        new Action<>(REFRESH)
            .withLabel("Refresh")
            .withIcon(AzureIcons.Action.REFRESH.getIconPath())
            .withIdParam(s -> Optional.ofNullable(s).map(r -> {
                if (r instanceof AzResource) {
                    return ((AzResource) r).getName();
                } else if (r instanceof AbstractAzResourceModule) {
                    return ((AbstractAzResourceModule<?, ?, ?>) r).getResourceTypeName();
                }
                throw new IllegalArgumentException("Unsupported type: " + r.getClass());
            }).orElse(null))
            .withShortcut(shortcuts.refresh())
            .visibleWhen(s -> s instanceof Refreshable)
            .withHandler(Refreshable::refresh)
            .register(am);

        new Action<>(OPEN_PORTAL_URL)
            .withLabel("Open in Portal")
            .withIcon(AzureIcons.Action.PORTAL.getIconPath())
            .withIdParam(AzResource::getName)
            .withShortcut("control alt O")
            .visibleWhen(s -> s instanceof AzResource)
            .withHandler(s -> am.getAction(OPEN_URL).handle(s.getPortalUrl()))
            .register(am);

        new Action<>(OPEN_URL)
            .withLabel("Open Url")
            .withIdParam(u -> u)
            .withAuthRequired(false)
            .register(am);

        new Action<>(COPY_STRING)
            .withLabel("Copy")
            .withIdParam(u -> u)
            .withAuthRequired(false)
            .register(am);

        new Action<>(CONNECT)
            .withLabel("Connect to Project")
            .withIcon(AzureIcons.Connector.CONNECT.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof AzResourceBase)
            .enableWhen(s -> s.getFormalStatus().isRunning())
            .register(am);

        new Action<>(SHOW_PROPERTIES)
            .withLabel("Show Properties")
            .withIcon(AzureIcons.Action.PROPERTIES.getIconPath())
            .withIdParam(AzResourceBase::getName)
            .visibleWhen(s -> s instanceof AzResourceBase)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .withShortcut(shortcuts.edit())
            .register(am);

        new Action<>(DEPLOY)
            .withLabel("Deploy")
            .withIcon(AzureIcons.Action.DEPLOY.getIconPath())
            .withIdParam(AzResource::getName)
            .withShortcut("control alt O")
            .visibleWhen(s -> s instanceof AzResourceBase)
            .enableWhen(s -> s.getFormalStatus().isWritable())
            .withShortcut(shortcuts.deploy())
            .register(am);

        new Action<>(OPEN_AZURE_SETTINGS)
            .withLabel("Open Azure Settings")
            .withAuthRequired(false)
            .register(am);

        new Action<>(OPEN_AZURE_EXPLORER)
            .withLabel("Open Azure Explorer")
            .withAuthRequired(false)
            .register(am);

        new Action<>(HIGHLIGHT_RESOURCE_IN_EXPLORER)
            .withLabel("Highlight resource in Azure Explorer")
            .withAuthRequired(false)
            .register(am);

        new Action<>(OPEN_AZURE_REFERENCE_BOOK)
            .withLabel("View Azure SDK")
            .withAuthRequired(false)
            .register(am);

        new Action<>(CREATE)
            .withLabel("Create")
            .withIcon(AzureIcons.Action.CREATE.getIconPath())
            .withIdParam(r -> {
                if (r instanceof AzResource) {
                    return ((AzResource) r).getName();
                } else if (r instanceof AzService) {
                    return ((AzService) r).getName();
                } else if (r instanceof AzResourceModule) {
                    return ((AzResourceModule<?>) r).getResourceTypeName();
                }
                return r.getClass().getSimpleName();
            })
            .withShortcut(shortcuts.add())
            .visibleWhen(s -> s instanceof AzService || s instanceof AzResourceModule || s instanceof AzResource)
            .enableWhen(s -> !(s instanceof AzResource) || !StringUtils.equalsIgnoreCase(((AzResourceBase) s).getStatus(), AzResource.Status.CREATING))
            .register(am);

        new Action<>(CREATE_IN_PORTAL)
            .withLabel("Create In Azure Portal")
            .withIcon(AzureIcons.Action.CREATE.getIconPath())
            .visibleWhen(s -> s instanceof AzService)
            .withHandler(s -> {
                final IAccount account = Azure.az(IAzureAccount.class).account();
                final String url = String.format("%s/#create/%s", account.getPortalUrl(), s.getName());
                am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(url);
            })
            .withShortcut(shortcuts.add())
            .register(am);

        final Favorites favorites = Favorites.getInstance();
        new Action<>(PIN)
            .withLabel(s -> Objects.nonNull(s) && favorites.exists(s.getId()) ? "Unmark As Favorite" : "Mark As Favorite")
            .withIcon(s -> Objects.nonNull(s) && favorites.exists(s.getId()) ? AzureIcons.Action.PIN.getIconPath() : AzureIcons.Action.UNPIN.getIconPath())
            .withShortcut("F11")
            .visibleWhen(s -> s instanceof AbstractAzResource)
            .withHandler((r) -> {
                if (favorites.exists(r.getId())) {
                    favorites.unpin(r.getId());
                } else {
                    favorites.pin(r);
                }
            })
            .withAuthRequired(false)
            .register(am);

        new Action<>(INSTALL_DOTNET_RUNTIME)
            .withLabel("Install .Net Runtime")
            .withAuthRequired(false)
            .register(am);

        new Action<>(RESTART_IDE)
            .withLabel("Restart IDE")
            .withAuthRequired(false)
            .register(am);
    }

    @AzureOperation(name = "boundary/common.copy_string.string", params = {"s"})
    private static void copyString(String s) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(s), null);
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
