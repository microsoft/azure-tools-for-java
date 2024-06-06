/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.guidance;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Context;
import com.microsoft.azure.toolkit.ide.guidance.config.InputConfig;
import com.microsoft.azure.toolkit.ide.guidance.input.GuidanceInput;
import com.microsoft.azure.toolkit.ide.guidance.input.GuidanceInputProvider;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironmentDraft;
import lombok.Getter;

import javax.annotation.Nonnull;

import static com.microsoft.azure.toolkit.ide.guidance.task.SelectSubscriptionTask.SUBSCRIPTION;

public class ContainerAppInputProvider implements GuidanceInputProvider {
    @Override
    public GuidanceInput<?> createInputComponent(@Nonnull InputConfig config, @Nonnull Context context) {
        final ComponentContext inputContext = new ComponentContext(config, context);
        return switch (config.getName()) {
            case "input.containerapp.env" -> new ContainerAppEnvInputWrapper(config, inputContext);
            case "input.containerapp.name" -> new ContainerAppNameInputWrapper(config, inputContext);
            default -> null;
        };
    }

    public static class ContainerAppEnvInputWrapper implements GuidanceInput<ContainerAppsEnvironment> {
        public static final String CONTAINER_APP_ENV = "containerAppEnv";

        private final ComponentContext context;
        @Getter
        private final ContainerAppEnvInput component;
        @Getter
        private final String description;

        public ContainerAppEnvInputWrapper(final InputConfig config, final ComponentContext context) {
            this.context = context;
            this.component = new ContainerAppEnvInput();
            this.description = config.getDescription();
            this.component.setSubscription((Subscription) context.getParameter(SUBSCRIPTION));
            context.addPropertyListener(SUBSCRIPTION, v -> component.setSubscription((Subscription) v));
            context.addPropertyListener(CONTAINER_APP_ENV, v -> component.setValue((ContainerAppsEnvironment) v));
        }

        @Override
        public void applyResult() {
            final ContainerAppsEnvironment env = component.getValue();
            context.applyResult(CONTAINER_APP_ENV, env);
            context.applyResult(CreateResourceGroupTask.RESOURCE_GROUP, env.getResourceGroup());
            if (env.isDraftForCreating() && env instanceof ContainerAppsEnvironmentDraft draft) {
                context.applyResult(CreateLogWorkspaceTask.LOG_ANALYTICS_WORKSPACE, draft.getConfig().getLogAnalyticsWorkspace());
            } else {
                context.applyResult(CreateLogWorkspaceTask.LOG_ANALYTICS_WORKSPACE, null);
            }
        }
    }

    public static class ContainerAppNameInputWrapper implements GuidanceInput<String> {
        public static final String CONTAINER_APP_NAME = "containerAppName";
        public static final String CONTAINER_APP_ENV = "containerAppEnv";
        private final ComponentContext context;
        @Getter
        private final ContainerAppNameInput component;
        @Getter
        private final String description;

        public ContainerAppNameInputWrapper(final InputConfig config, final ComponentContext context) {
            this.context = context;
            this.component = new ContainerAppNameInput();
            this.description = config.getDescription();
            this.component.setValue((String) context.getParameter(CONTAINER_APP_NAME));
            this.component.setEnv((ContainerAppsEnvironment) context.getParameter(CONTAINER_APP_ENV));
            context.addPropertyListener(CONTAINER_APP_ENV, env -> this.component.setEnv((ContainerAppsEnvironment) env));
            context.addPropertyListener(CONTAINER_APP_NAME, name -> this.component.setValue((String) name));
        }

        @Override
        public void applyResult() {
            context.applyResult(CONTAINER_APP_NAME, this.component.getValue());
        }
    }
}

