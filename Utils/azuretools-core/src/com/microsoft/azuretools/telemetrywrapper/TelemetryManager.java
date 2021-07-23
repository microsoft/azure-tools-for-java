/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.telemetrywrapper;

import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetryClient;
import com.microsoft.azuretools.ActionConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TelemetryManager {

    private String eventNamePrefix = "";
    private Map<String, String> commonProperties = Collections.unmodifiableMap(new HashMap<>());

    private static final class SingletonHolder {
        private static final TelemetryManager INSTANCE = new TelemetryManager();
    }

    private TelemetryManager() {
    }

    public static TelemetryManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void setTelemetryClient(AzureTelemetryClient telemetryClient) {
        CommonUtil.client = telemetryClient;
    }

    public String getEventNamePrefix() {
        return eventNamePrefix;
    }

    public void setEventNamePrefix(String eventNamePrefix) {
        this.eventNamePrefix = eventNamePrefix;
    }

    public void sendCachedTelemetries() {
        CommonUtil.clearCachedEvents();
    }

    public Map<String, String> getCommonProperties() {
        return commonProperties;
    }

    public synchronized void setCommonProperties(Map<String, String> commonProperties) {
        if (commonProperties != null) {
            this.commonProperties = Collections.unmodifiableMap(commonProperties);
        }
    }

    public static Operation createOperation(String serviceName, String operationName) {
        return new DefaultOperation(serviceName, operationName);
    }

    public static Operation createOperation(String actionString) {
        ActionConstants.ActionEntity action = ActionConstants.parse(actionString);
        return new DefaultOperation(action.getServiceName(), action.getOperationName());
    }

}
