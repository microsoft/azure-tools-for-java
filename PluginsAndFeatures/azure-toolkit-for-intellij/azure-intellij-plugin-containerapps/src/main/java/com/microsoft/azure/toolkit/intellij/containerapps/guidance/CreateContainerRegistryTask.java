/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.guidance;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Phase;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistryModule;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistryDraft;
import com.microsoft.azure.toolkit.lib.containerregistry.model.Sku;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Optional;

import static com.microsoft.azure.toolkit.ide.guidance.ComponentContext.PARAM_PREFIX;
import static com.microsoft.azure.toolkit.intellij.containerapps.guidance.CreateResourceGroupTask.RESOURCE_GROUP;

public class CreateContainerRegistryTask implements Task {
    public static final String CONTAINER_REGISTRY = "containerRegistry";
    private final ComponentContext context;

    @Getter
    private boolean toSkip = false;

    public CreateContainerRegistryTask(final ComponentContext context) {
        this.context = context;
    }

    @Override
    public void prepare() {
        final Optional<ContainerRegistry> contextRegistry = Optional.ofNullable((ContainerRegistry) context.getParameter(CONTAINER_REGISTRY));
        this.toSkip = contextRegistry.filter(rg -> !rg.isDraftForCreating()).isPresent();
        if (!this.toSkip) {
            final Phase currentPhase = context.getCourse().getCurrentPhase();
            currentPhase.expandPhasePanel();
        }
    }

    @Override
    @AzureOperation(name = "internal/guidance.create_container_registry")
    public void execute() {
        final ContainerRegistry registry = getOrCreateContainerRegistryDraft();
        if (registry.isDraftForCreating() && registry instanceof ContainerRegistryDraft registryDraft) {
            registryDraft.createIfNotExist();
            context.applyResult(CONTAINER_REGISTRY, registryDraft);
        }
    }

    private @Nonnull ContainerRegistry getOrCreateContainerRegistryDraft() {
        final Optional<ContainerRegistry> contextRg = Optional.ofNullable((ContainerRegistry) context.getParameter(CONTAINER_REGISTRY));
        if (contextRg.isPresent()) {
            return contextRg.get();
        }

        final String prefix = Optional.ofNullable((String) this.context.getParameter(PARAM_PREFIX))
                .or(() -> Optional.of(context.getCourse().getName()))
                .orElse("getstarted");
        final String name = String.format("%s-acr-%s", prefix, context.getCourse().getTimestamp()).replaceAll("\\W", "");
        final ResourceGroup rg = Optional.ofNullable((ResourceGroup) context.getParameter(RESOURCE_GROUP))
                .orElseThrow(() -> new AzureToolkitRuntimeException("'resourceGroup' is required"));

        final AzureContainerRegistryModule registryModule = Azure.az(AzureContainerRegistry.class)
                .registry(rg.getSubscriptionId());
        final ContainerRegistryDraft registryDraft = registryModule.create(name, rg.getName());
        registryDraft.setSku(Sku.Standard);
        registryDraft.setRegion(Region.US_EAST);
        registryDraft.setAdminUserEnabled(true);
        return registryDraft;
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.containerapp.create_container_registry";
    }
}
