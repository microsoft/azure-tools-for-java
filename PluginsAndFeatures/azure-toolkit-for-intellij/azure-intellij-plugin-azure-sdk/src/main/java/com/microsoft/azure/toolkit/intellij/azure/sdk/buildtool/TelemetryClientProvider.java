package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;


import com.microsoft.applicationinsights.TelemetryClient;
import java.io.FileNotFoundException;

import java.util.Timer;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.TimerTask;
import java.util.stream.Collectors;

/**
 * This class reads the instrumentation key from the applicationinsights.json file
 * and returns a TelemetryClient object with the instrumentation key set.
 * This object is used to send telemetry data to Application Insights.
 * The trackMemoryUsage method is used to track the memory usage of the application.
 * The start method is used to start a timer that runs the trackMemoryUsage method every 10 seconds.
 */
public class TelemetryClientProvider {

    public static TelemetryClient getTelemetryClient() {

        TelemetryClient telemetry;
        telemetry = new TelemetryClient();

        String instrumentationKey = null;

        try {
            InputStream inputStream = ConnectionStringCheck.class.getClassLoader().getResourceAsStream("META-INF/applicationinsights.json");
            if (inputStream == null) {
                throw new FileNotFoundException("File not found on classpath");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String json = reader.lines().collect(Collectors.joining("\n"));
            reader.close();

            JSONObject jsonObject = new JSONObject(json);
            instrumentationKey = jsonObject.getString("instrumentationKey");

        } catch (Exception e) {
            throw new RuntimeException("Could not read the instrumentation key from the applicationinsights.json file.");
        }
        telemetry.getContext().setInstrumentationKey(instrumentationKey);
        return telemetry;
    }

    public static void trackMemoryUsage(TelemetryClient telemetry) {

        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();

        telemetry.trackMetric("UsedMemory", usedMemory);
        telemetry.flush();
    }

    public static void start() {
        Timer timer = new Timer();
        timer.schedule(new TelemetryTask(), 0, 60000);
    }

    private static class TelemetryTask extends TimerTask {
        @Override
        public void run() {
            TelemetryClient telemetry = TelemetryClientProvider.getTelemetryClient();
            TelemetryClientProvider.trackMemoryUsage(telemetry);
        }
    }
}
