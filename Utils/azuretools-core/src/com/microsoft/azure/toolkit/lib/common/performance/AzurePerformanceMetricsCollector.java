/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.performance;

import com.microsoft.applicationinsights.core.dependencies.apachecommons.io.FileUtils;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationRef;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationUtils;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskContext;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AzurePerformanceMetricsCollector {
    private static final File file = new File(System.getProperty("user.home") + "/performance.csv");

    private static final String PERFORMANCE = "PERFORMANCE";
    private static final String TELEMETRY_OP_TIMESTAMP = "timestamp";
    private static final String TELEMETRY_OP_ACTION = "action";
    private static final String TELEMETRY_OP_CONTEXT_ID = "context_id";
    private static final String TELEMETRY_OP_ID = "id";
    private static final String TELEMETRY_OP_PARENT_ID = "parent_id";
    private static final String TELEMETRY_OP_NAME = "name";
    private static final String TELEMETRY_OP_TYPE = "type";
    private static final String OP_ACTION_CREATE = "CREATE";
    private static final String OP_ACTION_ENTER = "ENTER";
    private static final String OP_ACTION_EXIT = "EXIT";

    public static void afterCreate(final IAzureOperation op) {
        final long timestamp = System.currentTimeMillis();
        final Map<String, String> properties = buildProperties(op);
        properties.put(TELEMETRY_OP_TIMESTAMP, String.valueOf(timestamp));
        properties.put(TELEMETRY_OP_ACTION, OP_ACTION_CREATE);
        sendTelemetry(properties);
    }

    public static void beforeEnter(final IAzureOperation op) {
        final long timestamp = System.currentTimeMillis();
        final Map<String, String> properties = buildProperties(op);
        properties.put(TELEMETRY_OP_TIMESTAMP, String.valueOf(timestamp));
        properties.put(TELEMETRY_OP_ACTION, OP_ACTION_ENTER);
        sendTelemetry(properties);
    }

    public static void afterExit(final IAzureOperation op) {
        final long timestamp = System.currentTimeMillis();
        final Map<String, String> properties = buildProperties(op);
        properties.put(TELEMETRY_OP_TIMESTAMP, String.valueOf(timestamp));
        properties.put(TELEMETRY_OP_ACTION, OP_ACTION_EXIT);
        sendTelemetry(properties);
    }

    @NotNull
    private static Map<String, String> buildProperties(final IAzureOperation op) {
        final Deque<IAzureOperation> ctxOperations = AzureTaskContext.getContextOperations();
        final Optional<IAzureOperation> parent = Optional.ofNullable(ctxOperations.peekFirst());
        final Map<String, String> properties = new HashMap<>();
        properties.put(TELEMETRY_OP_CONTEXT_ID, getCompositeId(ctxOperations, op));
        properties.put(TELEMETRY_OP_ID, op.getId());
        properties.put(TELEMETRY_OP_PARENT_ID, parent.map(IAzureOperation::getId).orElse("/"));
        properties.put(TELEMETRY_OP_NAME, op.getName().replaceAll("\\(.+\\)","(***)"));
        properties.put(TELEMETRY_OP_TYPE, op.getType());
        return properties;
    }

    private static String getCompositeId(final Deque<? extends IAzureOperation> ops, IAzureOperation op) {
        final List<IAzureOperation> revised = revise(ops);
        revised.add(op);
        return revised.stream().map(IAzureOperation::getId).collect(Collectors.joining("/", "/", ""));
    }

    /**
     * get all ancestors until the last operation of ACTION type.
     */
    public static List<IAzureOperation> revise(Deque<? extends IAzureOperation> ops) {
        final LinkedList<IAzureOperation> result = new LinkedList<>();
        for (final IAzureOperation op : ops) {
            result.addFirst(op);
            if (op instanceof AzureOperationRef) {
                final AzureOperation annotation = AzureOperationUtils.getAnnotation((AzureOperationRef) op);
                if (annotation.type() == AzureOperation.Type.ACTION) {
                    break;
                }
            }
        }
        return result;
    }

    @SneakyThrows
    private static void sendTelemetry(final Map<String, String> properties) {
        AppInsightsClient.create(PERFORMANCE, null, properties);
    }

    private static void writeToCsvFile(Map<String, String> properties) throws IOException {
        final String val = String.format("%s, %s, %s, %s, %s, %s, %s",
            properties.get(TELEMETRY_OP_CONTEXT_ID),
            properties.get(TELEMETRY_OP_TIMESTAMP),
            properties.get(TELEMETRY_OP_ID),
            properties.get(TELEMETRY_OP_NAME),
            properties.get(TELEMETRY_OP_TYPE),
            properties.get(TELEMETRY_OP_ACTION),
            properties.get(TELEMETRY_OP_PARENT_ID)
        );
        FileUtils.writeStringToFile(file, val + System.lineSeparator(), StandardCharsets.UTF_8, true);
    }
}
