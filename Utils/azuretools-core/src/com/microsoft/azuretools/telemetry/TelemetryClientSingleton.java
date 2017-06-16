package com.microsoft.azuretools.telemetry;
import com.microsoft.applicationinsights.TelemetryClient;

public final class TelemetryClientSingleton {
    private TelemetryClient telemetry = null;

    private static final class SingletonHolder {
        private static final TelemetryClientSingleton INSTANCE = init();
        private static TelemetryClientSingleton init(){
            return new TelemetryClientSingleton();
        }
    }

    public static TelemetryClient getTelemetry(){
        return SingletonHolder.INSTANCE.telemetry;
    }
        
    private TelemetryClientSingleton(){
        telemetry = new TelemetryClient();
    }
}
