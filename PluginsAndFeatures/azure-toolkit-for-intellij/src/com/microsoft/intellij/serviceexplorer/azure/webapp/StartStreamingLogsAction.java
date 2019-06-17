/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.serviceexplorer.azure.webapp;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.helpers.webapp.WebAppStreamingLogConsoleViewProvider;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotNode;
import org.jetbrains.annotations.NotNull;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;

@Name("Start Streaming Logs")
public class StartStreamingLogsAction extends NodeActionListener {

    private Node node;
    private Project project;
    private String subscriptionId;
    private String webAppId;
    private String webAppName;
    private String deploymentSlotName;


    public StartStreamingLogsAction(WebAppNode webAppNode) {
        super(webAppNode);
        this.node = webAppNode;
        this.project = (Project) webAppNode.getProject();
        this.subscriptionId = webAppNode.getSubscriptionId();
        this.webAppId = webAppNode.getWebAppId();
        this.webAppName = webAppNode.getWebAppName();
    }

    public StartStreamingLogsAction(DeploymentSlotNode deploymentSlotNode) {
        super(deploymentSlotNode);
        this.node = deploymentSlotNode;
        this.project = (Project) deploymentSlotNode.getProject();
        this.subscriptionId = deploymentSlotNode.getSubscriptionId();
        this.webAppId = deploymentSlotNode.getWebAppId();
        this.webAppName = deploymentSlotNode.getWebAppName();
        this.deploymentSlotName = deploymentSlotNode.getName();
    }

    @Override
    protected void actionPerformed(NodeActionEvent nodeActionEvent) throws AzureCmdException {
        EventUtil.executeWithLog(WEBAPP, "StartStreamingLog", operation -> {
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Start Streaming Logs", true) {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    WebAppStreamingLogConsoleViewProvider.INSTANCE
                            .startStreamingLogs(project, subscriptionId, webAppId, webAppName, deploymentSlotName);
                }
            });
        });
    }
}
