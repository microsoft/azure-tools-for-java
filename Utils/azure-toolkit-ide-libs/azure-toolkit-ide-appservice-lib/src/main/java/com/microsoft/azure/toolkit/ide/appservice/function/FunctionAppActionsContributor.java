/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.function;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.toolkit.ide.appservice.AppServiceActionsContributor;
import com.microsoft.azure.toolkit.ide.appservice.function.node.TriggerFunctionInBrowserAction;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.containerapps.ContainerAppsActionsContributor;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS;

public class FunctionAppActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = Math.max(AppServiceActionsContributor.INITIALIZE_ORDER, ContainerAppsActionsContributor.INITIALIZE_ORDER) + 1;

    public static final String SERVICE_ACTIONS = "actions.function.service";
    public static final String FUNCTION_APP_ACTIONS = "actions.function.function_app";
    public static final String FUNCTIONS_ACTIONS = "actions.function.functions";
    public static final String FUNCTION_ACTION = "actions.function.function";
    public static final String DEPLOYMENT_SLOTS_ACTIONS = "actions.function.deployment_slots";
    public static final String DEPLOYMENT_SLOT_ACTIONS = "actions.function.deployment_slot";

    public static final Action.Id<FunctionAppBase<?, ?, ?>> ENABLE_REMOTE_DEBUGGING = Action.Id.of("user/function.enable_remote_debugging.app");
    public static final Action.Id<FunctionAppBase<?, ?, ?>> DISABLE_REMOTE_DEBUGGING = Action.Id.of("user/function.disable_remote_debugging.app");
    public static final Action.Id<FunctionAppBase<?, ?, ?>> REMOTE_DEBUGGING = Action.Id.of("user/function.start_remote_debugging.app");
    public static final Action.Id<FunctionAppDeploymentSlot> SWAP_DEPLOYMENT_SLOT = Action.Id.of("user/function.swap_deployment.deployment|app");
    public static final Action.Id<FunctionApp> REFRESH_FUNCTIONS = Action.Id.of("user/function.refresh_functions.app");
    public static final Action.Id<FunctionApp> FOCUS_CONTAINER_ENVIRONMENT = Action.Id.of("user/function.focus_environment.app");
    public static final Action.Id<FunctionEntity> TRIGGER_FUNCTION = FunctionEntity.TRIGGER_FUNCTION;
    public static final Action.Id<FunctionEntity> TRIGGER_FUNCTION_IN_BROWSER = FunctionEntity.TRIGGER_FUNCTION_IN_BROWSER;
    public static final Action.Id<FunctionEntity> TRIGGER_FUNCTION_WITH_HTTP_CLIENT = Action.Id.of("user/function.trigger_function_with_http_client.trigger");
    public static final Action.Id<Object> DOWNLOAD_CORE_TOOLS = Action.Id.of("user/function.download_core_tools");
    public static final Action.Id<Object> CONFIG_CORE_TOOLS = Action.Id.of("user/function.config_core_tools");
    public static final String CORE_TOOLS_URL = "https://aka.ms/azfunc-install";

    public static final Action.Id<ContainerAppsEnvironment> ENVIRONMENT_CREATE_FUNCTION = Action.Id.of("user/function.create_app.environment");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_FUNCTION = Action.Id.of("user/function.create_app.group");

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

        final ActionGroup functionAppActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            ResourceCommonActionsContributor.BROWSE_SERVICE_AZURE_SAMPLES,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            ResourceCommonActionsContributor.SHOW_PROPERTIES,
            FunctionAppActionsContributor.FOCUS_CONTAINER_ENVIRONMENT,
            "---",
            am.getAction(ResourceCommonActionsContributor.DEPLOY).bind(null)
                    .visibleWhen(s -> s instanceof FunctionApp app && StringUtils.isBlank(app.getEnvironmentId())),
            AppServiceActionsContributor.UPDATE_IMAGE,
            "---",
            FunctionAppActionsContributor.REMOTE_DEBUGGING,
            FunctionAppActionsContributor.ENABLE_REMOTE_DEBUGGING,
            FunctionAppActionsContributor.DISABLE_REMOTE_DEBUGGING,
            "---",
            ResourceCommonActionsContributor.START,
            am.getAction(ResourceCommonActionsContributor.STOP).bind(null)
                        .visibleWhen(s -> s instanceof FunctionApp app && StringUtils.isBlank(app.getEnvironmentId())),
            am.getAction(ResourceCommonActionsContributor.RESTART).bind(null)
                        .visibleWhen(s -> s instanceof FunctionApp app && StringUtils.isBlank(app.getEnvironmentId())),
            ResourceCommonActionsContributor.DELETE,
            "---",
            AppServiceActionsContributor.START_STREAM_LOG,
            AppServiceActionsContributor.STOP_STREAM_LOG,
            AppServiceActionsContributor.OPEN_LOGS_IN_MONITOR
            // todo: add profile actions like log streaming
        );
        am.registerGroup(FUNCTION_APP_ACTIONS, functionAppActionGroup);

        final ActionGroup slotActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
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
        am.registerGroup(DEPLOYMENT_SLOT_ACTIONS, slotActionGroup);

        am.registerGroup(DEPLOYMENT_SLOTS_ACTIONS, new ActionGroup(ResourceCommonActionsContributor.REFRESH));

        am.registerGroup(FUNCTION_ACTION, new ActionGroup(FunctionAppActionsContributor.TRIGGER_FUNCTION,
            FunctionAppActionsContributor.TRIGGER_FUNCTION_IN_BROWSER, FunctionAppActionsContributor.TRIGGER_FUNCTION_WITH_HTTP_CLIENT));
        am.registerGroup(FUNCTIONS_ACTIONS, new ActionGroup(FunctionAppActionsContributor.REFRESH_FUNCTIONS));

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_FUNCTION);

        final IActionGroup environment = am.getGroup(ContainerAppsActionsContributor.CONTAINER_APPS_ENVIRONMENT_CREATE_ACTIONS);
        environment.addAction(ENVIRONMENT_CREATE_FUNCTION);
    }

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(SWAP_DEPLOYMENT_SLOT)
            .visibleWhen(s -> s instanceof FunctionAppDeploymentSlot)
            .enableWhen(s -> s.getFormalStatus().isRunning())
            .withLabel("Swap with Production")
            .withIdParam(AbstractAzResource::getName)
            .register(am);

        new Action<>(REFRESH_FUNCTIONS)
            .withLabel("Refresh")
            .withIcon(AzureIcons.Action.REFRESH.getIconPath())
            .withIdParam(AbstractAzResource::getName)
            .visibleWhen(s -> s instanceof FunctionApp)
            .withShortcut(am.getIDEDefaultShortcuts().refresh())
            .withHandler(s -> AzureEventBus.emit("appservice|function.functions.refresh", s))
            .register(am);

        new Action<>(TRIGGER_FUNCTION)
            .withLabel("Trigger Function")
            .withIdParam(FunctionEntity::getName)
            .visibleWhen(s -> s instanceof FunctionEntity && !AzureFunctionsUtils.isHttpTrigger((FunctionEntity) s))
            .register(am);

        new Action<>(TRIGGER_FUNCTION_IN_BROWSER)
            .withLabel("Trigger Function In Browser")
            .withIdParam(FunctionEntity::getName)
            .visibleWhen(s -> s instanceof FunctionEntity && AzureFunctionsUtils.isHttpTrigger((FunctionEntity) s))
            .withHandler(s -> new TriggerFunctionInBrowserAction(s).trigger())
            .register(am);

        new Action<>(TRIGGER_FUNCTION_WITH_HTTP_CLIENT)
            .withLabel("Trigger Function with Http Client")
            .withIdParam(FunctionEntity::getName)
            .visibleWhen(s -> s instanceof FunctionEntity)
            .register(am);

        new Action<>(DOWNLOAD_CORE_TOOLS)
            .withLabel("Download")
            .withAuthRequired(false)
            .register(am);

        new Action<>(CONFIG_CORE_TOOLS)
            .withLabel("Configure")
            .withHandler((v, e) -> am.getAction(OPEN_AZURE_SETTINGS).handle(null, e))
            .withAuthRequired(false)
            .register(am);

        new Action<>(FOCUS_CONTAINER_ENVIRONMENT)
                .withLabel(s -> "Navigate to Environment")
                .withIcon(AzureIcons.ContainerApps.MODULE.getIconPath())
                .visibleWhen(s -> s instanceof FunctionApp app && StringUtils.isNotBlank(app.getEnvironmentId()))
                .withHandler((r) -> {
                    final AzResource resource = Azure.az().getById(r.getEnvironmentId());
                    if (Objects.isNull(resource)) {
                        final String environment = ResourceId.fromString(r.getEnvironmentId()).name();
                        AzureMessager.getMessager().info(AzureString.format("Cannot find Azure Container Apps environment (%s).", environment));
                        return;
                    }
                    AzureEventBus.emit("azure.explorer.select_resource", resource);
                })
                .withAuthRequired(false)
                .register(am);

        new Action<>(GROUP_CREATE_FUNCTION)
            .withLabel("Function App")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ResourceGroup)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .register(am);

        new Action<>(ENVIRONMENT_CREATE_FUNCTION)
            .withLabel("Function App")
            .withIdParam(AzResource::getName)
            .visibleWhen(resource -> resource instanceof ContainerAppsEnvironment)
            .enableWhen(resource -> resource.getFormalStatus().isConnected())
            .register(am);

        new Action<>(ENABLE_REMOTE_DEBUGGING)
            .withLabel("Enable Remote Debugging")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof FunctionAppBase<?, ?, ?> && ((FunctionAppBase<?, ?, ?>) s).getFormalStatus().isRunning() &&
                !((FunctionAppBase<?, ?, ?>) s).isRemoteDebugEnabled() && !(s instanceof FunctionApp functionApp && (StringUtils.isNotBlank(functionApp.getEnvironmentId()))))
            .register(am);

        new Action<>(DISABLE_REMOTE_DEBUGGING)
            .withLabel("Disable Remote Debugging")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof FunctionAppBase<?, ?, ?> && ((FunctionAppBase<?, ?, ?>) s).getFormalStatus().isRunning()
                && ((FunctionAppBase<?, ?, ?>) s).isRemoteDebugEnabled() && !(s instanceof FunctionApp functionApp && (StringUtils.isNotBlank(functionApp.getEnvironmentId()))))
            .register(am);

        new Action<>(REMOTE_DEBUGGING)
            .withLabel("Attach Debugger")
            .withIcon(AzureIcons.Action.ATTACH_DEBUGGER.getIconPath())
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof FunctionAppBase<?, ?, ?> && !(s instanceof FunctionApp functionApp && (StringUtils.isNotBlank(functionApp.getEnvironmentId()))))
            .enableWhen(s -> s.getFormalStatus().isRunning())
            .register(am);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER; //after azure resource common actions registered
    }
}
