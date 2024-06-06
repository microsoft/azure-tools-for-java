/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppModule;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask;

import javax.annotation.Nullable;
import java.util.Objects;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class CreateContainerAppAction {
    public static void create(@Nullable Project project, @Nullable ContainerAppDraft.Config data) {
        AzureTaskManager.getInstance().runLater(() -> {
            final ContainerAppCreationDialog dialog = new ContainerAppCreationDialog(project);
            if (Objects.nonNull(data)) {
                dialog.getForm().setValue(data);
            }
            final Action.Id<ContainerAppDraft.Config> actionId = Action.Id.of("user/containerapps.create_container_app.app");
            dialog.setOkAction(new Action<>(actionId)
                .withLabel("Create")
                .withIdParam(ContainerAppDraft.Config::getName)
                .withSource(ContainerAppDraft.Config::getResourceGroup)
                .withAuthRequired(true)
                .withHandler(draft -> doCreate(draft, project)));
            dialog.show();
        });
    }

    public static ContainerAppDraft doCreate(final ContainerAppDraft.Config config, final Project project) {
        final ResourceGroup rg = config.getResourceGroup();
        if (rg.isDraftForCreating()) {
            new CreateResourceGroupTask(rg.getSubscriptionId(), rg.getName(), config.getEnvironment().getRegion()).execute();
        }
        final ContainerAppModule module = az(AzureContainerApps.class).containerApps(config.getSubscription().getId());
        final ContainerAppDraft draft = module.create(config.getName(), config.getResourceGroup().getName());
        CacheManager.getUsageHistory(ContainerApp.class).push(draft);
        draft.setConfig(config);
        draft.commit();
        return draft;
    }
}
