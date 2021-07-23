/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;

// TODO(Qianjin): remove implementations of Sortable and Groupable
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
    }

    protected Mono<Map<String, String>> buildProp(Node node) {
        return Mono.fromCallable(() -> {
            final Map<String, String> properties = new HashMap<>();
            if (node instanceof TelemetryProperties) {
                properties.putAll(((TelemetryProperties) node).toProperties());
            }
            return properties;
        });
    }

    protected abstract void actionPerformed(NodeActionEvent e)
            throws AzureCmdException;

    public AzureIconSymbol getIconSymbol() {
        return null;
    }

    public AzureActionEnum getAction() {
        return null;
    }

    public ListenableFuture<Void> actionPerformedAsync(NodeActionEvent e) {
        String serviceName = transformHDInsight(getServiceName(e), e.getAction().getNode());
        String operationName = getOperationName(e);
        Operation operation = TelemetryManager.createOperation(serviceName, operationName);
        Node node = e.getAction().getNode();
        Mono<Map<String, String>> telemetryMono = buildProp(node);
        try {
            operation.start();
            actionPerformed(e);
            return Futures.immediateFuture(null);
        } catch (AzureCmdException | RuntimeException ex) {
            EventUtil.logError(operation, ErrorType.systemError, ex, null, null);
            AzureMessager.getMessager().error(ex);
            return Futures.immediateFailedFuture(ex);
        } finally {
            telemetryMono.subscribeOn(Schedulers.boundedElastic()).subscribe(properties -> {
                operation.trackProperties(properties);
                operation.complete();
            });
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
