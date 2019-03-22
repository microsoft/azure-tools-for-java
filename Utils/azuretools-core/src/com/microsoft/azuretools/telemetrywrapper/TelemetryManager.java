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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TelemetryManager {
    private String eventNamePrefix = "";
    private Map<String, String> commonProperties = Collections.unmodifiableMap(new HashMap<>());
    private Producer producer = new DefaultProducer();

    private static final class SingletonHolder {
        private static final TelemetryManager INSTANCE = new TelemetryManager();
    }

    private TelemetryManager() {
    }

    public static TelemetryManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void setTelemetryClient(TelemetryClient telemetryClient) {
        CommonUtil.client = telemetryClient;
    }

    public String getEventNamePrefix() {
        return eventNamePrefix;
    }

    public void setEventNamePrefix(String eventNamePrefix) {
        this.eventNamePrefix = eventNamePrefix;
    }

    public Map<String, String> getCommonProperties() {
        return commonProperties;
    }

    public synchronized void setCommonProperties(Map<String, String> commonProperties) {
        if (commonProperties != null) {
            this.commonProperties = Collections.unmodifiableMap(commonProperties);
        }
    }

    public synchronized Producer getProducer() {
        if (producer == null) {
            producer = new DefaultProducer();
        }
        return producer;
    }
}
