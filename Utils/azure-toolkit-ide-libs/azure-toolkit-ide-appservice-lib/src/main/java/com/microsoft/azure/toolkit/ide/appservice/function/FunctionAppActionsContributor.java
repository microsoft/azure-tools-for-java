/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.function;

import com.microsoft.azure.toolkit.ide.appservice.AppServiceActionsContributor;
import com.microsoft.azure.toolkit.ide.appservice.function.node.TriggerFunctionInBrowserAction;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.appservice.entity.FunctionEntity;
import com.microsoft.azure.toolkit.lib.appservice.service.impl.FunctionApp;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;

import java.util.Optional;
import java.util.function.Consumer;

import static com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle.title;

public class FunctionAppActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = AppServiceActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.function.service";
    public static final String FUNCTION_APP_ACTIONS = "actions.function.function_app";
    public static final String FUNCTIONS_ACTIONS = "actions.function.functions";
    public static final String FUNCTION_ACTION = "actions.function.function";

    public static final Action.Id<FunctionApp> REFRESH_FUNCTIONS = Action.Id.of("actions.function.functions.refresh");
    public static final Action.Id<FunctionEntity> TRIGGER_FUNCTION = Action.Id.of("actions.function.function.trigger");
    public static final Action.Id<FunctionEntity> TRIGGER_FUNCTION_IN_BROWSER = Action.Id.of("actions.function.function.trigger_in_browser");

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.SERVICE_REFRESH,
                ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup functionAppActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_PORTAL_URL,
                ResourceCommonActionsContributor.DEPLOY,
                ResourceCommonActionsContributor.SHOW_PROPERTIES,
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

        am.registerGroup(FUNCTION_ACTION, new ActionGroup(FunctionAppActionsContributor.TRIGGER_FUNCTION,
                FunctionAppActionsContributor.TRIGGER_FUNCTION_IN_BROWSER));
        am.registerGroup(FUNCTIONS_ACTIONS, new ActionGroup(FunctionAppActionsContributor.REFRESH_FUNCTIONS));
    }

    @Override
    public void registerActions(AzureActionManager am) {
        final Consumer<FunctionApp> refresh = functionApp -> AzureEventBus.emit("appservice|function.functions.refresh", functionApp);
        final ActionView.Builder refreshView = new ActionView.Builder("Refresh", "/icons/action/refresh.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("function.refresh_funcs")).orElse(null))
                .enabled(s -> s instanceof FunctionApp);
        am.registerAction(REFRESH_FUNCTIONS, new Action<>(refresh, refreshView));

        final ActionView.Builder triggerView = new ActionView.Builder("Trigger Function")
                .title(s -> Optional.ofNullable(s).map(r -> title("function.trigger_func.app", ((FunctionEntity) s).getName())).orElse(null))
                .enabled(s -> s instanceof FunctionEntity);
        am.registerAction(TRIGGER_FUNCTION, new Action<>(triggerView));

        final Consumer<FunctionEntity> triggerInBrowserHandler = entity -> new TriggerFunctionInBrowserAction(entity).trigger();
        final ActionView.Builder triggerInBrowserView = new ActionView.Builder("Trigger Function In Browser")
                .title(s -> Optional.ofNullable(s).map(r -> title("function.trigger_func_http.app", ((FunctionEntity) s).getName())).orElse(null))
                .enabled(s -> s instanceof FunctionEntity && TriggerFunctionInBrowserAction.isApplyFor((FunctionEntity) s));
        am.registerAction(TRIGGER_FUNCTION_IN_BROWSER, new Action<>(triggerInBrowserHandler, triggerInBrowserView));
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER; //after azure resource common actions registered
    }
}
