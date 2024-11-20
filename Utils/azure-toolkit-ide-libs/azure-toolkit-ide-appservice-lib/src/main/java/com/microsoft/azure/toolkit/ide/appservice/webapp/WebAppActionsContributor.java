/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.webapp;

import com.azure.resourcemanager.appservice.models.DeploymentSlot;
import com.microsoft.azure.toolkit.ide.appservice.AppServiceActionsContributor;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.lang3.StringUtils;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public class WebAppActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = AppServiceActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.webapp.service";
    public static final String WEBAPP_ACTIONS = "actions.webapp.management";
    public static final String DEPLOYMENT_SLOTS_ACTIONS = "actions.webapp.deployment_slots";
    public static final String DEPLOYMENT_SLOT_ACTIONS = "actions.webapp.deployment_slot";
    public static final Action.Id<WebAppDeploymentSlot> SWAP_DEPLOYMENT_SLOT = Action.Id.of("user/webapp.swap_deployment.deployment|app");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_WEBAPP = Action.Id.of("user/webapp.create_app.group");

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            "---",
            ResourceCommonActionsContributor.GETTING_STARTED,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            ResourceCommonActionsContributor.BROWSE_SERVICE_AZURE_SAMPLES,
            "---",
            ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup webAppActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            ResourceCommonActionsContributor.BROWSE_SERVICE_AZURE_SAMPLES,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            AppServiceActionsContributor.OPEN_IN_BROWSER,
            ResourceCommonActionsContributor.SHOW_PROPERTIES,
            "---",
            ResourceCommonActionsContributor.DEPLOY,
            AppServiceActionsContributor.UPDATE_IMAGE,
            "---",
            ResourceCommonActionsContributor.START,
            ResourceCommonActionsContributor.STOP,
            ResourceCommonActionsContributor.RESTART,
            ResourceCommonActionsContributor.DELETE,
            "---",
            AppServiceActionsContributor.PROFILE_FLIGHT_RECORD,
            AppServiceActionsContributor.SSH_INTO_WEBAPP,
            AppServiceActionsContributor.START_STREAM_LOG,
            AppServiceActionsContributor.STOP_STREAM_LOG,
            AppServiceActionsContributor.OPEN_LOGS_IN_MONITOR
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

        am.registerGroup(DEPLOYMENT_SLOTS_ACTIONS, new ActionGroup(ResourceCommonActionsContributor.REFRESH));

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_WEBAPP);
    }

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(SWAP_DEPLOYMENT_SLOT)
            .withLabel("Swap With Production")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof WebAppDeploymentSlot)
            .enableWhen(s -> s.getFormalStatus().isRunning())
            .register(am);

        new Action<>(GROUP_CREATE_WEBAPP)
            .withLabel("Web App")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ResourceGroup)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);
    }

    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<AzResource, Object> startCondition = (r, e) -> r instanceof AppServiceAppBase &&
            StringUtils.equals(r.getStatus(), AzResource.Status.STOPPED);
        final BiConsumer<AzResource, Object> startHandler = (c, e) -> ((AppServiceAppBase<?, ?, ?>) c).start();
        am.registerHandler(ResourceCommonActionsContributor.START, startCondition, startHandler);

        final BiPredicate<AzResource, Object> stopCondition = (r, e) -> r instanceof AppServiceAppBase &&
            StringUtils.equals(r.getStatus(), AzResource.Status.RUNNING);
        final BiConsumer<AzResource, Object> stopHandler = (c, e) -> ((AppServiceAppBase<?, ?, ?>) c).stop();
        am.registerHandler(ResourceCommonActionsContributor.STOP, stopCondition, stopHandler);

        final BiPredicate<AzResource, Object> restartCondition = (r, e) -> r instanceof AppServiceAppBase &&
            StringUtils.equals(r.getStatus(), AzResource.Status.RUNNING);
        final BiConsumer<AzResource, Object> restartHandler = (c, e) -> ((AppServiceAppBase<?, ?, ?>) c).restart();
        am.registerHandler(ResourceCommonActionsContributor.RESTART, restartCondition, restartHandler);

        final BiPredicate<WebAppDeploymentSlot, Object> swapDeploymentSlotCondition = (r, e) -> r != null &&
                StringUtils.equals(r.getStatus(), AzResource.Status.RUNNING);
        final BiConsumer<WebAppDeploymentSlot, Object> swapDeploymentSlotHandler = (c, e) -> {
            final DeploymentSlot deploymentSLot = c.getRemote();
            if (deploymentSLot != null) {
                deploymentSLot.swap("production");
            }
        };
        am.registerHandler(SWAP_DEPLOYMENT_SLOT, swapDeploymentSlotCondition, swapDeploymentSlotHandler);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER; //after azure resource common actions registered
    }
}
