/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.containerapps;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.Revision;

import java.util.Optional;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.lib.common.operation.OperationBundle.description;

public class ContainerAppsActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.containerapps.service";
    public static final String ENVIRONMENT_ACTIONS = "actions.containerapps.environment";
    public static final String CONTAINER_APP_ACTIONS = "actions.containerapps.containerapp";
    public static final String REVISION_ACTIONS = "actions.containerapps.revision";

    public static final Action.Id<ContainerApp> OPEN_LATEST_REVISION_IN_BROWSER = Action.Id.of("containerapps.open_in_browser.app");
    public static final Action.Id<ContainerApp> ACTIVATE_LATEST_REVISION = Action.Id.of("containerapps.activate_latest_revision.app");
    public static final Action.Id<ContainerApp> DEACTIVATE_LATEST_REVISION = Action.Id.of("containerapps.deactivate_latest_revision.app");
    public static final Action.Id<ContainerApp> RESTART_LATEST_REVISION = Action.Id.of("containerapps.restart_latest_revision.app");
    public static final Action.Id<ContainerApp> UPDATE_IMAGE = Action.Id.of("containerapps.update_image.app");
    public static final Action.Id<ContainerApp> OPEN_LOG_STREAMS = Action.Id.of("containerapps.open_log_streams.app");
    public static final Action.Id<Revision> ACTIVATE = Action.Id.of("containerapps.activate.revision");
    public static final Action.Id<Revision> DEACTIVATE = Action.Id.of("containerapps.deactivate.revision");
    public static final Action.Id<Revision> RESTART = Action.Id.of("containerapps.restart.revision");
    public static final Action.Id<Revision> OPEN_IN_BROWSER = Action.Id.of("containerapps.open_in_browser.revision");

    @Override
    public void registerActions(AzureActionManager am) {
        final Consumer<ContainerApp> openInBrowser = resource ->
                AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle("https://" + resource.getLatestRevisionFqdn());
        final ActionView.Builder openInBrowserView = new ActionView.Builder("Open In Browser", AzureIcons.Action.BROWSER.getIconPath())
                .title(s -> Optional.ofNullable(s).map(r -> description("containerapps.open_in_browser.app", ((ContainerApp) r).getName())).orElse(null))
                .enabled(s -> s instanceof ContainerApp && ((ContainerApp) s).getFormalStatus().isConnected());
        final Action<ContainerApp> openInBrowserAction = new Action<>(OPEN_LATEST_REVISION_IN_BROWSER, openInBrowser, openInBrowserView);
        am.registerAction(openInBrowserAction);

        final Consumer<ContainerApp> activateLatestRevision = resource -> resource.activate();
        final ActionView.Builder activateLatestRevisionView = new ActionView.Builder("Activate Latest Revision", AzureIcons.Action.START.getIconPath())
                .title(s -> Optional.ofNullable(s).map(r -> description("containerapps.activate_latest_revision.app", ((ContainerApp) r).getName())).orElse(null))
                .enabled(s -> s instanceof ContainerApp && ((ContainerApp) s).getFormalStatus().isConnected());
        final Action<ContainerApp> activateLatestRevisionAction = new Action<>(ACTIVATE_LATEST_REVISION, activateLatestRevision, activateLatestRevisionView);
        am.registerAction(activateLatestRevisionAction);

        final Consumer<ContainerApp> deactivateLatestRevision = resource -> resource.deactivate();
        final ActionView.Builder deactivateLatestRevisionView = new ActionView.Builder("Deactivate Latest Revision", AzureIcons.Action.STOP.getIconPath())
                .title(s -> Optional.ofNullable(s).map(r -> description("containerapps.deactivate_latest_revision.app", ((ContainerApp) r).getName())).orElse(null))
                .enabled(s -> s instanceof ContainerApp && ((ContainerApp) s).getFormalStatus().isConnected());
        final Action<ContainerApp> deactivateLatestRevisionAction = new Action<>(DEACTIVATE_LATEST_REVISION, deactivateLatestRevision, deactivateLatestRevisionView);
        am.registerAction(deactivateLatestRevisionAction);

        final Consumer<ContainerApp> restartLatestRevision = resource -> resource.restart();
        final ActionView.Builder restartLatestRevisionView = new ActionView.Builder("Restart Latest Revision", AzureIcons.Action.RESTART.getIconPath())
                .title(s -> Optional.ofNullable(s).map(r -> description("containerapps.restart_latest_revision.app", ((ContainerApp) r).getName())).orElse(null))
                .enabled(s -> s instanceof ContainerApp && ((ContainerApp) s).getFormalStatus().isConnected());
        final Action<ContainerApp> restartLatestRevisionAction = new Action<>(RESTART_LATEST_REVISION, restartLatestRevision, restartLatestRevisionView);
        am.registerAction(restartLatestRevisionAction);

        final ActionView.Builder updateImageView = new ActionView.Builder("Update Image", AzureIcons.Action.UPLOAD.getIconPath())
                .title(s -> Optional.ofNullable(s).map(r -> description("containerapps.update_image.app", ((ContainerApp) r).getName())).orElse(null))
                .enabled(s -> s instanceof ContainerApp && ((ContainerApp) s).getFormalStatus().isConnected());
        final Action<ContainerApp> updateImageAction = new Action<>(UPDATE_IMAGE, updateImageView);
        am.registerAction(updateImageAction);

        final Consumer<ContainerApp> openLogStreams = resource ->
                AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle(resource.getPortalUrl() + "/logstream");
        final ActionView.Builder openLogStreamView = new ActionView.Builder("Open Log Streams", AzureIcons.Action.BROWSER.getIconPath())
                .title(s -> Optional.ofNullable(s).map(r -> description("containerapps.open_log_streams.app", ((ContainerApp) r).getName())).orElse(null))
                .enabled(s -> s instanceof ContainerApp && ((ContainerApp) s).getFormalStatus().isConnected());
        final Action<ContainerApp> openLogStreamsAction = new Action<>(OPEN_LOG_STREAMS, openLogStreams, openLogStreamView);
        am.registerAction(openLogStreamsAction);

        final Consumer<Revision> activate = resource -> resource.activate();
        final ActionView.Builder activateView = new ActionView.Builder("Activate", AzureIcons.Action.START.getIconPath())
                .title(s -> Optional.ofNullable(s).map(r -> description("containerapps.activate.revision", ((Revision) r).getName())).orElse(null))
                .enabled(s -> s instanceof Revision && ((Revision) s).getFormalStatus().isConnected());
        final Action<Revision> activateRevisionAction = new Action<>(ACTIVATE, activate, activateView);
        am.registerAction(activateRevisionAction);

        final Consumer<Revision> deactivateRevision = resource -> resource.deactivate();
        final ActionView.Builder deactivateRevisionView = new ActionView.Builder("Deactivate", AzureIcons.Action.STOP.getIconPath())
                .title(s -> Optional.ofNullable(s).map(r -> description("containerapps.deactivate.revision", ((Revision) r).getName())).orElse(null))
                .enabled(s -> s instanceof Revision && ((Revision) s).getFormalStatus().isConnected());
        final Action<Revision> deactivateRevisionAction = new Action<>(DEACTIVATE, deactivateRevision, deactivateRevisionView);
        am.registerAction(deactivateRevisionAction);

        final Consumer<Revision> restartRevision = resource -> resource.restart();
        final ActionView.Builder restartRevisionView = new ActionView.Builder("Restart", AzureIcons.Action.RESTART.getIconPath())
                .title(s -> Optional.ofNullable(s).map(r -> description("containerapps.restart.revision", ((Revision) r).getName())).orElse(null))
                .enabled(s -> s instanceof Revision && ((Revision) s).getFormalStatus().isConnected());
        final Action<Revision> restartRevisionAction = new Action<>(RESTART, restartRevision, restartRevisionView);
        am.registerAction(restartRevisionAction);

        final Consumer<Revision> openRevision = resource ->
                AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle(resource.getFqdn());
        final ActionView.Builder openRevisionView = new ActionView.Builder("Open In Browser", AzureIcons.Action.BROWSER.getIconPath())
                .title(s -> Optional.ofNullable(s).map(r -> description("containerapps.open_in_browser.revision", ((ContainerApp) r).getName())).orElse(null))
                .enabled(s -> s instanceof Revision && ((Revision) s).getFormalStatus().isConnected());
        final Action<Revision> openRevisionAction = new Action<>(OPEN_IN_BROWSER, openRevision, openRevisionView);
        am.registerAction(openRevisionAction);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
                "---",
                ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup environmentActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.PIN,
                "---",
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
                ResourceCommonActionsContributor.OPEN_PORTAL_URL,
                "---",
                ResourceCommonActionsContributor.CREATE,
                ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(ENVIRONMENT_ACTIONS, environmentActionGroup);

        final ActionGroup containerAppActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.PIN,
                "---",
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
                ResourceCommonActionsContributor.OPEN_PORTAL_URL,
                ContainerAppsActionsContributor.OPEN_LATEST_REVISION_IN_BROWSER,
                "---",
                ContainerAppsActionsContributor.ACTIVATE_LATEST_REVISION,
                ContainerAppsActionsContributor.DEACTIVATE_LATEST_REVISION,
                ContainerAppsActionsContributor.RESTART_LATEST_REVISION,
                ContainerAppsActionsContributor.UPDATE_IMAGE,
                ResourceCommonActionsContributor.DELETE,
                "---",
                ContainerAppsActionsContributor.OPEN_LOG_STREAMS
        );
        am.registerGroup(CONTAINER_APP_ACTIONS, containerAppActionGroup);

        final ActionGroup revisionActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.PIN,
                "---",
                ResourceCommonActionsContributor.REFRESH,
                ContainerAppsActionsContributor.OPEN_IN_BROWSER,
                "---",
                ContainerAppsActionsContributor.ACTIVATE,
                ContainerAppsActionsContributor.DEACTIVATE,
                ContainerAppsActionsContributor.RESTART,
                ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(REVISION_ACTIONS, revisionActionGroup);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
