package com.microsoft.azuretools.telemetrywrapper;

import com.microsoft.applicationinsights.TelemetryClient;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TelemetryManager {

    private TelemetryClient telemetryClient;
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

    public TelemetryClient getTelemetryClient() {
        return telemetryClient;
    }

    public void setTelemetryClient(TelemetryClient telemetryClient) {
        this.telemetryClient = telemetryClient;
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
        return producer;
    }
}
