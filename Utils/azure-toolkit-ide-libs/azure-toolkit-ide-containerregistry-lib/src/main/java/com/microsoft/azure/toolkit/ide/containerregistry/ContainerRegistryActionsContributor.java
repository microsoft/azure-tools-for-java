/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.containerregistry;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;

import java.util.Optional;

import static com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle.title;

public class ContainerRegistryActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.registry.service";
    public static final String REGISTRY_ACTIONS = "actions.registry.registry";

    public static final Action.Id<ContainerRegistry> PUSH_IMAGE = Action.Id.of("action.acr.push_image");

    @Override
    public void registerActions(AzureActionManager am) {
        final ActionView.Builder pushImageView = new ActionView.Builder("Push Image")
                .title(s -> Optional.ofNullable(s).map(r -> title("acr.push_image.acr", ((ContainerRegistry) r).name())).orElse(null))
                .enabled(s -> s instanceof ContainerRegistry);
        am.registerAction(PUSH_IMAGE, new Action<>(pushImageView));
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.SERVICE_REFRESH
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup accountActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.OPEN_PORTAL_URL,
                "---",
                ContainerRegistryActionsContributor.PUSH_IMAGE,
                "---",
                ResourceCommonActionsContributor.SHOW_PROPERTIES
        );
        am.registerGroup(REGISTRY_ACTIONS, accountActionGroup);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}

