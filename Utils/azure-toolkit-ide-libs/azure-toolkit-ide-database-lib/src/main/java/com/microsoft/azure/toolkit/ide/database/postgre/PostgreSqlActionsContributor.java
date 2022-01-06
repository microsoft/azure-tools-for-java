/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.database.postgre;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.entity.IAzureBaseResource;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServer;

import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle.title;

public class PostgreSqlActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.postgre.service";
    public static final String POSTGRE_ACTIONS = "actions.postgre.instance";

    private static final String NAME_PREFIX = "PostgreSQL Server - %s";
    public static final Action.Id<IAzureBaseResource<?, ?>> OPEN_DATABASE_TOOL = Action.Id.of("action.database.open_database_tool");

    @Override
    public void registerActions(AzureActionManager am) {
        final ActionView.Builder openDatabaseTool = new ActionView.Builder("Open by Database Tools")
                .title(s -> Optional.ofNullable(s).map(r -> title("postgre.connect_server.server", ((IAzureBaseResource<?, ?>) r).name())).orElse(null))
                .enabled(s -> s instanceof PostgreSqlServer);
        am.registerAction(OPEN_DATABASE_TOOL, new Action<>(openDatabaseTool));
    }

    public int getOrder() {
        return INITIALIZE_ORDER;
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.SERVICE_REFRESH,
                ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup postgreActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.OPEN_PORTAL_URL,
                ResourceCommonActionsContributor.SHOW_PROPERTIES,
                PostgreSqlActionsContributor.OPEN_DATABASE_TOOL,
                "---",
                ResourceCommonActionsContributor.CONNECT,
                ResourceCommonActionsContributor.RESTART,
                ResourceCommonActionsContributor.DELETE,
                "---",
                ResourceCommonActionsContributor.REFRESH
        );
        am.registerGroup(POSTGRE_ACTIONS, postgreActionGroup);
    }
}
