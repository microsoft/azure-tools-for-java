/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.telemetrywrapper;

import java.io.Closeable;

/**
 * The operation is used for trace the request. The typical usage is:
 *
 * Operation operation = TelemetryManager.createOperation(eventName, operationName);
 * try {
 *    operation.start();
 *    dosomething();
 *    EventUtil.logEvent(eventType, operation, ...); // Here you should pass the operation as a parameter.
 * } catch Exception(e) {
 *    EventUtil.logError(operation, e, ...); // Here you should pass the operation as a parameter.
 * } finally {
 *    operation.complete();
 * }
 *
 * The whole operation will share the same operation id, by this way, we can trace the operation.
 * When you start a operation, you should complete it. Or you can not correctly trace the request.
 * If you do not need to trace the request, you can directly use EventUtil.logEvent(eventType, ...)
 *
 * We also provided Syntactic sugar, the usage is:
 *
 * EventUtil.logCommand(eventName, operationName, () -> {
 *    yourFunction();
 * });
 *
 * it will automatically start the operation, logerror and complete operation.
 *
 */
public interface Operation extends Closeable {

    void start();

    void complete();
}
