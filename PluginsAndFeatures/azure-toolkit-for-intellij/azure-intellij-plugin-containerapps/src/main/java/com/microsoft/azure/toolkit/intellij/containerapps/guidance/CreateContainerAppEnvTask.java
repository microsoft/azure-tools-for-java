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
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironmentDraft;
import com.microsoft.azure.toolkit.lib.monitor.AzureLogAnalyticsWorkspace;
import com.microsoft.azure.toolkit.lib.monitor.LogAnalyticsWorkspaceDraft;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupDraft;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.Optional;

import static com.microsoft.azure.toolkit.ide.guidance.ComponentContext.PARAM_PREFIX;
import static com.microsoft.azure.toolkit.ide.guidance.task.SelectSubscriptionTask.SUBSCRIPTION;
import static com.microsoft.azure.toolkit.intellij.containerapps.guidance.CreateLogWorkspaceTask.LOG_ANALYTICS_WORKSPACE;
import static com.microsoft.azure.toolkit.intellij.containerapps.guidance.CreateResourceGroupTask.RESOURCE_GROUP;

public class CreateContainerAppEnvTask implements Task {
    public static final String CONTAINER_APP_ENV = "containerAppEnv";
    private final ComponentContext context;
    @Getter
    private boolean toSkip;

    public CreateContainerAppEnvTask(final ComponentContext context) {
        this.context = context;
        this.initParams(context);
    }

    private void initParams(final ComponentContext context) {
        this.context.addPropertyListener(SUBSCRIPTION, (v) -> {
            final String prefix = Optional.ofNullable((String) this.context.getParameter(PARAM_PREFIX))
                    .or(() -> Optional.of(this.context.getCourse().getName()))
                    .orElse("containerapp");
            final String defaultRgName = String.format("%s-rg-%s", prefix, this.context.getCourse().getTimestamp());
            final String defaultEnvName = String.format("%s-env-%s", prefix, this.context.getCourse().getTimestamp());
            final String defaultLogWorkspaceName = String.format("%s-logworkspace-%s", prefix, this.context.getCourse().getTimestamp());
            final Region region = Region.US_EAST;

            final Subscription subscription = Optional.ofNullable((Subscription) this.context.getParameter(SUBSCRIPTION))
                    .orElseThrow(() -> new AzureToolkitRuntimeException("Failed to get subscription to create application insights"));

            final ResourceGroupDraft rgDraft = Azure.az(AzureResources.class)
                    .groups(subscription.getId())
                    .create(defaultRgName, defaultRgName);
            rgDraft.setRegion(region);

            final LogAnalyticsWorkspaceDraft workspaceDraft = Azure.az(AzureLogAnalyticsWorkspace.class)
                    .logAnalyticsWorkspaces(subscription.getId())
                    .create(defaultLogWorkspaceName, defaultRgName);
            workspaceDraft.setRegion(region);

            final ContainerAppsEnvironmentDraft envDraft = Azure.az(AzureContainerApps.class)
                    .environments(subscription.getId())
                    .create(defaultEnvName, defaultRgName);
            final ContainerAppsEnvironmentDraft.Config envConfig = new ContainerAppsEnvironmentDraft.Config();
            envConfig.setSubscription(subscription);
            envConfig.setResourceGroup(rgDraft);
            envConfig.setRegion(region);
            envConfig.setName(defaultEnvName);
            envConfig.setLogAnalyticsWorkspace(workspaceDraft);
            envDraft.setConfig(envConfig);

            this.context.initParameter(RESOURCE_GROUP, rgDraft);
            this.context.initParameter(LOG_ANALYTICS_WORKSPACE, workspaceDraft);
            this.context.initParameter(CONTAINER_APP_ENV, envDraft);
        });
    }

    @Override
    public void prepare() {
        final Optional<ContainerAppsEnvironment> contextRg = Optional.ofNullable((ContainerAppsEnvironment) context.getParameter(CONTAINER_APP_ENV));
        this.toSkip = contextRg.filter(rg -> !rg.isDraftForCreating()).isPresent();
        if (!this.toSkip) {
            final Phase currentPhase = context.getCourse().getCurrentPhase();
            currentPhase.expandPhasePanel();
        }
    }

    @Override
    @AzureOperation(name = "internal/guidance.create_container_app_env")
    public void execute() {
        final ContainerAppsEnvironment env = Optional.ofNullable((ContainerAppsEnvironment) context.getParameter(CONTAINER_APP_ENV))
                .orElseThrow(() -> new AzureToolkitRuntimeException("'environment' is not initialized"));
        if (env instanceof ContainerAppsEnvironmentDraft envDraft) {
            envDraft.createIfNotExist();
            context.applyResult(CONTAINER_APP_ENV, env);
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.containerapp.create_env";
    }
}
