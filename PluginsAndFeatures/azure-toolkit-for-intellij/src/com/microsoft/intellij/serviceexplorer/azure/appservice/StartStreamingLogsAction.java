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

package com.microsoft.intellij.serviceexplorer.azure.appservice;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskRunner;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.helpers.AppServiceStreamingLogManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionNode;
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

    public StartStreamingLogsAction(FunctionNode functionNode) {
        super();
        this.project = (Project) functionNode.getProject();
        this.resourceId = functionNode.getId();
        this.service = FUNCTION;
        this.operation = START_STREAMING_LOG_FUNCTION_APP;
    }

    @Override
    protected void actionPerformed(NodeActionEvent nodeActionEvent) throws AzureCmdException {
        EventUtil.executeWithLog(service, operation, op -> {
            AzureTaskRunner.getInstance().runInBackground(new AzureTask(project, "Start Streaming Logs", true, () -> {
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
        });
    }
}
