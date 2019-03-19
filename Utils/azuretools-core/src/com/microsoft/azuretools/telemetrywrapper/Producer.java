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

import com.microsoft.applicationinsights.TelemetryClient;
import java.util.Map;

/**
 * Each time when you start a transaction, we will generate a operationId(the operationId will store in thread local),
 * then you can send info,warn,error with this operationId, you need to end this transaction.
 * If you start a new transaction without close the previous one, we just generate a new operationId.
 * If you send info, error, warn or end transaction without start a transaction, we will ignore this operation.
 * Take care, each time when you start a transaction you need to end it. Or you cannot correctly trace the operation.
 *
 * If you just want to record an event or error, you do not want to trace the transaction. Please directly use logevent
 * or logerror.
 *
 * For transaction, the snappy code just like this:
 *   try {
 *       startTransaction();
 *       doSomething();
 *       sendInfo();
 *   } catch (Exception e) {
 *       sendError();
 *   } finally {
 *       endTransaction();
 *   }
 *
 *   For independent event, the snappy code just like this:
 *   dosomething1();
 *   logEvent();
 *   dosomething2();
 *   logEvent();
 *
 *   Sequence Diagram:
 *
 *                         operationId
 *   start a transaction        |
 *   send info                  |
 *   send info                  |
 *   send error                 |
 *   end a transaction          v
 */
public interface Producer {

    void startTransaction(String eventName, String operName, Map<String, String> properties,
        Map<String, Double> metrics);

    void endTransaction(Map<String, String> properties, Map<String, Double> metrics);

    void sendError(ErrorType errorType, String errMsg, Map<String, String> properties, Map<String, Double> metrics);

    void sendInfo(Map<String, String> properties, Map<String, Double> metrics);

    void sendWarn(Map<String, String> properties, Map<String, Double> metrics);

    void logEvent(EventType eventType, String eventName, String operName, Map<String, String> properties,
        Map<String, Double> metrics);

    void logError(String eventName, String operName, ErrorType errorType, String errMsg,
        Map<String, String> properties, Map<String, Double> metrics);

    void setTelemetryClient(TelemetryClient client);
}
