/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.telemetrywrapper;

import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.Environment;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import org.apache.commons.lang3.ObjectUtils;
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
        logError(serviceName, operName, errorType, e, properties, metrics, true);
    }

    // We define this new API to remove error message and stacktrace as per privacy review requirements
    public static void logErrorClassNameOnly(String serviceName, String operName, ErrorType errorType, Throwable e,
                                             Map<String, String> properties, Map<String, Double> metrics) {
        logError(serviceName, operName, errorType, e, properties, metrics, false);
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

    public static void executeWithLog(String actionString, Map<String, String> properties,
                                      Map<String, Double> metrics, TelemetryConsumer<Operation> consumer, Consumer<Exception> errorHandle) {
        Operation operation = TelemetryManager.createOperation(actionString);
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

    public static void executeWithLog(String actionString, TelemetryConsumer<Operation> consumer) {
        executeWithLog(actionString, null, null, consumer, null);
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

    // Will collect error stack traces only if user signed in with Azure account
    public static boolean isAbleToCollectErrorStacks() {
        final AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        return azureManager != null && azureManager.getEnvironment() != null &&
            ObjectUtils.equals(azureManager.getEnvironment().getAzureEnvironment(), Environment.GLOBAL.getAzureEnvironment());
    }

    private static void logError(String serviceName, String operName, ErrorType errorType, Throwable e,
                                Map<String, String> properties, Map<String, Double> metrics, boolean logErrorTraces) {
        try {
            Map<String, String> mutableProps = properties == null ? new HashMap<>() : new HashMap<>(properties);
            mutableProps.put(CommonUtil.OPERATION_NAME, operName);
            mutableProps.put(CommonUtil.OPERATION_ID, UUID.randomUUID().toString());
            mutableProps.put(CommonUtil.ERROR_CODE, "1");
            mutableProps.put(CommonUtil.ERROR_CLASSNAME, e != null ? e.getClass().getName() : "");
            mutableProps.put(CommonUtil.ERROR_TYPE, errorType.name());
            if (logErrorTraces && isAbleToCollectErrorStacks()) {
                mutableProps.put(CommonUtil.ERROR_MSG, e != null ? e.getMessage() : "");
                mutableProps.put(CommonUtil.ERROR_STACKTRACE, ExceptionUtils.getStackTrace(e));
            }
            sendTelemetry(EventType.error, serviceName, mergeProperties(mutableProps), metrics);
        } catch (Exception ignore) {
        }
    }
}
