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

import static com.microsoft.azure.toolkit.lib.common.operation.OperationBundle.description;

public class ContainerRegistryActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.registry.service";
    public static final String REGISTRY_ACTIONS = "actions.registry.registry";

    public static final Action.Id<ContainerRegistry> PUSH_IMAGE = Action.Id.of("acr.push_image");

    @Override
    public void registerActions(AzureActionManager am) {
        final ActionView.Builder pushImageView = new ActionView.Builder("Push Image")
                .title(s -> Optional.ofNullable(s).map(r -> description("acr.push_image.acr", ((ContainerRegistry) r).name())).orElse(null))
                .enabled(s -> s instanceof ContainerRegistry && ((ContainerRegistry) s).getFormalStatus().isRunning());
        am.registerAction(PUSH_IMAGE, new Action<>(PUSH_IMAGE, pushImageView));
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup registryActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            ResourceCommonActionsContributor.SHOW_PROPERTIES,
            "---",
            ContainerRegistryActionsContributor.PUSH_IMAGE
        );
        am.registerGroup(REGISTRY_ACTIONS, registryActionGroup);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}

