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

package com.microsoft.tooling.msservices.serviceexplorer;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.azure.toolkit.lib.common.handler.AzureExceptionHandler;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.azuretools.telemetrywrapper.*;

import javax.swing.*;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;

public abstract class NodeActionListener implements EventListener, Sortable, Groupable {
    protected int priority = Sortable.DEFAULT_PRIORITY;
    protected int group = Groupable.DEFAULT_GROUP;

    public NodeActionListener() {
        // need a nullary constructor defined in order for
        // Class.newInstance to work on sub-classes
    }

    @Override
    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    protected void beforeActionPerformed(NodeActionEvent e) {
        // mark node as loading
        sendTelemetry(e);
    }

    protected void sendTelemetry(NodeActionEvent nodeActionEvent) {
        Node node = nodeActionEvent.getAction().getNode();
        AppInsightsClient.createByType(AppInsightsClient.EventType.Action, node.getClass().getSimpleName(),
            nodeActionEvent.getAction().getName(), buildProp(node));
    }

    private Map<String, String> buildProp(Node node) {
        final Map<String, String> properties = new HashMap<>();
        properties.put("Node", node.getId());
        properties.put("Name", node.getName());
        if (node instanceof TelemetryProperties) {
            properties.putAll(((TelemetryProperties) node).toProperties());
        }
        if (node.getParent() != null) {
            properties.put("Parent", node.getParent().getName());
            properties.put("ParentType", node.getParent().getClass().getSimpleName());
        }
        return properties;
    }

    protected abstract void actionPerformed(NodeActionEvent e)
            throws AzureCmdException;

    public Icon getIcon() {
        return null;
    }

    public ListenableFuture<Void> actionPerformedAsync(NodeActionEvent e) {
        String serviceName = transformHDInsight(getServiceName(e), e.getAction().getNode());
        String operationName = getOperationName(e);
        Operation operation = TelemetryManager.createOperation(serviceName, operationName);
        try {
            operation.start();
            Node node = e.getAction().getNode();
            EventUtil.logEvent(EventType.info, operation, buildProp(node));
            actionPerformed(e);
            return Futures.immediateFuture(null);
        } catch (AzureCmdException | RuntimeException ex) {
            EventUtil.logError(operation, ErrorType.systemError, ex, null, null);
            AzureExceptionHandler.getInstance().handleException(ex, false);
            return Futures.immediateFailedFuture(ex);
        } finally {
            operation.complete();
        }
    }

    /**
     * If nodeName contains spark and hdinsight, we just think it is a spark node.
     * So set the service name to hdinsight
     * @param serviceName
     * @return
     */
    private String transformHDInsight(String serviceName, Node node) {
        try {
            if (serviceName.equals(TelemetryConstants.ACTION)) {
                String nodeName = node.getName().toLowerCase();
                if (nodeName.contains("spark") || nodeName.contains("hdinsight")) {
                    return TelemetryConstants.HDINSIGHT;
                }
                if (node.getParent() != null) {
                    String parentName = node.getParent().getName().toLowerCase();
                    if (parentName.contains("spark") || parentName.contains("hdinsight")) {
                        return TelemetryConstants.HDINSIGHT;
                    }
                }
            }
            return serviceName;
        } catch (Exception ignore) {
        }
        return serviceName;
    }

    protected String getServiceName(NodeActionEvent event) {
        try {
            return event.getAction().getNode().getServiceName();
        } catch (Exception ignore) {
            return TelemetryConstants.ACTION;
        }
    }

    protected String getOperationName(NodeActionEvent event) {
        try {
            return event.getAction().getName().toLowerCase().trim().replace(" ", "-");
        } catch (Exception ignore) {
            return "";
        }
    }

    protected void afterActionPerformed(NodeActionEvent e) {
        // mark node as done loading
    }

}
