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

package com.microsoft.azuretools.telemetrywrapper;

import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static com.microsoft.azuretools.telemetrywrapper.CommonUtil.mergeProperties;
import static com.microsoft.azuretools.telemetrywrapper.CommonUtil.sendTelemetry;

public class EventUtil {

    public static void logEvent(EventType eventType, String serviceName, String operName, Map<String, String> properties,
        Map<String, Double> metrics) {
        try {
            // Parameter properties might be a ImmutableMap, which means calling properties.put will lead to UnsupportedOperationException
            Map<String, String> mutableProps = properties == null ? new HashMap<>() : new HashMap<>(properties);
            mutableProps.put(CommonUtil.OPERATION_NAME, operName);
            mutableProps.put(CommonUtil.OPERATION_ID, UUID.randomUUID().toString());
            sendTelemetry(eventType, serviceName, mergeProperties(mutableProps), metrics);
        } catch (Exception ignore) {
        }
    }

    public static void logEvent(EventType eventType, String serviceName, String operName,
        Map<String, String> properties) {
        logEvent(eventType, serviceName, operName, properties, null);
    }

    public static void logError(String serviceName, String operName, ErrorType errorType, Throwable e,
        Map<String, String> properties, Map<String, Double> metrics) {
        try {
            Map<String, String> mutableProps = properties == null ? new HashMap<>() : new HashMap<>(properties);
            mutableProps.put(CommonUtil.OPERATION_NAME, operName);
            mutableProps.put(CommonUtil.OPERATION_ID, UUID.randomUUID().toString());
            mutableProps.put(CommonUtil.ERROR_CODE, "1");
            mutableProps.put(CommonUtil.ERROR_MSG, e != null ? e.getMessage() : "");
            mutableProps.put(CommonUtil.ERROR_CLASSNAME, e != null ? e.getClass().getName() : "");
            mutableProps.put(CommonUtil.ERROR_TYPE, errorType.name());
            mutableProps.put(CommonUtil.ERROR_STACKTRACE, ExceptionUtils.getStackTrace(e));
            sendTelemetry(EventType.error, serviceName, mergeProperties(mutableProps), metrics);
        } catch (Exception ignore) {
        }
    }

    // We define this new API to remove error message and stacktrace as per privacy review requirements
    public static void logErrorClassNameOnly(String serviceName, String operName, ErrorType errorType, Throwable e,
                                Map<String, String> properties, Map<String, Double> metrics) {
        try {
            Map<String, String> mutableProps = properties == null ? new HashMap<>() : new HashMap<>(properties);
            mutableProps.put(CommonUtil.OPERATION_NAME, operName);
            mutableProps.put(CommonUtil.OPERATION_ID, UUID.randomUUID().toString());
            mutableProps.put(CommonUtil.ERROR_CODE, "1");
            mutableProps.put(CommonUtil.ERROR_CLASSNAME, e != null ? e.getClass().getName() : "");
            mutableProps.put(CommonUtil.ERROR_TYPE, errorType.name());
            sendTelemetry(EventType.error, serviceName, mergeProperties(mutableProps), metrics);
        } catch (Exception ignore) {
        }
    }

    public static void logEvent(EventType eventType, Operation operation, Map<String, String> properties,
        Map<String, Double> metrics) {
        if (operation == null) {
            return;
        }

        ((DefaultOperation) operation).logEvent(eventType, properties, metrics);
    }

    public static void logEventWithComplete(EventType eventType, Operation operation, Map<String, String> properties,
                                Map<String, Double> metrics) {
        if (operation == null) {
            return;
        }

        logEvent(eventType, operation, properties, metrics);
        operation.complete();
    }

    public static void logEvent(EventType eventType, Operation operation, Map<String, String> properties) {
        if (operation == null) {
            return;
        }

        logEvent(eventType, operation, properties, null);
    }

    public static void logError(Operation operation, ErrorType errorType, Throwable e,
        Map<String, String> properties, Map<String, Double> metrics) {
        if (operation == null) {
            return;
        }

        ((DefaultOperation) operation).logError(errorType, e, properties, metrics);
    }

    // We define this new API to remove error message and stacktrace as per privacy review requirements
    public static void logErrorClassNameOnly(Operation operation, ErrorType errorType, Throwable e,
                                Map<String, String> properties, Map<String, Double> metrics) {
        if (operation == null) {
            return;
        }

        ((DefaultOperation) operation).logErrorClassNameOnly(errorType, e, properties, metrics);
    }

    public static void logErrorWithComplete(Operation operation, ErrorType errorType, Throwable e,
                                Map<String, String> properties, Map<String, Double> metrics) {
        if (operation == null) {
            return;
        }

        logError(operation, errorType, e, properties, metrics);
        operation.complete();
    }

    // We define this new API to remove error message and stacktrace as per privacy review requirements
    public static void logErrorClassNameOnlyWithComplete(Operation operation, ErrorType errorType, Throwable e,
                                            Map<String, String> properties, Map<String, Double> metrics) {
        if (operation == null) {
            return;
        }

        logErrorClassNameOnly(operation, errorType, e, properties, metrics);
        operation.complete();
    }

    public static void executeWithLog(String serviceName, String operName, Map<String, String> properties,
        Map<String, Double> metrics, TelemetryConsumer<Operation> consumer, Consumer<Exception> errorHandle) {
        Operation operation = TelemetryManager.createOperation(serviceName, operName);
        try {
            operation.start();
            consumer.accept(operation);
        } catch (Exception e) {
            logError(operation, ErrorType.userError, e, properties, metrics);
            if (errorHandle != null) {
                errorHandle.accept(e);
            } else {
                throw new RuntimeException(e);
            }
        } finally {
            operation.complete();
        }
    }

    public static <R> R executeWithLog(String serviceName, String operName, Map<String, String> properties,
        Map<String, Double> metrics, TelemetryFunction<Operation, R> function, Consumer<Exception> errorHandle) {
        Operation operation = TelemetryManager.createOperation(serviceName, operName);
        try {
            operation.start();
            return function.apply(operation);
        } catch (Exception e) {
            logError(operation, ErrorType.userError, e, properties, metrics);
            if (errorHandle != null) {
                errorHandle.accept(e);
            } else {
                throw new RuntimeException(e);
            }
        } finally {
            operation.complete();
        }
        return null;
    }

    public static void executeWithLog(String serviceName, String operName, TelemetryConsumer<Operation> consumer) {
        executeWithLog(serviceName, operName, null, null, consumer, null);
    }

    public static void executeWithLog(String serviceName, String operName, TelemetryConsumer<Operation> consumer,
        Consumer<Exception> errorHandle) {
        executeWithLog(serviceName, operName, null, null, consumer, errorHandle);
    }

    public static <R> R executeWithLog(String serviceName, String operName, TelemetryFunction<Operation, R> consumer,
        Consumer<Exception> errorHandle) {
        return executeWithLog(serviceName, operName, null, null, consumer, errorHandle);
    }

    public static <R> R executeWithLog(String serviceName, String operName, TelemetryFunction<Operation, R> function) {
        return executeWithLog(serviceName, operName, null, null, function, null);
    }
}
