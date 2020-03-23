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
