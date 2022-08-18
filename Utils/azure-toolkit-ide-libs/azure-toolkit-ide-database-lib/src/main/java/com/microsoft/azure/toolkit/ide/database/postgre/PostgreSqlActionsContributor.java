/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.database.postgre;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResourceBase;
import com.microsoft.azure.toolkit.lib.postgre.PostgreSqlServer;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.common.operation.OperationBundle.description;

public class PostgreSqlActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.postgre.service";
    public static final String SERVER_ACTIONS = "actions.postgre.server";

    private static final String NAME_PREFIX = "PostgreSQL Server - %s";
    public static final Action.Id<AzResource<?, ?, ?>> OPEN_DATABASE_TOOL = Action.Id.of("postgre.open_database_tool");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_POSTGRE = Action.Id.of("group.create_postgre_server");

    @Override
    public void registerActions(AzureActionManager am) {
        final ActionView.Builder openDatabaseTool = new ActionView.Builder("Open with Database Tools", AzureIcons.Action.OPEN_DATABASE_TOOL.getIconPath())
            .title(s -> Optional.ofNullable(s).map(r -> description("postgre.connect_server.server", ((AzResource<?, ?, ?>) r).name())).orElse(null))
            .enabled(s -> s instanceof PostgreSqlServer && ((AzResourceBase) s).getFormalStatus().isRunning());
        final Action<AzResource<?, ?, ?>> action = new Action<>(OPEN_DATABASE_TOOL, openDatabaseTool);
        action.setShortcuts("control alt D");
        am.registerAction(OPEN_DATABASE_TOOL, action);

        final ActionView.Builder createServerView = new ActionView.Builder("PostgreSQL server")
            .title(s -> Optional.ofNullable(s).map(r -> description("postgre.create_server.group", ((ResourceGroup) r).getName())).orElse(null))
            .enabled(s -> s instanceof ResourceGroup);
        am.registerAction(GROUP_CREATE_POSTGRE, new Action<>(GROUP_CREATE_POSTGRE, createServerView));
    }

    public int getOrder() {
        return INITIALIZE_ORDER;
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

        final ActionGroup serverActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            ResourceCommonActionsContributor.SHOW_PROPERTIES,
            "---",
            PostgreSqlActionsContributor.OPEN_DATABASE_TOOL,
            ResourceCommonActionsContributor.CONNECT,
            "---",
            ResourceCommonActionsContributor.RESTART,
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(SERVER_ACTIONS, serverActionGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_POSTGRE);
    }
}
