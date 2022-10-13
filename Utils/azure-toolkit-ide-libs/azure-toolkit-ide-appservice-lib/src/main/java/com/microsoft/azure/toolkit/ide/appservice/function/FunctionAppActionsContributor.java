/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.function;

import com.microsoft.azure.toolkit.ide.appservice.AppServiceActionsContributor;
import com.microsoft.azure.toolkit.ide.appservice.function.node.TriggerFunctionInBrowserAction;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionApp;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppDeploymentSlotModule;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppDeploymentSlot;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import java.util.Optional;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.ide.appservice.AppServiceActionsContributor.REFRESH_DEPLOYMENT_SLOTS;
import static com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor.OPEN_AZURE_SETTINGS;
import static com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor.OPEN_URL;
import static com.microsoft.azure.toolkit.lib.common.operation.OperationBundle.description;

public class FunctionAppActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = AppServiceActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.function.service";
    public static final String FUNCTION_APP_ACTIONS = "actions.function.function_app";
    public static final String FUNCTIONS_ACTIONS = "actions.function.functions";
    public static final String FUNCTION_ACTION = "actions.function.function";
    public static final String DEPLOYMENT_SLOTS_ACTIONS = "actions.function.deployment_slots";
    public static final String DEPLOYMENT_SLOT_ACTIONS = "actions.function.deployment_slot";

    public static final Action.Id<FunctionAppDeploymentSlot> SWAP_DEPLOYMENT_SLOT = Action.Id.of("function.swap_deployment_slot");
    public static final Action.Id<FunctionApp> REFRESH_FUNCTIONS = Action.Id.of("function.refresh_functions");
    public static final Action.Id<FunctionEntity> TRIGGER_FUNCTION = Action.Id.of("function.trigger_function");
    public static final Action.Id<FunctionEntity> TRIGGER_FUNCTION_IN_BROWSER = Action.Id.of("function.trigger_in_browser");
    public static final Action.Id<FunctionEntity> TRIGGER_FUNCTION_WITH_HTTP_CLIENT = Action.Id.of("function.trigger_function_with_http_client");
    public static final Action.Id<Object> DOWNLOAD_CORE_TOOLS = Action.Id.of("function.download_core_tools");
    public static final Action.Id<Object> CONFIG_CORE_TOOLS = Action.Id.of("function.config_core_tools");
    public static final String CORE_TOOLS_URL = "https://aka.ms/azfunc-install";

    public static final Action.Id<ResourceGroup> GROUP_CREATE_FUNCTION = Action.Id.of("group.create_function");

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            "---",
            ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup functionAppActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            ResourceCommonActionsContributor.SHOW_PROPERTIES,
            "---",
            ResourceCommonActionsContributor.DEPLOY,
            "---",
            ResourceCommonActionsContributor.START,
            ResourceCommonActionsContributor.STOP,
            ResourceCommonActionsContributor.RESTART,
            ResourceCommonActionsContributor.DELETE,
            "---",
            AppServiceActionsContributor.START_STREAM_LOG,
            AppServiceActionsContributor.STOP_STREAM_LOG
            // todo: add profile actions like log streaming
        );
        am.registerGroup(FUNCTION_APP_ACTIONS, functionAppActionGroup);

        final ActionGroup slotActionGroup = new ActionGroup(
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
        am.registerGroup(DEPLOYMENT_SLOT_ACTIONS, slotActionGroup);

        am.registerGroup(DEPLOYMENT_SLOTS_ACTIONS, new ActionGroup(REFRESH_DEPLOYMENT_SLOTS));

        am.registerGroup(FUNCTION_ACTION, new ActionGroup(FunctionAppActionsContributor.TRIGGER_FUNCTION,
                FunctionAppActionsContributor.TRIGGER_FUNCTION_IN_BROWSER, FunctionAppActionsContributor.TRIGGER_FUNCTION_WITH_HTTP_CLIENT));
        am.registerGroup(FUNCTIONS_ACTIONS, new ActionGroup(FunctionAppActionsContributor.REFRESH_FUNCTIONS));

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_FUNCTION);
    }

    @Override
    public void registerActions(AzureActionManager am) {
        final Consumer<FunctionAppDeploymentSlot> swap = slot -> slot.getParent().swap(slot.getName());
        final ActionView.Builder swapView = new ActionView.Builder("Swap With Production")
                .title(s -> Optional.ofNullable(s).map(r -> description("function.swap_deployment.deployment|app",
                        ((FunctionAppDeploymentSlot) s).getName(), ((FunctionAppDeploymentSlot) s).getParent().getName())).orElse(null))
                .enabled(s -> s instanceof FunctionAppDeploymentSlot && ((FunctionAppDeploymentSlot) s).getFormalStatus().isRunning());
        am.registerAction(SWAP_DEPLOYMENT_SLOT, new Action<>(SWAP_DEPLOYMENT_SLOT, swap, swapView));

        final Consumer<FunctionApp> refresh = functionApp -> AzureEventBus.emit("appservice|function.functions.refresh", functionApp);
        final ActionView.Builder refreshView = new ActionView.Builder("Refresh", AzureIcons.Action.REFRESH.getIconPath())
                .title(s -> Optional.ofNullable(s).map(r -> description("function.refresh_funcs")).orElse(null))
                .enabled(s -> s instanceof FunctionApp);
        final Action<FunctionApp> refreshAction = new Action<>(REFRESH_FUNCTIONS, refresh, refreshView);
        refreshAction.setShortcuts(am.getIDEDefaultShortcuts().refresh());
        am.registerAction(REFRESH_FUNCTIONS, refreshAction);

        final ActionView.Builder triggerView = new ActionView.Builder("Trigger Function")
                .title(s -> Optional.ofNullable(s).map(r -> description("function.trigger_func.trigger", ((FunctionEntity) s).getName())).orElse(null))
                .enabled(s -> s instanceof FunctionEntity && !AzureFunctionsUtils.isHttpTrigger((FunctionEntity) s));
        am.registerAction(TRIGGER_FUNCTION, new Action<>(TRIGGER_FUNCTION, triggerView));

        final Consumer<FunctionEntity> triggerInBrowserHandler = entity -> new TriggerFunctionInBrowserAction(entity).trigger();
        final ActionView.Builder triggerInBrowserView = new ActionView.Builder("Trigger Function In Browser")
                .title(s -> Optional.ofNullable(s).map(r -> description("function.trigger_func_in_browser.trigger", ((FunctionEntity) s).getName())).orElse(null))
                .enabled(s -> s instanceof FunctionEntity && AzureFunctionsUtils.isHttpTrigger((FunctionEntity) s));
        am.registerAction(TRIGGER_FUNCTION_IN_BROWSER, new Action<>(TRIGGER_FUNCTION_IN_BROWSER, triggerInBrowserHandler, triggerInBrowserView));

        final ActionView.Builder triggerWIthHttpClientView = new ActionView.Builder("Trigger Function with Http Client")
                .title(s -> Optional.ofNullable(s).map(r -> description("function.trigger_function_with_http_client.trigger",
                        ((FunctionEntity) s).getName())).orElse(null))
                .enabled(s -> s instanceof FunctionEntity);
        am.registerAction(TRIGGER_FUNCTION_WITH_HTTP_CLIENT, new Action<>(TRIGGER_FUNCTION_WITH_HTTP_CLIENT, triggerWIthHttpClientView));

        final ActionView.Builder downloadCliView = new ActionView.Builder("Download")
                .title(s -> description("function.download_core_tools"));
        final Action<Object> downloadCliAction = new Action<>(DOWNLOAD_CORE_TOOLS, (v) -> am.getAction(OPEN_URL).handle(CORE_TOOLS_URL), downloadCliView);
        downloadCliAction.setAuthRequired(false);
        am.registerAction(DOWNLOAD_CORE_TOOLS, downloadCliAction);

        final ActionView.Builder configCliView = new ActionView.Builder("Configure")
                .title(s -> description("function.config_core_tools"));
        final Action<Object> configCliAction = new Action<>(CONFIG_CORE_TOOLS, (v, e) -> am.getAction(OPEN_AZURE_SETTINGS).handle(null, e), configCliView);
        configCliAction.setAuthRequired(false);
        am.registerAction(CONFIG_CORE_TOOLS, configCliAction);

        final ActionView.Builder createFunctionView = new ActionView.Builder("Function App")
            .title(s -> Optional.ofNullable(s).map(r -> description("function.create_app.group", ((ResourceGroup) r).getName())).orElse(null))
            .enabled(s -> s instanceof ResourceGroup && ((ResourceGroup) s).getFormalStatus().isConnected());
        am.registerAction(GROUP_CREATE_FUNCTION, new Action<>(GROUP_CREATE_FUNCTION, createFunctionView));
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER; //after azure resource common actions registered
    }
}
