/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.webapp;

import com.microsoft.azure.toolkit.ide.appservice.AppServiceActionsContributor;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.lib.common.operation.OperationBundle.description;

public class WebAppActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = AppServiceActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.webapp.service";
    public static final String WEBAPP_ACTIONS = "actions.webapp.management";
    public static final String DEPLOYMENT_SLOTS_ACTIONS = "actions.webapp.deployment_slots";
    public static final String DEPLOYMENT_SLOT_ACTIONS = "actions.webapp.deployment_slot";

    public static final Action.Id<WebApp> REFRESH_DEPLOYMENT_SLOTS = Action.Id.of("webapp.refresh_deployment_slots");
    public static final Action.Id<WebAppDeploymentSlot> SWAP_DEPLOYMENT_SLOT = Action.Id.of("webapp.swap_deployment_slot");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_WEBAPP = Action.Id.of("group.create_webapp");

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            "---",
            ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup webAppActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            AppServiceActionsContributor.OPEN_IN_BROWSER,
            ResourceCommonActionsContributor.SHOW_PROPERTIES,
            "---",
            ResourceCommonActionsContributor.DEPLOY,
            "---",
            ResourceCommonActionsContributor.START,
            ResourceCommonActionsContributor.STOP,
            ResourceCommonActionsContributor.RESTART,
            ResourceCommonActionsContributor.DELETE,
            "---",
            AppServiceActionsContributor.PROFILE_FLIGHT_RECORD,
            AppServiceActionsContributor.SSH_INTO_WEBAPP,
            AppServiceActionsContributor.START_STREAM_LOG,
            AppServiceActionsContributor.STOP_STREAM_LOG
        );
        am.registerGroup(WEBAPP_ACTIONS, webAppActionGroup);

        final ActionGroup deploymentSlotActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            AppServiceActionsContributor.OPEN_IN_BROWSER,
            ResourceCommonActionsContributor.SHOW_PROPERTIES,
            "---",
            SWAP_DEPLOYMENT_SLOT,
            "---",
            ResourceCommonActionsContributor.START,
            ResourceCommonActionsContributor.STOP,
            ResourceCommonActionsContributor.RESTART,
            ResourceCommonActionsContributor.DELETE,
            "---",
            AppServiceActionsContributor.START_STREAM_LOG,
            AppServiceActionsContributor.STOP_STREAM_LOG
        );
        am.registerGroup(DEPLOYMENT_SLOT_ACTIONS, deploymentSlotActionGroup);

        am.registerGroup(DEPLOYMENT_SLOTS_ACTIONS, new ActionGroup(REFRESH_DEPLOYMENT_SLOTS));

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_WEBAPP);
    }

    @Override
    public void registerActions(AzureActionManager am) {
        final Consumer<WebApp> refresh = webApp -> AzureEventBus.emit("appservice|webapp.slot.refresh", webApp);
        final ActionView.Builder refreshView = new ActionView.Builder("Refresh", AzureIcons.Action.REFRESH.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> description("webapp.list_deployments.app", ((WebApp) r).getName())).orElse(null))
                .enabled(s -> s instanceof WebApp);
        final Action<WebApp> refreshAction = new Action<>(REFRESH_DEPLOYMENT_SLOTS, refresh, refreshView);
        refreshAction.setShortcuts(am.getIDEDefaultShortcuts().refresh());
        am.registerAction(REFRESH_DEPLOYMENT_SLOTS, refreshAction);

        final Consumer<WebAppDeploymentSlot> swap = slot -> slot.getParent().swap(slot.getName());
        final ActionView.Builder swapView = new ActionView.Builder("Swap With Production")
            .title(s -> Optional.ofNullable(s).map(r -> description("webapp.swap_deployment.deployment|app",
                ((WebAppDeploymentSlot) s).getName(), ((WebAppDeploymentSlot) s).getParent().getName())).orElse(null))
            .enabled(s -> s instanceof WebAppDeploymentSlot && ((WebAppDeploymentSlot) s).getFormalStatus().isRunning());
        am.registerAction(SWAP_DEPLOYMENT_SLOT, new Action<>(SWAP_DEPLOYMENT_SLOT, swap, swapView));

        final ActionView.Builder createWebAppView = new ActionView.Builder("Web App")
            .title(s -> Optional.ofNullable(s).map(r -> description("webapp.create_app.group", ((ResourceGroup) r).getName())).orElse(null))
            .enabled(s -> s instanceof ResourceGroup);
        am.registerAction(GROUP_CREATE_WEBAPP, new Action<>(GROUP_CREATE_WEBAPP, createWebAppView));
    }

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<AzResource<?, ?, ?>, Object> startCondition = (r, e) -> r instanceof AppServiceAppBase &&
            StringUtils.equals(r.getStatus(), AzResource.Status.STOPPED);
        final BiConsumer<AzResource<?, ?, ?>, Object> startHandler = (c, e) -> ((AppServiceAppBase<?, ?, ?>) c).start();
        am.registerHandler(ResourceCommonActionsContributor.START, startCondition, startHandler);

        final BiPredicate<AzResource<?, ?, ?>, Object> stopCondition = (r, e) -> r instanceof AppServiceAppBase &&
            StringUtils.equals(r.getStatus(), AzResource.Status.RUNNING);
        final BiConsumer<AzResource<?, ?, ?>, Object> stopHandler = (c, e) -> ((AppServiceAppBase<?, ?, ?>) c).stop();
        am.registerHandler(ResourceCommonActionsContributor.STOP, stopCondition, stopHandler);

        final BiPredicate<AzResource<?, ?, ?>, Object> restartCondition = (r, e) -> r instanceof AppServiceAppBase &&
            StringUtils.equals(r.getStatus(), AzResource.Status.RUNNING);
        final BiConsumer<AzResource<?, ?, ?>, Object> restartHandler = (c, e) -> ((AppServiceAppBase<?, ?, ?>) c).restart();
        am.registerHandler(ResourceCommonActionsContributor.RESTART, restartCondition, restartHandler);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER; //after azure resource common actions registered
    }
}
