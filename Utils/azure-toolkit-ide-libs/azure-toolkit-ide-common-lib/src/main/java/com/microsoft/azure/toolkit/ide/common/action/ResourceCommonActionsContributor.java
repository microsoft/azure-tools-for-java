/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.action;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.favorite.Favorites;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.common.store.AzureConfigInitializer;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.*;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.StreamingLogSupport;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinker;
import com.microsoft.azure.toolkit.lib.servicelinker.ServiceLinkerModule;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.ide.common.icon.AzureIcons.Common.GET_START;
import static com.microsoft.azure.toolkit.ide.common.icon.AzureIcons.Common.GET_START_NEW;

public class ResourceCommonActionsContributor implements IActionsContributor {

    public static final int INITIALIZE_ORDER = 0;
    public static final String AZURE_EXPLORER = "azure.explorer";
    public static final String PROJECT_VIEW = "project.view";

    public static final Action.Id<AzResource> START = Action.Id.of("user/$resource.start_resource.resource");
    public static final Action.Id<AzResource> STOP = Action.Id.of("user/$resource.stop_resource.resource");
    public static final Action.Id<AzResource> RESTART = Action.Id.of("user/$resource.restart_resource.resource");
    public static final Action.Id<Refreshable> REFRESH = Action.Id.of("user/$resource.refresh_resource.resource");
    public static final Action.Id<AzResource> DELETE = Action.Id.of("user/$resource.delete_resource.resource");
    public static final Action.Id<AzResource> OPEN_PORTAL_URL = Action.Id.of("user/$resource.open_portal_url.resource");
    public static final Action.Id<AzResource> SHOW_PROPERTIES = Action.Id.of("user/$resource.show_properties.resource");
    public static final Action.Id<AzResource> DEPLOY = AzResource.DEPLOY;
    public static final Action.Id<AzResource> CONNECT = AzResource.CONNECT_RESOURCE;
    public static final Action.Id<Object> CREATE = AzResource.CREATE_RESOURCE;
    public static final Action.Id<AzService> CREATE_IN_PORTAL = Action.Id.of("user/$resource.create_resource_in_portal.type");
    public static final Action.Id<AbstractAzResource<?, ?, ?>> PIN = Action.Id.of("user/$resource.pin");
    public static final Action.Id<AbstractAzResource<?, ?, ?>> OPEN_IN_SERVICES_VIEW = Action.Id.of("user/$resource.open_in_services_view.resource");
    public static final Action.Id<String> OPEN_URL = Action.Id.of("user/common.open_url.url");
    public static final Action.Id<String> COPY_STRING = Action.Id.of("user/common.copy_string");
    public static final Action.Id<Object> OPEN_AZURE_SETTINGS = Action.OPEN_AZURE_SETTINGS;
    public static final Action.Id<Object> OPEN_AZURE_EXPLORER = Action.Id.of("user/common.open_azure_explorer");
    public static final Action.Id<Object> OPEN_AZURE_REFERENCE_BOOK = Action.Id.of("user/common.open_azure_reference_book");
    public static final Action.Id<AzComponent> BROWSE_SERVICE_AZURE_SAMPLES = Action.Id.of("user/samples.browse_service_samples.service");
    public static final Action.Id<Object> SELECT_RESOURCE_IN_EXPLORER = Action.Id.of("internal/common.select_resource_in_explorer");
    public static final Action.Id<Object> INSTALL_DOTNET_RUNTIME = Action.Id.of("user/bicep.install_dotnet_runtime");
    public static final Action.Id<Object> RESTART_IDE = Action.Id.of("user/common.restart_ide");
    public static final Action.Id<File> REVEAL_FILE = Action.Id.of("user/common.reveal_file_in_explorer");
    public static final Action.Id<File> OPEN_FILE = Action.Id.of("user/common.open_file_in_editor");
    public static final Action.Id<ServiceLinker> FOCUS_ON_CONNECTED_SERVICE = Action.Id.of("user/common.focus_on_connected_service");
    public static final Action.Id<ServiceLinkerModule> CREATE_SERVICE_LINKER_IN_PORTAL = Action.Id.of("user/$resource.create_service_linker_in_portal");
    public static final Action.Id<AbstractAzService<?, ?>> GETTING_STARTED = Action.Id.of("user/$resource.open_getting_start.service");
    public static final Action.Id<String> INVOKE_COMMAND_IN_TERMINAL = Action.Id.of("user/common.invoke_command_in_terminal");
    public static final Action.Id<Object> BROWSE_AZURE_SAMPLES = Action.Id.of("user/samples.browse_samples");
    public static final Action.Id<String> SEARCH_INSTALLED_PLUGIN = Action.Id.of("user/common.search_installed_plugin.keyword");
    public static final Action.Id<String> SEARCH_MARKETPLACE_PLUGIN = Action.Id.of("user/common.search_marketplace_plugin.keyword");
    public static final Action.Id<String> ENABLE_PLUGIN = Action.Id.of("user/common.enable_plugin.pluginid");
    public static final Action.Id<String> ENABLE_PLUGIN_AND_RESTART = Action.Id.of("user/common.enable_plugin_and_restart.pluginid");
    public static final Action.Id<Object> SHOW_COURSES = Action.Id.of("user/$resource.show_courses");
    public static final Action.Id<Object> OPEN_MONITOR = Action.Id.of("user/monitor.open_azure_monitor");
    public static final Action.Id<Action.Id<?>> SUPPRESS_ACTION = Action.Id.of("user/common.never_show_again");

    public static final String SERVICE_LINKER_ACTIONS = "actions.resource.service_linker";
    public static final String SERVICE_LINKER_MODULE_ACTIONS = "actions.resource.service_linker_module";
    public static final String CONNECTION_STRING_RESOURCE_ACTIONS = "actions.connection_string_resource";
    public static final String RESOURCE_GROUP_CREATE_ACTIONS = "actions.resource.create.group";

    @Override
    public void registerActions(AzureActionManager am) {
        final AzureActionManager.Shortcuts shortcuts = am.getIDEDefaultShortcuts();
        new Action<>(START)
            .withLabel("Start")
            .withIcon(AzureIcons.Action.START.getIconPath())
            .withIdParam(AzResource::getName)
            .withShortcut(shortcuts.start())
            .visibleWhen(s -> s instanceof Startable && ((Startable) s).isStartable())
            .withHandler(s -> ((Startable) s).start())
            .register(am);

        new Action<>(STOP)
            .withLabel("Stop")
            .withIcon(AzureIcons.Action.STOP.getIconPath())
            .withIdParam(AzResource::getName)
            .withShortcut(shortcuts.stop())
            .visibleWhen(s -> s instanceof Startable && ((Startable) s).isStoppable())
            .withHandler(s -> ((Startable) s).stop())
            .register(am);

        new Action<>(RESTART)
            .withLabel("Restart")
            .withIcon(AzureIcons.Action.RESTART.getIconPath())
            .withIdParam(AzResource::getName)
            .withShortcut(shortcuts.restart())
            .visibleWhen(s -> s instanceof Startable && ((Startable) s).isRestartable())
            .withHandler(s -> ((Startable) s).restart())
            .register(am);

        new Action<>(DELETE)
            .withLabel("Delete")
            .withIcon(AzureIcons.Action.DELETE.getIconPath())
            .withIdParam(AzResource::getName)
            .withShortcut(shortcuts.delete())
            .visibleWhen((s, place) -> s instanceof AzResource && s instanceof Deletable)
            .enableWhen(s -> {
                if (s instanceof AbstractAzResource) {
                    final AbstractAzResource<?, ?, ?> r = (AbstractAzResource<?, ?, ?>) s;
                    return !r.getFormalStatus().isDeleted() && !r.isDraftForCreating();
                }
                return true;
            })
            .withHandler((s) -> {
                if (AzureMessager.getMessager().confirm(String.format("This will delete %s \"%s\" from Azure cloud, are you sure to do this?", s.getResourceTypeName(), s.getName()))) {
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
            .withHandler((s, e) -> am.getAction(OPEN_URL).handle(s.getPortalUrl(), e))
            .register(am);

        new Action<>(OPEN_URL)
            .withLabel("Open Url")
            .withIdParam(u -> u)
            .withAuthRequired(false)
            .register(am);

        new Action<>(COPY_STRING)
            .withLabel("Copy")
            .withIdParam(u -> u)
            .withHandler(ResourceCommonActionsContributor::copyString)
            .withAuthRequired(false)
            .register(am);

        new Action<>(CONNECT)
            .withLabel("Connect to Project")
            .withIcon(AzureIcons.Connector.CONNECT.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen((s, place) -> !StringUtils.startsWithAny(place.toLowerCase(), "projectviewpopup", "project.view") && s instanceof AzResource)
            .enableWhen(s -> s.getFormalStatus().isRunning())
            .withAuthRequired(false)
            .register(am);

        new Action<>(SHOW_PROPERTIES)
            .withLabel("Show Properties")
            .withIcon(AzureIcons.Action.PROPERTIES.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof AzResource)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .withShortcut(shortcuts.edit())
            .register(am);

        new Action<>(DEPLOY)
            .withLabel("Deploy")
            .withIcon(AzureIcons.Action.DEPLOY.getIconPath())
            .withIdParam(AzResource::getName)
            .withShortcut("control alt O")
            .visibleWhen(s -> s instanceof AzResource)
            .enableWhen(s -> s.getFormalStatus().isRunning())
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

        new Action<>(SELECT_RESOURCE_IN_EXPLORER)
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
                } else if (r instanceof AzResourceModule) {
                    return ((AzResourceModule<?>) r).getResourceTypeName();
                }
                return r.getClass().getSimpleName();
            })
            .withShortcut(shortcuts.add())
            .visibleWhen(s -> s instanceof AzService || s instanceof AzResourceModule || s instanceof AzResource)
            .enableWhen(s -> !(s instanceof AzResource) || !StringUtils.equalsIgnoreCase(((AzResource) s).getStatus(), AzResource.Status.CREATING))
            .register(am);

        new Action<>(CREATE_IN_PORTAL)
            .withLabel("Create In Azure Portal")
            .withIcon(AzureIcons.Action.CREATE.getIconPath())
            .visibleWhen(s -> s instanceof AzService)
            .withHandler((s, e) -> {
                final IAccount account = Azure.az(IAzureAccount.class).account();
                final String url = String.format("%s/#create/%s", account.getPortalUrl(), s.getName());
                am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(url, e);
            })
            .withShortcut(shortcuts.add())
            .register(am);

        final Favorites favorites = Favorites.getInstance();
        new Action<>(PIN)
            .withLabel(s -> Objects.nonNull(s) && favorites.exists(s.getId()) ? "Unmark As Favorite" : "Mark As Favorite")
            .withIcon(s -> Objects.nonNull(s) && favorites.exists(s.getId()) ? AzureIcons.Action.PIN.getIconPath() : AzureIcons.Action.UNPIN.getIconPath())
            .withShortcut("F11")
            .visibleWhen((s, place) -> StringUtils.startsWithIgnoreCase(place, AZURE_EXPLORER) && s instanceof AbstractAzResource)
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

        new Action<>(INVOKE_COMMAND_IN_TERMINAL)
            .withLabel("Invoke command in terminal")
            .withAuthRequired(false)
            .register(am);

        new Action<>(FOCUS_ON_CONNECTED_SERVICE)
            .withLabel(s -> "Focus on Connected Resource")
            .withIcon(s -> AzureIcons.Connector.FOCUS_ON_CONNECTED_SERVICE.getIconPath())
            .visibleWhen(s -> s instanceof ServiceLinker)
            .withHandler((r) -> {
                final AzResource resource = Azure.az().getById(r.getTargetServiceId());
                if (Objects.isNull(resource)) {
                    final String serviceName = ResourceId.fromString(r.getTargetServiceId()).name();
                    AzureMessager.getMessager().info(AzureString.format("Cannot find connected service(%s) in Azure Explorer.", serviceName));
                    return;
                }
                AzureEventBus.emit("azure.explorer.select_resource", resource);
            })
            .withAuthRequired(false)
            .register(am);

        new Action<>(CREATE_SERVICE_LINKER_IN_PORTAL)
            .withLabel(s -> "Create In Azure Portal")
            .withIcon(AzureIcons.Action.CREATE.getIconPath())
            .visibleWhen(s -> s instanceof ServiceLinkerModule)
            .withHandler((r, e) -> {
                if (r.getParent() instanceof SpringCloudDeployment) {
                    final SpringCloudApp app = ((SpringCloudDeployment) r.getParent()).getParent();
                    final String appUrl = app.getParent().getPortalUrl();
                    final String message = String.format("Please create Service Connector from {0} in <a href=\"%s\">Azure portal</a>.", appUrl);
                    AzureMessager.getMessager().info(AzureString.format(message, String.format("apps/%s/settings/Service Connector", app.getName())));
                    return;
                }
                final String parentUrl = r.getParent().getPortalUrl();
                am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(String.format("%s/serviceConnector", parentUrl), e);
            })
            .register(am);

        new Action<>(Action.DISABLE_AUTH_CACHE)
            .withLabel("Disable Auth Cache")
            .visibleWhen(s -> Azure.az().config().isAuthPersistenceEnabled())
            .withHandler((s) -> {
                Azure.az().config().setAuthPersistenceEnabled(false);
                AzureConfigInitializer.saveAzConfig();
                final AzureAccount az = Azure.az(AzureAccount.class);
                if (az.isLoggedIn()) {
                    az.logout();
                }
                final Action<Object> signIn = am.getAction(Action.AUTHENTICATE);
                AzureMessager.getMessager().info("Auth cache disabled, please re-signin to take effect.", signIn);
            })
            .withAuthRequired(false)
            .register(am);

        new Action<>(GETTING_STARTED)
            .withLabel("Getting Started")
            .withIdParam(s -> s.getClass().getSimpleName())
            .withIcon(AzureIcons.Common.GET_START.getIconPath())
            .withAuthRequired(false)
            .register(am);

        new Action<>(SHOW_COURSES)
            .withLabel("Getting Started")
            .withIdParam(s -> s.getClass().getSimpleName())
            .withIcon(o -> {
                final String isActionTriggerVal = AzureStoreManager.getInstance().getIdeStore().getProperty("guidance", "is_action_triggered");
                final boolean isActionTriggered = Optional.ofNullable(isActionTriggerVal).map(Boolean::parseBoolean).orElse(false);
                return isActionTriggered ? GET_START.getIconPath() : GET_START_NEW.getIconPath();
            })
            .withAuthRequired(false)
            .register(am);

        new Action<>(OPEN_MONITOR)
            .withLabel("Open Azure Monitor")
            .withIcon(AzureIcons.Common.AZURE_MONITOR.getIconPath())
            .withAuthRequired(false)
            .register(am);

        new Action<>(SUPPRESS_ACTION)
            .withLabel("Don't show again")
            .withAuthRequired(false)
            .register(am);

        new Action<>(SEARCH_INSTALLED_PLUGIN)
            .withLabel("Search installed plugin")
            .withIdParam(s -> s)
            .withAuthRequired(false)
            .register(am);

        new Action<>(SEARCH_MARKETPLACE_PLUGIN)
            .withLabel("Search plugin in marketplace")
            .withIdParam(s -> s)
            .withAuthRequired(false)
            .register(am);

        new Action<>(ENABLE_PLUGIN)
            .withLabel("Enable plugin")
            .withIdParam(s -> s)
            .withAuthRequired(false)
            .register(am);

        new Action<>(ENABLE_PLUGIN_AND_RESTART)
            .withLabel("Enable plugin and Restart")
            .withIdParam(s -> s)
            .withAuthRequired(false)
            .register(am);

        new Action<>(OPEN_IN_SERVICES_VIEW)
            .withIcon(AzureIcons.Common.SERVICES.getIconPath())
            .withLabel("Open in \"Services\" View")
            .withIdParam(AbstractAzResource::getName)
            .withAuthRequired(false)
            .register(am);

        new Action<>(StreamingLogSupport.OPEN_STREAMING_LOG)
            .withLabel("Open Log Streaming")
            .withIdParam(StreamingLogSupport::getDisplayName)
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
        am.registerGroup(SERVICE_LINKER_ACTIONS, new ActionGroup(
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            ResourceCommonActionsContributor.FOCUS_ON_CONNECTED_SERVICE,
            ResourceCommonActionsContributor.DELETE
        ));
        am.registerGroup(SERVICE_LINKER_MODULE_ACTIONS, new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.CREATE_SERVICE_LINKER_IN_PORTAL
        ));

        final ActionGroup connectionStringActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            ResourceCommonActionsContributor.BROWSE_SERVICE_AZURE_SAMPLES
        );
        am.registerGroup(CONNECTION_STRING_RESOURCE_ACTIONS, connectionStringActionGroup);
    }

    public int getOrder() {
        return INITIALIZE_ORDER; //after azure resource common actions registered
    }
}
