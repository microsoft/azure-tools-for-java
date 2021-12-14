/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.function;

import com.microsoft.azure.toolkit.ide.appservice.AppServiceActionsContributor;
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
    public static final Action.Id<Void> DOWNLOAD_CORE_TOOLS = Action.Id.of("action.function.download_core_tools");
    public static final Action.Id<Void> CONFIG_CORE_TOOLS = Action.Id.of("action.function.config_core_tools");
    public static final String CORE_TOOLS_URL = "https://aka.ms/azfunc-install";

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

        am.registerGroup(FUNCTION_ACTION, new ActionGroup(FunctionAppActionsContributor.TRIGGER_FUNCTION));
        am.registerGroup(FUNCTIONS_ACTIONS, new ActionGroup(FunctionAppActionsContributor.REFRESH_FUNCTIONS));
    }

    @Override
    public void registerActions(AzureActionManager am) {
        final Consumer<FunctionApp> refresh = functionApp -> AzureEventBus.emit("appservice|function.functions.refresh", functionApp);
        final ActionView.Builder refreshView = new ActionView.Builder("Refresh", "/icons/action/refresh.svg")
                .title(s -> Optional.ofNullable(s).map(r -> title("appservice|function.functions.refresh", ((FunctionApp) r).name())).orElse(null))
                .enabled(s -> s instanceof FunctionApp);
        am.registerAction(REFRESH_FUNCTIONS, new Action<>(refresh, refreshView));

        final ActionView.Builder triggerView = new ActionView.Builder("Trigger Function")
                .title(s -> Optional.ofNullable(s).map(r -> title("appservice|function.function.trigger", ((FunctionEntity) s).getName())).orElse(null))
                .enabled(s -> s instanceof FunctionEntity);
        am.registerAction(TRIGGER_FUNCTION, new Action<>(triggerView));

        final ActionView.Builder downloadCliView = new ActionView.Builder("Download")
                .title(s -> Optional.ofNullable(s).map(r -> title("function.download_core_tools")).orElse(null));
        final Action<Void> downloadCliAction = new Action<>((v) -> {
            am.getAction(ResourceCommonActionsContributor.OPEN_URL).handle(CORE_TOOLS_URL);
        }, downloadCliView);
        downloadCliAction.authRequired(false);
        am.registerAction(DOWNLOAD_CORE_TOOLS, downloadCliAction);

        final ActionView.Builder configCliView = new ActionView.Builder("Configure")
                .title(s -> Optional.ofNullable(s).map(r -> title("function.config_core_tools")).orElse(null));
        final Action<Void> configCliAction = new Action<>((v) -> {
            // TODO: @wangmi open Azure Settings dialog.
        }, configCliView);
        configCliAction.authRequired(false);
        am.registerAction(CONFIG_CORE_TOOLS, configCliAction);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER; //after azure resource common actions registered
    }
}
