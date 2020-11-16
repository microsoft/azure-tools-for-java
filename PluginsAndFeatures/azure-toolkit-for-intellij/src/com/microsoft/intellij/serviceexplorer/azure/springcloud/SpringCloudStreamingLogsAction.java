/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.serviceexplorer.azure.springcloud;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.DeploymentInstance;
import com.microsoft.azure.management.appplatform.v2019_05_01_preview.implementation.DeploymentResourceInner;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskRunner;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.core.mvp.model.springcloud.AzureSpringCloudMvpModel;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.forms.springcloud.SpringCloudAppStreamingLogDialog;
import com.microsoft.intellij.helpers.springcloud.SpringCloudStreamingLogManager;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.springcloud.SpringCloudAppNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.util.List;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.SPRING_CLOUD;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.START_STREAMING_LOG_SPRING_CLOUD_APP;

@Name("Streaming Logs")
public class SpringCloudStreamingLogsAction extends NodeActionListener {

    private static final String FAILED_TO_START_LOG_STREAMING = "Failed to start log streaming";

    private static final String NO_ACTIVE_DEPLOYMENT = "No active deployment in current app.";
    private static final String NO_AVAILABLE_INSTANCES = "No available instances in current app.";
    private static final String FAILED_TO_LIST_INSTANCES = "Failed to list spring cloud app instances.";
    private static final String FAILED_TO_LIST_INSTANCES_WITH_MESSAGE =
            "Failed to list spring cloud app instances: %s";

    private Project project;
    private String appId;

    public SpringCloudStreamingLogsAction(SpringCloudAppNode springCloudAppNode) {
        super();
        this.project = (Project) springCloudAppNode.getProject();
        this.appId = springCloudAppNode.getId();
    }

    @Override
    protected void actionPerformed(NodeActionEvent nodeActionEvent) throws AzureCmdException {
        EventUtil.executeWithLog(SPRING_CLOUD, START_STREAMING_LOG_SPRING_CLOUD_APP, operation -> {
            AzureTaskRunner.getInstance().runInBackground(new AzureTask(project, "Start Streaming Logs", false, () -> {
                try {
                    final DeploymentResourceInner deploymentResourceInner =
                            AzureSpringCloudMvpModel.getActiveDeploymentForApp(appId);
                    if (deploymentResourceInner == null) {
                        DefaultLoader.getIdeHelper().invokeLater(() -> PluginUtil.displayWarningDialog(
                                FAILED_TO_START_LOG_STREAMING, NO_ACTIVE_DEPLOYMENT));
                        return;
                    }
                    final List<DeploymentInstance> instances = deploymentResourceInner.properties().instances();
                    if (CollectionUtils.isEmpty(instances)) {
                        DefaultLoader.getIdeHelper().invokeLater(() -> PluginUtil.displayWarningDialog(
                                FAILED_TO_START_LOG_STREAMING, NO_AVAILABLE_INSTANCES));
                    } else {
                        showLogStreamingDialog(instances);
                    }
                } catch (Exception e) {
                    final String errorMessage = StringUtils.isEmpty(e.getMessage()) ? FAILED_TO_LIST_INSTANCES :
                                                String.format(FAILED_TO_LIST_INSTANCES_WITH_MESSAGE, e.getMessage());
                    DefaultLoader.getUIHelper().showError(errorMessage, FAILED_TO_START_LOG_STREAMING);
                }
            }));
        });
    }

    private void showLogStreamingDialog(List<DeploymentInstance> instances) {
        DefaultLoader.getIdeHelper().invokeLater(() -> {
            final SpringCloudAppStreamingLogDialog dialog = new SpringCloudAppStreamingLogDialog(project, instances);
            if (dialog.showAndGet()) {
                final DeploymentInstance target = dialog.getInstance();
                SpringCloudStreamingLogManager.getInstance().showStreamingLog(project, appId, target.name());
            }
        });
    }
}
