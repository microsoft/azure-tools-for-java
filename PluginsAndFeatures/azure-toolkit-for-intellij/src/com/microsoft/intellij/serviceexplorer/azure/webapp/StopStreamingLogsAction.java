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

package com.microsoft.intellij.serviceexplorer.azure.webapp;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.helpers.webapp.WebAppStreamingLogConsoleViewProvider;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppNode;
import com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.deploymentslot.DeploymentSlotNode;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;

@Name("Stop Streaming Logs")
public class StopStreamingLogsAction extends NodeActionListener {

    private final Node node;
    private String webAppId;
    private String deploymentSlotName;

    public StopStreamingLogsAction(WebAppNode webAppNode) {
        super();
        this.node = webAppNode;
        this.webAppId = webAppNode.getWebAppId();
    }

    public StopStreamingLogsAction(DeploymentSlotNode deploymentSlotNode) {
        super();
        this.node = deploymentSlotNode;
        this.webAppId = deploymentSlotNode.getWebAppId();
        this.deploymentSlotName = deploymentSlotNode.getName();
    }

    @Override
    protected void actionPerformed(NodeActionEvent nodeActionEvent) throws AzureCmdException {
        EventUtil.executeWithLog(WEBAPP, "StopStreamingLog",
                (operation) -> {
                    WebAppStreamingLogConsoleViewProvider.INSTANCE.stopStreamingLogs(webAppId, deploymentSlotName);
                },
                (exception)->{
                    UIUtils.showNotification((Project) node.getProject(),exception.getMessage(), MessageType.ERROR);
                });
    }
}
