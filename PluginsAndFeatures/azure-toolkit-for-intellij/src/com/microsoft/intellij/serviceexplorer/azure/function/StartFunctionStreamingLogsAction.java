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
package com.microsoft.intellij.serviceexplorer.azure.function;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.helpers.function.FunctionStreamingLogManager;
import com.microsoft.intellij.ui.util.UIUtils;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionEvent;
import com.microsoft.tooling.msservices.serviceexplorer.NodeActionListener;
import com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionNode;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.WEBAPP;

@Name("Start Streaming Logs")
public class StartFunctionStreamingLogsAction extends NodeActionListener {

    private final Node node;
    private String name;
    private String functionId;
    private String subscriptionId;
    private Project project;

    public StartFunctionStreamingLogsAction(FunctionNode functionNode) {
        super();
        this.node = functionNode;
        this.subscriptionId = functionNode.getSubscriptionId();
        this.project = (Project) functionNode.getProject();
        this.functionId = functionNode.getFunctionAppId();
        this.name = functionNode.getFunctionAppName();
    }

    @Override
    protected void actionPerformed(NodeActionEvent nodeActionEvent) throws AzureCmdException {
        EventUtil.executeWithLog(WEBAPP, "StartStreamingLog",
            (operation) -> {
                FunctionStreamingLogManager.INSTANCE.showStreamingLog(project, subscriptionId, name, functionId);
            },
            (exception) -> {
                UIUtils.showNotification((Project) node.getProject(), exception.getMessage(), MessageType.ERROR);
            });
    }
}
