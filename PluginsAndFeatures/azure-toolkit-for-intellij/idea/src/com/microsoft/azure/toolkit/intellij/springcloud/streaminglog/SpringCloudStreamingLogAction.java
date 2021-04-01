/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.streaminglog;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appplatform.v2020_07_01.DeploymentInstance;
import com.microsoft.azure.management.appplatform.v2020_07_01.implementation.AppPlatformManager;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceId;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceUtils;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperationTitle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.AzureSpringCloud;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.Groupable;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud.SpringCloudAppNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Optional;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.SPRING_CLOUD;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.START_STREAMING_LOG_SPRING_CLOUD_APP;

@Name("Streaming Logs")
public class SpringCloudStreamingLogAction extends NodeActionListener {

    private static final String FAILED_TO_START_LOG_STREAMING = "Failed to start log streaming";

    private static final String NO_ACTIVE_DEPLOYMENT = "No active deployment in current app.";
    private static final String NO_AVAILABLE_INSTANCES = "No available instances in current app.";
    private static final String FAILED_TO_LIST_INSTANCES = "Failed to list spring cloud app instances.";
    private static final String FAILED_TO_LIST_INSTANCES_WITH_MESSAGE = "Failed to list spring cloud app instances: %s";

    private final SpringCloudApp app;
    private final Project project;
    private final String appId;

    public SpringCloudStreamingLogAction(SpringCloudAppNode springCloudAppNode) {
        super();
        this.project = (Project) springCloudAppNode.getProject();
        this.appId = springCloudAppNode.getId();
        final ResourceId appId = ResourceId.fromString(this.appId);
        final String subscriptionId = appId.subscriptionId();
        final String clusterName = appId.parent().name();
        this.app = AzureSpringCloud.az(getSpringManager(subscriptionId)).cluster(clusterName).app(appId.name());
    }

    @Override
    protected void actionPerformed(NodeActionEvent nodeActionEvent) throws AzureCmdException {
        EventUtil.executeWithLog(SPRING_CLOUD, START_STREAMING_LOG_SPRING_CLOUD_APP, operation -> {
            final IAzureOperationTitle title = AzureOperationBundle.title("springcloud|log_stream.open", ResourceUtils.nameFromResourceId(appId));
            AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, title, false, () -> {
                try {
                    final String deploymentName = this.app.getActiveDeploymentName();
                    final SpringCloudDeployment deployment = Optional.ofNullable(deploymentName).map(this.app::deployment).orElse(null);
                    if (deploymentName == null || !deployment.exists()) {
                        DefaultLoader.getIdeHelper().invokeLater(() ->
                            PluginUtil.displayWarningDialog(FAILED_TO_START_LOG_STREAMING, NO_ACTIVE_DEPLOYMENT));
                        return;
                    }
                    final List<DeploymentInstance> instances = deployment.entity().getInstances();
                    if (CollectionUtils.isEmpty(instances)) {
                        DefaultLoader.getIdeHelper().invokeLater(() ->
                            PluginUtil.displayWarningDialog(FAILED_TO_START_LOG_STREAMING, NO_AVAILABLE_INSTANCES));
                    } else {
                        showLogStreamingDialog(instances);
                    }
                } catch (final Exception e) {
                    final String errorMessage = StringUtils.isEmpty(e.getMessage()) ?
                        FAILED_TO_LIST_INSTANCES : String.format(FAILED_TO_LIST_INSTANCES_WITH_MESSAGE, e.getMessage());
                    DefaultLoader.getUIHelper().showError(errorMessage, FAILED_TO_START_LOG_STREAMING);
                }
            }));
        });
    }

    private void showLogStreamingDialog(List<DeploymentInstance> instances) {
        DefaultLoader.getIdeHelper().invokeLater(() -> {
            final SpringCloudStreamingLogDialog dialog = new SpringCloudStreamingLogDialog(project, instances);
            if (dialog.showAndGet()) {
                final DeploymentInstance target = dialog.getInstance();
                SpringCloudStreamingLogManager.getInstance().showStreamingLog(project, appId, target.name());
            }
        });
    }

    @Override
    public int getGroup() {
        return Groupable.DIAGNOSTIC_GROUP;
    }

    private static AppPlatformManager getSpringManager(String sid) {
        return AuthMethodManager.getInstance().getAzureSpringCloudClient(sid);
    }
}
