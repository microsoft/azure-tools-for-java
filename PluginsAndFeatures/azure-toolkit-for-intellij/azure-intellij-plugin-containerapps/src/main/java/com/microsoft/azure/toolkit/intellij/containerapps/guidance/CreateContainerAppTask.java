/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.guidance;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Phase;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.intellij.containerapps.component.QuickStartImageForm;
import com.microsoft.azure.toolkit.intellij.containerapps.creation.CreateContainerAppAction;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.model.IngressConfig;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.ide.guidance.ComponentContext.PARAM_PREFIX;
import static com.microsoft.azure.toolkit.intellij.containerapps.guidance.CreateContainerAppEnvTask.CONTAINER_APP_ENV;

public class CreateContainerAppTask implements Task {
    public static final String CONTAINER_APP = "containerApp";
    public static final String CONTAINER_APP_NAME = "containerAppName";
    private final ComponentContext context;

    public CreateContainerAppTask(final ComponentContext context) {
        this.context = context;
        this.initParams(context);
    }

    private void initParams(final ComponentContext context) {
        final String prefix = Optional.ofNullable((String) this.context.getParameter(PARAM_PREFIX))
                .or(() -> Optional.of(context.getCourse().getName()))
                .orElse("containerapp");
        final String defaultContainerAppName = String.format("%s-app-%s", prefix, context.getCourse().getTimestamp());
        context.initParameter(CONTAINER_APP_NAME, defaultContainerAppName);
    }

    @Override
    public void prepare() {
        final Phase currentPhase = context.getCourse().getCurrentPhase();
        currentPhase.expandPhasePanel();
    }

    @Override
    @AzureOperation(name = "internal/guidance.create_container_app")
    public void execute() {
        final ContainerAppDraft.Config creationConfig = this.getContainerAppCreationConfig();
        final ContainerAppDraft app = CreateContainerAppAction.doCreate(creationConfig, context.getProject());
        context.applyResult(CONTAINER_APP, app);
    }

    private ContainerAppDraft.Config getContainerAppCreationConfig() {
        final ContainerAppsEnvironment env = (ContainerAppsEnvironment) Objects.requireNonNull(context.getParameter(CONTAINER_APP_ENV), "`environment` is required to create container app");
        final String name = (String) Objects.requireNonNull(context.getParameter(CONTAINER_APP_NAME), "`name` is required to create container app");

        final ContainerAppDraft.ImageConfig imageConfig = new ContainerAppDraft.ImageConfig(QuickStartImageForm.QUICK_START_IMAGE);
        final IngressConfig ingressConfig = IngressConfig.builder().enableIngress(true).external(true).targetPort(80).build();
        final ContainerAppDraft.ScaleConfig scaleConfig = ContainerAppDraft.ScaleConfig.builder()
                .maxReplicas(10)
                .minReplicas(1)
                .build();

        final ContainerAppDraft.Config result = new ContainerAppDraft.Config();
        result.setSubscription(env.getSubscription());
        result.setResourceGroup(env.getResourceGroup());
        result.setEnvironment(env);
        result.setName(name);
        result.setIngressConfig(ingressConfig);
        result.setImageConfig(imageConfig);
        result.setScaleConfig(scaleConfig);
        return result;
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.containerapp.create_app";
    }
}
