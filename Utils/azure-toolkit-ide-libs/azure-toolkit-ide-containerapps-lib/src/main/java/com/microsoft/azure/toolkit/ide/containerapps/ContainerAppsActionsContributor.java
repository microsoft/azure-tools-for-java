/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.containerapps;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAccount;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.view.IView;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.Revision;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Objects;

import static com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor.SHOW_PROPERTIES;

public class ContainerAppsActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.containerapps.service";
    public static final String ENVIRONMENT_ACTIONS = "actions.containerapps.environment";
    public static final String CONTAINER_APP_ACTIONS = "actions.containerapps.containerapp";
    public static final String REVISION_ACTIONS = "actions.containerapps.revision";
    public static final String REVISION_MODULE_ACTIONS = "actions.containerapps.revision_module";
    public static final String STREAMING_LOG_ACTIONS = "actions.containerapps.streaming_log.group";
    public static final String CONTAINER_APPS_ENVIRONMENT_CREATE_ACTIONS = "actions.resource.create.container_apps_environment";

    public static final Action.Id<ContainerAppsEnvironment> CREATE_CONTAINER_APP = ContainerAppsEnvironment.CREATE_CONTAINER_APP;
    public static final Action.Id<ContainerAppsEnvironment> START_ENV_LOG_STREAM = Action.Id.of("user/containerapps.start_log_streams.environment");
    public static final Action.Id<ContainerAppsEnvironment> STOP_ENV_LOG_STREAM = Action.Id.of("user/containerapps.stop_log_streams.environment");
    public static final Action.Id<AzureContainerApps> CREATE_CONTAINER_APPS_ENVIRONMENT = Action.Id.of("user/containerapps.create_container_apps_environment");
    public static final Action.Id<ContainerApp> BROWSE = ContainerApp.BROWSE;
    public static final Action.Id<ContainerApp> ACTIVATE_LATEST_REVISION = Action.Id.of("user/containerapps.activate_latest_revision.app");
    public static final Action.Id<ContainerApp> DEACTIVATE_LATEST_REVISION = Action.Id.of("user/containerapps.deactivate_latest_revision.app");
    public static final Action.Id<ContainerApp> RESTART_LATEST_REVISION = Action.Id.of("user/containerapps.restart_latest_revision.app");
    public static final Action.Id<ContainerApp> UPDATE_IMAGE = ContainerApp.UPDATE_IMAGE;
    public static final Action.Id<ContainerApp> START_CONSOLE_LOG_STREAMS = Action.Id.of("user/containerapps.start_console_log_streams.app");
    public static final Action.Id<ContainerApp> START_SYSTEM_LOG_STREAMS = Action.Id.of("user/containerapps.start_system_log_streams.app");
    public static final Action.Id<ContainerApp> STOP_APP_LOG_STREAMS = Action.Id.of("user/containerapps.stop_log_streams.app");
    public static final Action.Id<ContainerApp> OPEN_LOGS_IN_MONITOR = Action.Id.of("user/containerapps.open_azure_monitor.app");
    public static final Action.Id<Revision> ACTIVATE = Action.Id.of("user/containerapps.activate.revision");
    public static final Action.Id<Revision> DEACTIVATE = Action.Id.of("user/containerapps.deactivate.revision");
    public static final Action.Id<Revision> RESTART = Action.Id.of("user/containerapps.restart.revision");
    public static final Action.Id<Revision> OPEN_IN_BROWSER = Action.Id.of("user/containerapps.open_in_browser.revision");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_CONTAINER_APP = Action.Id.of("user/containerapps.create_container_app.group");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_CONTAINER_APPS_ENVIRONMENT = Action.Id.of("user/containerapps.create_container_apps_environment.group");
    public static final Action.Id<Object> SERVICE_CREATE_CONTAINER_APP = Action.Id.of("user/containerapps.service_create_container_app");

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(CREATE_CONTAINER_APP)
            .withLabel("Container App")
            .register(am);

        new Action<>(SERVICE_CREATE_CONTAINER_APP)
            .withLabel("Create Container App")
            .withIcon(AzureIcons.Action.CREATE.getIconPath())
            .register(am);

        new Action<>(START_ENV_LOG_STREAM)
                .withLabel("Start Streaming Logs")
                .withIcon(AzureIcons.Action.LOG.getIconPath())
                .withIdParam(AbstractAzResource::getName)
                .register(am);

        new Action<>(STOP_ENV_LOG_STREAM)
                .withLabel("Stop Streaming Logs")
                .withIcon(AzureIcons.Action.LOG.getIconPath())
                .withIdParam(AbstractAzResource::getName)
                .register(am);

        new Action<>(CREATE_CONTAINER_APPS_ENVIRONMENT)
                .withLabel("Create Container Apps Environment")
                .withIcon(AzureIcons.Action.CREATE.getIconPath())
                .register(am);

        new Action<>(BROWSE)
            .withLabel("Open In Browser")
            .withIcon(AzureIcons.Action.BROWSER.getIconPath())
            .withIdParam(AbstractAzResource::getName)
            .visibleWhen(s -> s instanceof ContainerApp)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .withHandler((s, e) -> {
                if (!s.isIngressEnabled() || StringUtils.isBlank(s.getIngressFqdn())) {
                    final Action<AzResource> action = new Action<>(SHOW_PROPERTIES)
                        .withLabel("Open Properties Editor")
                        .withHandler(r -> am.getAction(ResourceCommonActionsContributor.SHOW_PROPERTIES).handle(s, e));
                    AzureMessager.getMessager().warning("Ingress is not enabled for this container app.", null, action);
                } else {
                    am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle("https://" + s.getIngressFqdn(), e);
                }
            })
            .register(am);

        new Action<>(ACTIVATE_LATEST_REVISION)
            .withLabel("Activate Latest Revision")
            .withIcon(AzureIcons.Action.START.getIconPath())
            .withIdParam(AbstractAzResource::getName)
            .visibleWhen(s -> s instanceof ContainerApp &&
                ((ContainerApp) s).getFormalStatus().isConnected() &&
                Objects.nonNull(((ContainerApp) s).getCachedLatestRevision()) &&
                !((ContainerApp) s).getCachedLatestRevision().isActive())
            .withHandler(ContainerApp::activate)
            .register(am);

        new Action<>(DEACTIVATE_LATEST_REVISION)
            .withLabel("Deactivate Latest Revision")
            .withIcon(AzureIcons.Action.STOP.getIconPath())
            .withIdParam(AbstractAzResource::getName)
            .visibleWhen(s -> s instanceof ContainerApp &&
                ((ContainerApp) s).getFormalStatus().isConnected() &&
                Objects.nonNull(((ContainerApp) s).getCachedLatestRevision()) &&
                ((ContainerApp) s).getCachedLatestRevision().isActive())
            .withHandler(ContainerApp::deactivate)
            .register(am);

        new Action<>(RESTART_LATEST_REVISION)
            .withLabel("Restart Latest Revision")
            .withIcon(AzureIcons.Action.RESTART.getIconPath())
            .withIdParam(AbstractAzResource::getName)
            .visibleWhen(s -> s instanceof ContainerApp &&
                ((ContainerApp) s).getFormalStatus().isConnected() &&
                Objects.nonNull(((ContainerApp) s).getCachedLatestRevision()) &&
                ((ContainerApp) s).getCachedLatestRevision().isActive())
            .withHandler(ContainerApp::restart)
            .register(am);

        new Action<>(UPDATE_IMAGE)
            .withLabel("Update Deployment")
            .withIcon(AzureIcons.Action.DEPLOY.getIconPath())
            .withIdParam(AbstractAzResource::getName)
            .visibleWhen(s -> s instanceof ContainerApp)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);

        new Action<>(START_CONSOLE_LOG_STREAMS)
            .withLabel("Console")
            .withIdParam(AbstractAzResource::getName)
            .visibleWhen(s -> s instanceof ContainerApp)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);

        new Action<>(START_SYSTEM_LOG_STREAMS)
                .withLabel("System")
                .withIdParam(AbstractAzResource::getName)
                .visibleWhen(s -> s instanceof ContainerApp)
                .enableWhen(s -> s.getFormalStatus().isConnected())
                .register(am);

        new Action<>(STOP_APP_LOG_STREAMS)
                .withLabel("Stop Streaming Logs")
                .withIcon(AzureIcons.Action.LOG.getIconPath())
                .withIdParam(AbstractAzResource::getName)
                .enableWhen(s -> s.getFormalStatus().isConnected())
                .register(am);

        new Action<>(OPEN_LOGS_IN_MONITOR)
                .withLabel("Open Logs with Azure Monitor")
                .withIcon(AzureIcons.Common.AZURE_MONITOR.getIconPath())
                .withIdParam(AzResource::getName)
                .visibleWhen(s -> s instanceof ContainerApp)
                .enableWhen(s -> s.getFormalStatus().isConnected())
                .register(am);

        new Action<>(ACTIVATE)
            .withLabel("Activate")
            .withIcon(AzureIcons.Action.START.getIconPath())
            .withIdParam(AbstractAzResource::getName)
            .visibleWhen(s -> s instanceof Revision && ((Revision) s).getFormalStatus().isConnected() && !((Revision) s).isActive())
            .withHandler(Revision::activate)
            .register(am);

        new Action<>(DEACTIVATE)
            .withLabel("Deactivate")
            .withIcon(AzureIcons.Action.STOP.getIconPath())
            .withIdParam(AbstractAzResource::getName)
            .visibleWhen(s -> s instanceof Revision && ((Revision) s).getFormalStatus().isConnected() && ((Revision) s).isActive())
            .withHandler(Revision::deactivate)
            .register(am);

        new Action<>(RESTART)
            .withLabel("Restart")
            .withIcon(AzureIcons.Action.RESTART.getIconPath())
            .withIdParam(AbstractAzResource::getName)
            .visibleWhen(s -> s instanceof Revision)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .visibleWhen(s -> s instanceof Revision && ((Revision) s).getFormalStatus().isConnected() && ((Revision) s).isActive())
            .withHandler(Revision::restart)
            .register(am);

        new Action<>(OPEN_IN_BROWSER)
            .withLabel("Open In Browser")
            .withIcon(AzureIcons.Action.BROWSER.getIconPath())
            .withIdParam(AbstractAzResource::getName)
            .visibleWhen(s -> s instanceof Revision)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .withHandler((s, e) -> am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle("https://" + s.getFqdn(), e))
            .register(am);

        new Action<>(GROUP_CREATE_CONTAINER_APP)
            .withLabel("Container App")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ResourceGroup)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .withHandler((s, e) -> {
                final IAccount account = Azure.az(IAzureAccount.class).account();
                final String url = String.format("%s/#create/Microsoft.ContainerApp", account.getPortalUrl());
                am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(url, e);
            })
            .register(am);

        new Action<>(GROUP_CREATE_CONTAINER_APPS_ENVIRONMENT)
                .withLabel("Container Apps Environment")
                .withIdParam(AzResource::getName)
                .visibleWhen(s-> s instanceof ResourceGroup)
                .enableWhen(s -> s != null && s.getFormalStatus().isConnected())
                .register(am);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final IView.Label.Static createView = new IView.Label.Static("Create", "/icons/action/create.svg");
        final ActionGroup environmentCreateActions = new ActionGroup(new ArrayList<>(), createView);
        environmentCreateActions.addAction(CREATE_CONTAINER_APP);
        am.registerGroup(CONTAINER_APPS_ENVIRONMENT_CREATE_ACTIONS, environmentCreateActions);

        final IView.Label.Static view = new IView.Label.Static("Start Streaming Logs", AzureIcons.Action.LOG.getIconPath());
        final ActionGroup streamingLogActionGroup = new ActionGroup(new ArrayList<>(), view);
        streamingLogActionGroup.addAction(START_CONSOLE_LOG_STREAMS);
        streamingLogActionGroup.addAction(START_SYSTEM_LOG_STREAMS);
        am.registerGroup(STREAMING_LOG_ACTIONS, streamingLogActionGroup);

        final ActionGroup serviceActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            ResourceCommonActionsContributor.BROWSE_SERVICE_AZURE_SAMPLES,
            "---",
            ContainerAppsActionsContributor.CREATE_CONTAINER_APPS_ENVIRONMENT,
            ContainerAppsActionsContributor.SERVICE_CREATE_CONTAINER_APP
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup environmentActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            "---",
            ContainerAppsActionsContributor.CONTAINER_APPS_ENVIRONMENT_CREATE_ACTIONS,
            ResourceCommonActionsContributor.DELETE,
            "---",
            ContainerAppsActionsContributor.START_ENV_LOG_STREAM,
            ContainerAppsActionsContributor.STOP_ENV_LOG_STREAM
        );
        am.registerGroup(ENVIRONMENT_ACTIONS, environmentActionGroup);

        final ActionGroup containerAppActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            ContainerAppsActionsContributor.BROWSE,
            SHOW_PROPERTIES,
            "---",
            ContainerAppsActionsContributor.UPDATE_IMAGE,
            "---",
            ResourceCommonActionsContributor.DELETE,
            "---",
            ContainerAppsActionsContributor.ACTIVATE_LATEST_REVISION,
            ContainerAppsActionsContributor.DEACTIVATE_LATEST_REVISION,
            ContainerAppsActionsContributor.RESTART_LATEST_REVISION,
            "---",
            ContainerAppsActionsContributor.STREAMING_LOG_ACTIONS,
            ContainerAppsActionsContributor.STOP_APP_LOG_STREAMS,
            ContainerAppsActionsContributor.OPEN_LOGS_IN_MONITOR
        );
        am.registerGroup(CONTAINER_APP_ACTIONS, containerAppActionGroup);

        final ActionGroup revisionModuleGroup = new ActionGroup(ResourceCommonActionsContributor.REFRESH);
        am.registerGroup(REVISION_MODULE_ACTIONS, revisionModuleGroup);

        final ActionGroup revisionActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ContainerAppsActionsContributor.OPEN_IN_BROWSER,
            "---",
            ResourceCommonActionsContributor.DELETE,
            "---",
            ContainerAppsActionsContributor.ACTIVATE,
            ContainerAppsActionsContributor.DEACTIVATE,
            ContainerAppsActionsContributor.RESTART
        );
        am.registerGroup(REVISION_ACTIONS, revisionActionGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_CONTAINER_APP);
        group.addAction(GROUP_CREATE_CONTAINER_APPS_ENVIRONMENT);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
