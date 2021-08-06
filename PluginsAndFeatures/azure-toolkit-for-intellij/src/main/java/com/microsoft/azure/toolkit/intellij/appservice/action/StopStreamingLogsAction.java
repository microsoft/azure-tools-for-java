/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice.action;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceStreamingLogManager;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.Groupable;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotNode;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.*;

@Name("Stop Streaming Logs")
public class StopStreamingLogsAction extends NodeActionListener {

    private Project project;
    private String resourceId;
    private String service;
    private String operation;

    public StopStreamingLogsAction(WebAppNode webAppNode) {
        super();
        this.project = (Project) webAppNode.getProject();
        this.resourceId = webAppNode.getId();
        this.service = WEBAPP;
        this.operation = STOP_STREAMING_LOG_WEBAPP;
    }

    public StopStreamingLogsAction(DeploymentSlotNode deploymentSlotNode) {
        super();
        this.project = (Project) deploymentSlotNode.getProject();
        this.resourceId = deploymentSlotNode.getId();
        this.service = WEBAPP;
        this.operation = STOP_STREAMING_LOG_WEBAPP_SLOT;
    }

    public StopStreamingLogsAction(FunctionAppNode functionNode) {
        super();
        this.project = (Project) functionNode.getProject();
        this.resourceId = functionNode.getId();
        this.service = FUNCTION;
        this.operation = STOP_STREAMING_LOG_FUNCTION_APP;
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
    protected void actionPerformed(NodeActionEvent nodeActionEvent) {
        AppServiceStreamingLogManager.INSTANCE.closeStreamingLog(project, resourceId);
    }

    @Override
    public int getGroup() {
        return Groupable.DIAGNOSTIC_GROUP;
    }
}
