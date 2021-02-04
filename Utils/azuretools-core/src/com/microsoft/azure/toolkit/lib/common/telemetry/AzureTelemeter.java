/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.telemetry;

import com.microsoft.applicationinsights.core.dependencies.apachecommons.io.FileUtils;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationRef;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationUtils;
import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskContext;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import org.joda.time.Instant;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class AzureTelemeter {
    private static final File file = new File(System.getProperty("user.home") + "/performance.csv");

    private static final String OP_ID = "id";
    private static final String OP_NAME = "name";
    private static final String OP_TIMESTAMP = "timestamp";
    private static final String OP_TYPE = "type";
    private static final String OP_SERVICE_NAME = "serviceName";
    private static final String OP_OPERATION_NAME = "operationName";
    private static final String OP_ACTION = "action";
    private static final String OP_PARENT_ID = "parentId";
    private static final String OP_CONTEXT_ID = "contextId";
    private static final String OP_ACTION_CREATE = "CREATE";
    private static final String OP_ACTION_ENTER = "ENTER";
    private static final String OP_ACTION_EXIT = "EXIT";

    public static void afterCreate(final IAzureOperation op) {
        final Map<String, String> properties = buildProperties(op);
        properties.put(OP_TIMESTAMP, Instant.now().toString());
        properties.put(OP_ACTION, OP_ACTION_CREATE);
        AzureTelemeter.log(EventType.info, properties);
    }

    public static void beforeEnter(final IAzureOperation op) {
        final Map<String, String> properties = buildProperties(op);
        properties.put(OP_TIMESTAMP, Instant.now().toString());
        properties.put(OP_ACTION, OP_ACTION_ENTER);
        AzureTelemeter.log(EventType.opStart, properties);
    }

    public static void afterExit(final IAzureOperation op) {
        final Map<String, String> properties = buildProperties(op);
        properties.put(OP_TIMESTAMP, Instant.now().toString());
        properties.put(OP_ACTION, OP_ACTION_EXIT);
        AzureTelemeter.log(EventType.opEnd, properties);
    }

    public static void onError(final IAzureOperation op, Throwable error) {
        final Map<String, String> properties = buildProperties(op);
        properties.put(OP_TIMESTAMP, Instant.now().toString());
        properties.put(OP_ACTION, OP_ACTION_EXIT);
        AzureTelemeter.error(error, properties);
    }

    @NotNull
    private static Map<String, String> buildProperties(final IAzureOperation op) {
        final Deque<IAzureOperation> ctxOperations = AzureTaskContext.getContextOperations();
        final Optional<IAzureOperation> parent = Optional.ofNullable(ctxOperations.peekFirst());
        final Map<String, String> properties = new HashMap<>();
        final String name = op.getName().replaceAll("\\(.+\\)", "(***)"); // e.g. `appservice|file.list.dir`
        final String[] parts = name.split("\\."); // ["appservice|file", "list", "dir"]
        final String[] compositeServiceName = parts[0].split("\\|"); // ["appservice", "file"]
        final String mainServiceName = compositeServiceName[0]; // "appservice"
        final String operationName = compositeServiceName.length > 1 ? parts[1] + "_" + compositeServiceName[1] : parts[1]; // "list_file"
        properties.put(OP_CONTEXT_ID, getCompositeId(ctxOperations, op));
        properties.put(OP_ID, op.getId());
        properties.put(OP_PARENT_ID, parent.map(IAzureOperation::getId).orElse("/"));
        properties.put(OP_NAME, name);
        properties.put(OP_SERVICE_NAME, mainServiceName);
        properties.put(OP_OPERATION_NAME, operationName);
        properties.put(OP_TYPE, op.getType());
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

    private static void error(final Throwable ex, final Map<String, String> properties) {
        final String serviceName = properties.get(OP_SERVICE_NAME);
        final String operationName = properties.get(OP_OPERATION_NAME);
        final ErrorType type = ErrorType.userError;
        // TODO: (@wangmi & @Hanxiao.Liu)decide error type based on the type of ex.
        EventUtil.logError(serviceName, operationName, type, ex, properties, null);
    }

    private static void log(final EventType type, final Map<String, String> properties) {
        final String serviceName = properties.get(OP_SERVICE_NAME);
        final String operationName = properties.get(OP_OPERATION_NAME);
        EventUtil.logEvent(type, serviceName, operationName, properties, null);
    }

    private static void writeToCsvFile(Map<String, String> properties) throws IOException {
        final String val = String.format("%s, %s, %s, %s, %s, %s, %s, %s, %s",
            properties.get(OP_CONTEXT_ID),
            properties.get(OP_TIMESTAMP),
            properties.get(OP_ID),
            properties.get(OP_NAME),
            properties.get(OP_SERVICE_NAME),
            properties.get(OP_OPERATION_NAME),
            properties.get(OP_TYPE),
            properties.get(OP_ACTION),
            properties.get(OP_PARENT_ID)
        );
        FileUtils.writeStringToFile(file, val + System.lineSeparator(), StandardCharsets.UTF_8, true);
    }
}
