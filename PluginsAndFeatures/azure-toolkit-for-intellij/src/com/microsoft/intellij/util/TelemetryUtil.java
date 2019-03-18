/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.util;

import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetry.AppInsightsClient.ErrorType;
import com.microsoft.azuretools.telemetry.AppInsightsClient.EventType;
import java.util.HashMap;
import java.util.Map;

public class TelemetryUtil {

    public static void sendTelemetryOpStart(EventType eventType, String operationName, Map<String, String> properties) {
        AppInsightsClient.sendOpStart(eventType, operationName, properties);
    }

    public static void sendTelemetryOpEnd(EventType eventType, String operationName, Map<String, String> properties) {
        AppInsightsClient.sendOpEnd(eventType, operationName, properties);
    }

    public static void sendTelemetryOpEnd(EventType eventType, String operationName, Map<String, String> properties,
        long time) {
        AppInsightsClient.sendOpEnd(eventType, operationName, properties, buildMetrics(time));
    }

    public static void sendTelemetryOpError(EventType eventType, String operationName, ErrorType errorType, String errMsg,
        Map<String, String> properties) {
        AppInsightsClient.sendError(eventType, operationName, errorType, errMsg, properties);
    }

    public static void sendTelemetryOpError(EventType eventType, String operationName, ErrorType errorType, String errMsg,
        Map<String, String> properties, long time) {
        AppInsightsClient.sendError(eventType, operationName, errorType, errMsg, properties, buildMetrics(time));
    }

    private static Map<String, Double> buildMetrics(long time) {
        Map<String, Double> metrics = new HashMap<>();
        metrics.put("duration", (double) time);
        return metrics;
    }

}