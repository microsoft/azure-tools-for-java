/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.arm;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.auth.IAccountActions;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;

public class ResourceGroupActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String APPCENTRIC_RESOURCE_GROUPS_ACTIONS = "actions.resourceGroups.appCentric";
    public static final String TYPECENTRIC_RESOURCE_GROUPS_ACTIONS = "actions.resourceGroups.typeCentric";
    public static final String RESOURCE_GROUP_ACTIONS = "actions.resourceGroups.group";

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup appCentricResourceGroupsActions = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            "---",
            IAccountActions.SELECT_SUBS,
            ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(APPCENTRIC_RESOURCE_GROUPS_ACTIONS, appCentricResourceGroupsActions);

        final ActionGroup typeCentricResourceGroupsActions = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            "---",
            ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(TYPECENTRIC_RESOURCE_GROUPS_ACTIONS, typeCentricResourceGroupsActions);

        final ActionGroup groupActions = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            "---",
            ResourceCommonActionsContributor.DELETE,
            ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS // TODO: create any resource in this resource group.
        );
        am.registerGroup(RESOURCE_GROUP_ACTIONS, groupActions);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
