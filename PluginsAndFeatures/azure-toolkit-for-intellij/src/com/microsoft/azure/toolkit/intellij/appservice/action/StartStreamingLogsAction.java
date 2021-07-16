/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice.action;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceUtils;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceStreamingLogManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureText;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.Groupable;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotNode;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.*;

@Name("Start Streaming Logs")
public class StartStreamingLogsAction extends NodeActionListener {

    private Project project;
    private String resourceId;
    private String service;
    private String operation;

    public StartStreamingLogsAction(WebAppNode webAppNode) {
        super();
        this.project = (Project) webAppNode.getProject();
        this.resourceId = webAppNode.getId();
        this.service = WEBAPP;
        this.operation = START_STREAMING_LOG_WEBAPP;
    }

    public StartStreamingLogsAction(DeploymentSlotNode deploymentSlotNode) {
        super();
        this.project = (Project) deploymentSlotNode.getProject();
        this.resourceId = deploymentSlotNode.getId();
        this.service = WEBAPP;
        this.operation = START_STREAMING_LOG_WEBAPP_SLOT;
    }

    public StartStreamingLogsAction(FunctionAppNode functionNode) {
        super();
        this.project = (Project) functionNode.getProject();
        this.resourceId = functionNode.getId();
        this.service = FUNCTION;
        this.operation = START_STREAMING_LOG_FUNCTION_APP;
    }

    @Override
    protected String getServiceName(final NodeActionEvent event) {
        return this.service;
    }

    @Override
    protected String getOperationName(final NodeActionEvent event) {
        return this.operation;
    }

    @Override
    protected void actionPerformed(NodeActionEvent nodeActionEvent) throws AzureCmdException {
        final AzureText title = AzureOperationBundle.title("appservice|log_stream.start", ResourceUtils.nameFromResourceId(resourceId));
        AzureTaskManager.getInstance().runInBackground(new AzureTask(project, title, false, () -> {
            switch (operation) {
                case START_STREAMING_LOG_FUNCTION_APP:
                    AppServiceStreamingLogManager.INSTANCE.showFunctionStreamingLog(project, resourceId);
                    break;
                case START_STREAMING_LOG_WEBAPP:
                    AppServiceStreamingLogManager.INSTANCE.showWebAppStreamingLog(project, resourceId);
                    break;
                case START_STREAMING_LOG_WEBAPP_SLOT:
                    AppServiceStreamingLogManager.INSTANCE.showWebAppDeploymentSlotStreamingLog(project,
                                                                                                resourceId);
                    break;
                default:
                    DefaultLoader.getUIHelper().showError("Unsupported operation", "Unsupported operation");
                    break;
            }
        }));
    }

    @Override
    public int getGroup() {
        return Groupable.DIAGNOSTIC_GROUP;
    }
}
