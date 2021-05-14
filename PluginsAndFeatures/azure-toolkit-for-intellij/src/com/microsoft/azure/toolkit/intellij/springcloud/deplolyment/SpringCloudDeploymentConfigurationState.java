/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.deplolyment;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureRunProfileState;
import com.microsoft.azure.toolkit.intellij.common.messager.IntellijAzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.Utils;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.task.DeploySpringCloudAppTask;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.RunProcessHandler;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

class SpringCloudDeploymentConfigurationState extends AzureRunProfileState<SpringCloudDeployment> {
    private static final int GET_URL_TIMEOUT = 60;
    private static final int GET_STATUS_TIMEOUT = 180;
    private static final String UPDATE_APP_WARNING = "It may take some moments for the configuration to be applied at server side!";
    private static final String GET_DEPLOYMENT_STATUS_TIMEOUT = "Deployment succeeded but the app is still starting, " +
        "you can check the app status from Azure Portal.";
    private static final String NOTIFICATION_TITLE = "Deploy Spring Cloud App";

    private final SpringCloudDeploymentConfiguration config;

    public SpringCloudDeploymentConfigurationState(Project project, SpringCloudDeploymentConfiguration configuration) {
        super(project);
        this.config = configuration;
    }

    @Override
    @AzureOperation(name = "springcloud|app.create_update", params = {"this.config.getAppConfig().getAppName()"}, type = AzureOperation.Type.ACTION)
    public SpringCloudDeployment executeSteps(@NotNull RunProcessHandler processHandler, @NotNull Operation operation) {
        final MyMessager messager = new MyMessager(msg -> this.setText(processHandler, msg));
        AzureMessager.getContext().setMessager(messager);
        final SpringCloudAppConfig appConfig = this.config.getAppConfig();
        final DeploySpringCloudAppTask task = new DeploySpringCloudAppTask(appConfig);
        final SpringCloudDeployment deployment = task.execute();
        // TODO: notify azure explorer to refresh
        final SpringCloudApp app = deployment.app();
        final SpringCloudCluster cluster = app.getCluster();
        if (!deployment.waitUntilReady(GET_STATUS_TIMEOUT)) {
            AzureMessager.getDefaultMessager().warning(GET_DEPLOYMENT_STATUS_TIMEOUT, NOTIFICATION_TITLE);
        }
        printPublicUrl(app, processHandler);
        return deployment;
    }

    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.SPRING_CLOUD, TelemetryConstants.CREATE_SPRING_CLOUD_APP);
    }

    @Override
    protected void onSuccess(SpringCloudDeployment result, @NotNull RunProcessHandler processHandler) {
        setText(processHandler, "Deploy succeed");
        processHandler.notifyComplete();
    }

    @Override
    protected Map<String, String> getTelemetryMap() {
        final Map<String, String> props = new HashMap<>();
        props.put("runtime", config.getAppConfig().getRuntimeVersion());
        props.put("subscriptionId", config.getAppConfig().getSubscriptionId());
        props.put("public", String.valueOf(config.getAppConfig().isPublic()));
        props.put("jvmOptions", String.valueOf(StringUtils.isNotEmpty(config.getAppConfig().getDeployment().getJvmOptions())));
        props.put("instanceCount", String.valueOf(config.getAppConfig().getDeployment().getInstanceCount()));
        props.put("memory", String.valueOf(config.getAppConfig().getDeployment().getMemoryInGB()));
        props.put("cpu", String.valueOf(config.getAppConfig().getDeployment().getCpu()));
        props.put("persistentStorage", String.valueOf(config.getAppConfig().getDeployment().getEnablePersistentStorage()));
        return props;
    }

    private void printPublicUrl(final SpringCloudApp app, @NotNull RunProcessHandler processHandler) {
        final IAzureMessager messager = AzureMessager.getMessager();
        if (!app.entity().isPublic()) {
            return;
        }
        messager.info(String.format("Getting public url of app(%s)...", app.name()));
        String publicUrl = app.entity().getApplicationUrl();
        if (StringUtils.isEmpty(publicUrl)) {
            publicUrl = Utils.pollUntil(() -> app.refresh().entity().getApplicationUrl(), StringUtils::isNotBlank, GET_URL_TIMEOUT);
        }
        if (StringUtils.isEmpty(publicUrl)) {
            messager.warning("Failed to get application url", NOTIFICATION_TITLE);
        } else {
            messager.info(String.format("Application url: %s", publicUrl));
        }
    }

    @RequiredArgsConstructor
    private static class MyMessager extends IntellijAzureMessager {
        private final Consumer<String> out;

        @Override
        public void info(@Nonnull String message, String title) {
            out.accept(message);
        }

        @Override
        public void success(@Nonnull String message, String title) {
            out.accept(message);
            super.success(message, title);
        }

        @Override
        public void error(@Nonnull String message, String title) {
            out.accept(message);
            super.error(message, title);
        }
    }
}
