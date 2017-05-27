package com.microsoft.azuretools.telemetry;
import com.microsoft.applicationinsights.TelemetryClient;

public final class TelemetryClientSingleton {
    private TelemetryClient telemetry = null;
    private static String appInsightsKey = null;

    private static final class SingletonHolder {
        private static final TelemetryClientSingleton INSTANCE = init();
        private static TelemetryClientSingleton init(){
            return new TelemetryClientSingleton(appInsightsKey);
        }
    }

    public static TelemetryClient getTelemetry(String key){
        TelemetryClientSingleton.appInsightsKey = key;
        return SingletonHolder.INSTANCE.telemetry;
    }
        
    private TelemetryClientSingleton(String key){
        telemetry = new TelemetryClient();
        telemetry.getContext().setInstrumentationKey(key);
    }
}



