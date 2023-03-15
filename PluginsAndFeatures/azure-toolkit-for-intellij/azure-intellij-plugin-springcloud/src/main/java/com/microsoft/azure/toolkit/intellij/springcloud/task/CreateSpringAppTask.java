/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.task;

import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Phase;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.ide.guidance.view.components.PhasePanel;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.azure.toolkit.lib.springcloud.task.DeploySpringCloudAppTask;

import javax.annotation.Nonnull;
import java.util.Objects;

public class CreateSpringAppTask implements Task {
    public static final String SPRING_APP_CLUSTER = "springAppCluster";
    public static final String SPRING_APP = "springApp";
    public static final String SPRING_APP_NAME = "springAppName";
    public static final String DEFAULT_SPRING_APP_NAME = "defaultSpringAppName";
    private final ComponentContext context;

    public CreateSpringAppTask(@Nonnull final ComponentContext context) {
        this.context = context;
        this.init();
    }

    @Override
    public void prepare() {
        final Phase currentPhase = context.getCourse().getCurrentPhase();
        currentPhase.expandPhasePanel();
    }

    @Override
    public void execute() {
        final SpringCloudCluster cluster = (SpringCloudCluster) Objects.requireNonNull(context.getParameter(SPRING_APP_CLUSTER), "`cluster` is required to create spring app");
        final String name = (String) Objects.requireNonNull(context.getParameter(SPRING_APP_NAME), "`name` is required to create spring app");
        final SpringCloudDeploymentConfig deploymentConfig = SpringCloudDeploymentConfig.builder().runtimeVersion("Java 17").instanceCount(1).build();
        final SpringCloudAppConfig config = SpringCloudAppConfig.builder()
                .appName(name)
                .subscriptionId(cluster.getSubscriptionId())
                .clusterName(cluster.getName())
                .resourceGroup(cluster.getResourceGroupName())
                .runtimeVersion("Java 17")
                .isPublic(true)
                .deployment(deploymentConfig).build();
        final SpringCloudDeployment deployment = new DeploySpringCloudAppTask(config).execute();
        final SpringCloudApp parent = deployment.getParent();
        AzureMessager.getMessager().info(AzureString.format("Azure Spring app %s is successfully created.", name));
        context.applyResult(SPRING_APP, parent);
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.springcloud.create";
    }

    private void init() {
        final String defaultSpringAppName = Utils.generateRandomResourceName("app", 40);
        context.applyResult(DEFAULT_SPRING_APP_NAME, defaultSpringAppName);
    }
}
