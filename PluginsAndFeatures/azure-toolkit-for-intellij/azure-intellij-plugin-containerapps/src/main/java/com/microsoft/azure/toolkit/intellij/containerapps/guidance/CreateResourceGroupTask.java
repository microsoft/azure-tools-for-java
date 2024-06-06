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
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupDraft;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Optional;

import static com.microsoft.azure.toolkit.ide.guidance.ComponentContext.PARAM_PREFIX;
import static com.microsoft.azure.toolkit.ide.guidance.task.SelectSubscriptionTask.SUBSCRIPTION;

public class CreateResourceGroupTask implements Task {
    public static final String RESOURCE_GROUP = "resourceGroup";
    private final ComponentContext context;

    @Getter
    private boolean toSkip = false;

    public CreateResourceGroupTask(final ComponentContext context) {
        this.context = context;
    }

    @Override
    public void prepare() {
        final Optional<ResourceGroup> contextRg = Optional.ofNullable((ResourceGroup) context.getParameter(RESOURCE_GROUP));
        this.toSkip = contextRg.filter(rg -> !rg.isDraftForCreating()).isPresent();
        if (!this.toSkip) {
            final Phase currentPhase = context.getCourse().getCurrentPhase();
            currentPhase.expandPhasePanel();
        }
    }

    @Override
    @AzureOperation(name = "internal/guidance.create_resource_group")
    public void execute() {
        final ResourceGroup rg = getOrCreateResourceGroupDraft();
        if (rg.isDraftForCreating() && rg instanceof ResourceGroupDraft rgDraft) {
            rgDraft.createIfNotExist();
            context.applyResult(RESOURCE_GROUP, rgDraft);
        }
    }

    private @Nonnull ResourceGroup getOrCreateResourceGroupDraft() {
        final Optional<ResourceGroup> contextRg = Optional.ofNullable((ResourceGroup) context.getParameter(RESOURCE_GROUP));
        if (contextRg.isPresent()) {
            return contextRg.get();
        }

        final String prefix = Optional.ofNullable((String) this.context.getParameter(PARAM_PREFIX))
                .or(() -> Optional.of(context.getCourse().getName()))
                .orElse("getstarted");
        final String name = String.format("%s-rg-%s", prefix, context.getCourse().getTimestamp());
        final Region region = Region.US_EAST;
        final Subscription subscription = Optional.ofNullable((Subscription) context.getParameter(SUBSCRIPTION))
                .orElseThrow(() -> new AzureToolkitRuntimeException("Failed to get subscription to create resource group"));

        final ResourceGroupDraft rgDraft = Azure.az(AzureResources.class)
                .groups(subscription.getId())
                .create(name, name);
        rgDraft.setRegion(region);
        return rgDraft;
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.common.create_resource_group";
    }
}
