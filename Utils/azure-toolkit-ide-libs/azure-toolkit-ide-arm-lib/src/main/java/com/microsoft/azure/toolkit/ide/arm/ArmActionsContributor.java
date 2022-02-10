/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.arm;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.resource.ResourceDeployment;

import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle.title;

public class ArmActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String RESOURCE_MANAGEMENT_ACTIONS = "actions.resourceManagement.service";
    public static final String RESOURCE_GROUP_ACTIONS = "actions.resourceManagement.group";
    public static final String RESOURCE_DEPLOYMENT_ACTIONS = "actions.resourceManagement.deployment";

    public static final Action.Id<ResourceDeployment> EDIT = Action.Id.of("action.resourceDeployment.edit");
    public static final Action.Id<ResourceDeployment> UPDATE = Action.Id.of("action.resourceDeployment.update");
    public static final Action.Id<ResourceDeployment> EXPORT_TEMPLATE = Action.Id.of("action.resourceDeployment.export_template");
    public static final Action.Id<ResourceDeployment> EXPORT_PARAMETER = Action.Id.of("action.resourceDeployment.export_parameter");

    @Override
    public void registerActions(AzureActionManager am) {
        final ActionView.Builder editDeployment = new ActionView.Builder("Edit Deployment", "/icons/action/edit")
            .title(s -> Optional.ofNullable(s).map(r -> title("resourceDeployment.edit.deployment", ((ResourceDeployment) r).getName())).orElse(null))
            .enabled(s -> s instanceof ResourceDeployment);
        final ActionView.Builder updateDeployment = new ActionView.Builder("Update Deployment", "/icons/action/update")
            .title(s -> Optional.ofNullable(s).map(r -> title("resourceDeployment.update.deployment", ((ResourceDeployment) r).getName())).orElse(null))
            .enabled(s -> s instanceof ResourceDeployment);
        final ActionView.Builder exportTemplate = new ActionView.Builder("Export Template File", "/icons/action/export")
            .title(s -> Optional.ofNullable(s).map(r -> title("resourceDeployment.export_template.deployment", ((ResourceDeployment) r).getName())).orElse(null))
            .enabled(s -> s instanceof ResourceDeployment);
        final ActionView.Builder exportParameter = new ActionView.Builder("Export Parameter File", "/icons/action/export")
            .title(s -> Optional.ofNullable(s).map(r -> title("resourceDeployment.export_parameter.deployment", ((ResourceDeployment) r).getName())).orElse(null))
            .enabled(s -> s instanceof ResourceDeployment);
        am.registerAction(EDIT, new Action<>(editDeployment));
        am.registerAction(UPDATE, new Action<>(updateDeployment));
        am.registerAction(EXPORT_TEMPLATE, new Action<>(exportTemplate));
        am.registerAction(EXPORT_PARAMETER, new Action<>(exportParameter));
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.SERVICE_REFRESH,
            ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(RESOURCE_MANAGEMENT_ACTIONS, serviceActionGroup);

        final ActionGroup groupActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            "---",
            ResourceCommonActionsContributor.CREATE,
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(RESOURCE_GROUP_ACTIONS, groupActionGroup);

        final ActionGroup deploymentActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            ResourceCommonActionsContributor.SHOW_PROPERTIES,
            "---",
            ArmActionsContributor.EDIT,
            ArmActionsContributor.UPDATE,
            ArmActionsContributor.EXPORT_TEMPLATE,
            ArmActionsContributor.EXPORT_PARAMETER,
            "---",
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(RESOURCE_DEPLOYMENT_ACTIONS, deploymentActionGroup);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
