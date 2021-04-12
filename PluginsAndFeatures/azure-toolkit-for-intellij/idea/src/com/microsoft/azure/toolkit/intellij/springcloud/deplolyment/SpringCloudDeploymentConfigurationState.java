/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.deplolyment;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppPlatformManager;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppResourceInner;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.DeploymentResourceInner;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureRunProfileState;
import com.microsoft.azure.toolkit.intellij.springcloud.SpringCloudUtils;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppEntity;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeploymentEntity;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.azure.toolkit.lib.springcloud.model.ScaleSettings;
import com.microsoft.azure.tools.utils.RxUtils;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.springcloud.SpringCloudIdHelper;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.RunProcessHandler;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud.SpringCloudStateManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloudConfigUtils.DEFAULT_DEPLOYMENT_NAME;

class SpringCloudDeploymentConfigurationState extends AzureRunProfileState<AppResourceInner> {
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

    @Nullable
    @Override
    public AppResourceInner executeSteps(@NotNull RunProcessHandler processHandler, @NotNull Map<String, String> telemetryMap) throws Exception {
        // TODO: https://dev.azure.com/mseng/VSJava/_workitems/edit/1812811
        // prepare the jar to be deployed
        updateTelemetryMap(telemetryMap);
        final SpringCloudAppConfig appConfig = this.config.getAppConfig();
        final File artifactFile = appConfig.getDeployment().getArtifact().getFile();
        final boolean enableDisk = appConfig.getDeployment() != null && appConfig.getDeployment().isEnablePersistentStorage();
        final String clusterName = appConfig.getClusterName();
        final String appName = appConfig.getAppName();

        final SpringCloudDeploymentConfig deploymentConfig = appConfig.getDeployment();
        final Map<String, String> env = deploymentConfig.getEnvironment();
        final String jvmOptions = deploymentConfig.getJvmOptions();
        final ScaleSettings scaleSettings = deploymentConfig.getScaleSettings();
        final String runtimeVersion = deploymentConfig.getJavaVersion();

        final AzureSpringCloud az = AzureSpringCloud.az(SpringCloudUtils.getSpringManager(appConfig.getSubscriptionId()));
        final SpringCloudCluster cluster = az.cluster(clusterName);
        final SpringCloudApp app = cluster.app(appName);
        final String deploymentName = StringUtils.firstNonBlank(
            deploymentConfig.getDeploymentName(),
            appConfig.getActiveDeploymentName(),
            app.getActiveDeploymentName(),
            DEFAULT_DEPLOYMENT_NAME);
        final SpringCloudDeployment deployment = app.deployment(deploymentName);

        final boolean toCreateApp = !app.exists();
        final boolean toCreateDeployment = !deployment.exists();
        final List<AzureTask<?>> tasks = new ArrayList<>();
        if (toCreateApp) {
            setText(processHandler, String.format("Creating app(%s)...", app.name()));
            app.create().commit();
            SpringCloudStateManager.INSTANCE.notifySpringAppUpdate(cluster.id(), getInner(app.entity()), null);
            setText(processHandler, "Successfully created the app.");
        }
        setText(processHandler, String.format("Uploading artifact(%s) to Azure...", artifactFile.getPath()));
        final SpringCloudApp.Uploader artifactUploader = app.uploadArtifact(artifactFile.getPath());
        artifactUploader.commit();
        setText(processHandler, "Successfully uploaded the artifact.");

        final SpringCloudDeployment.Updater deploymentModifier = (toCreateDeployment ? deployment.create() : deployment.update())
            .configEnvironmentVariables(env)
            .configJvmOptions(jvmOptions)
            .configScaleSettings(scaleSettings)
            .configRuntimeVersion(runtimeVersion)
            .configArtifact(artifactUploader.getArtifact());
        setText(processHandler, String.format(toCreateDeployment ? "Creating deployment(%s)..." : "Updating deployment(%s)...", deploymentName));
        deploymentModifier.commit();
        setText(processHandler, toCreateDeployment ? "Successfully created the deployment" : "Successfully updated the deployment");
        SpringCloudStateManager.INSTANCE.notifySpringAppUpdate(cluster.id(), getInner(app.entity()), getInner(deployment.entity()));

        final SpringCloudApp.Updater appUpdater = app.update()
            .activate(StringUtils.firstNonBlank(StringUtils.firstNonBlank(app.getActiveDeploymentName(), toCreateDeployment ? deploymentName : null)))
            .setPublic(appConfig.isPublic())
            .enablePersistentDisk(enableDisk);
        if (!appUpdater.isSkippable()) {
            setText(processHandler, String.format("Updating app(%s)...", app.name()));
            appUpdater.commit();
            setText(processHandler, "Successfully updated the app.");
            DefaultLoader.getUIHelper().showWarningNotification(NOTIFICATION_TITLE, UPDATE_APP_WARNING);
        }

        SpringCloudStateManager.INSTANCE.notifySpringAppUpdate(cluster.id(), getInner(app.entity()), getInner(deployment.entity()));
        if (!deployment.waitUntilReady(GET_STATUS_TIMEOUT)) {
            DefaultLoader.getUIHelper().showWarningNotification(NOTIFICATION_TITLE, GET_DEPLOYMENT_STATUS_TIMEOUT);
        }
        printPublicUrl(app, processHandler);
        return getInner(app.entity()); // TODO: https://dev.azure.com/mseng/VSJava/_workitems/edit/1812811
    }

    @Override
    protected Operation createOperation() {
        return TelemetryManager.createOperation(TelemetryConstants.SPRING_CLOUD, TelemetryConstants.CREATE_SPRING_CLOUD_APP);
    }

    @Override
    protected void onSuccess(AppResourceInner result, @NotNull RunProcessHandler processHandler) {
        setText(processHandler, "Deploy succeed");
        processHandler.notifyComplete();
    }

    @Override
    protected String getDeployTarget() {
        return "SPRING_CLOUD";
    }

    @Override
    protected void updateTelemetryMap(@NotNull Map<String, String> telemetryMap) {
        final Map<String, String> props = new HashMap<>();
        props.put("runtime", config.getAppConfig().getRuntimeVersion());
        props.put("subscriptionId", config.getAppConfig().getSubscriptionId());
        telemetryMap.putAll(props);
    }

    private void printPublicUrl(final SpringCloudApp app, @NotNull RunProcessHandler processHandler) {
        if (!app.entity().isPublic()) {
            return;
        }
        setText(processHandler, String.format("Getting public url of app(%s)...", app.name()));
        String publicUrl = app.entity().getApplicationUrl();
        if (StringUtils.isEmpty(publicUrl)) {
            publicUrl = RxUtils.pollUntil(() -> app.refresh().entity().getApplicationUrl(), StringUtils::isNotBlank, GET_URL_TIMEOUT);
        }
        if (StringUtils.isEmpty(publicUrl)) {
            DefaultLoader.getUIHelper().showWarningNotification(NOTIFICATION_TITLE, "Failed to get application url");
        } else {
            setText(processHandler, String.format("Application url: %s", publicUrl));
        }
    }

    @SneakyThrows
    private static AppResourceInner getInner(final SpringCloudAppEntity app) {
        // TODO: https://dev.azure.com/mseng/VSJava/_workitems/edit/1812809
        final Field inner = SpringCloudAppEntity.class.getDeclaredField("inner");
        inner.setAccessible(true);
        return (AppResourceInner) inner.get(app);
    }

    @SneakyThrows
    private static DeploymentResourceInner getInner(final SpringCloudDeploymentEntity deployment) {
        // TODO: https://dev.azure.com/mseng/VSJava/_workitems/edit/1812809
        final Field inner = SpringCloudDeploymentEntity.class.getDeclaredField("inner");
        inner.setAccessible(true);
        return (DeploymentResourceInner) inner.get(deployment);
    }
}
