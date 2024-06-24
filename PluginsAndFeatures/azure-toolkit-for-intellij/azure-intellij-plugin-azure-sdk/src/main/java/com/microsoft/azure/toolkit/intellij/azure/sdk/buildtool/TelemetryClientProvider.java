package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.microsoft.applicationinsights.TelemetryClient;
import java.io.FileNotFoundException;

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.*;
import java.util.stream.Collectors;


/**
 * This class reads the instrumentation key from the applicationinsights.json file
 * and returns a TelemetryClient object with the instrumentation key set.
 * This object is used to send telemetry data to Application Insights.
 */
public class TelemetryClientProvider extends LocalInspectionTool {

    private static final TelemetryClient telemetryClient = getTelemetryClient();
    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private static Project project;

    private static final Map<String, Integer> methodCounts = new HashMap<>();

    public static void setProject(Project project) {
        System.out.println("Setting project.");
        TelemetryClientProvider.project = project;
        startTelemetryService();
    }

    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        Project project = holder.getProject();
        TelemetryClientProvider.setProject(project);
        // Reset the methodCounts map for each new visitor
        methodCounts.clear();
        return new JavaElementVisitor() {

            //  will only track the methods that are being called in the code
            @Override
            public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);

                PsiReferenceExpression methodExpression = expression.getMethodExpression();
                String methodName = methodExpression.getReferenceName();
                System.out.println("Method tracked.");

                synchronized (methodCounts) {
                    // Increment the count of the method call
                    methodCounts.put(methodName, methodCounts.getOrDefault(methodName, 0) + 1);
                }
                System.out.println(methodCounts.toString() + " method counts.");
            }
        };
    }


    private static void sendTelemetryData() {
        System.out.println("Sending telemetry data sent at." + new Date().toString());

        // print the time of the last event (using this to verify that the telemetry is being sent)
        telemetryClient.trackEvent("Telemetry data sent at " + new Date().toString());

        System.out.println(methodCounts.toString() + " sendTelemetryData method counts.");

        for (Map.Entry<String, Integer> entry : methodCounts.entrySet()) {
            String methodName = entry.getKey();
            int count = entry.getValue();

            telemetryClient.trackMetric("Method in use " + methodName, count);
            telemetryClient.trackEvent("Method in use " + methodName);
        }

        telemetryClient.flush();
    }

    private static void startTelemetryService() {
        executorService.scheduleAtFixedRate(() ->
            TelemetryClientProvider.sendTelemetryData(), 2, 3, TimeUnit.MINUTES);
    }


    public static TelemetryClient getTelemetryClient() {

        TelemetryClient telemetry;
        telemetry = new TelemetryClient();

        String instrumentationKey = null;

        try {
            InputStream inputStream = TelemetryClient.class.getClassLoader().getResourceAsStream("META-INF/applicationinsights.json");
            if (inputStream == null) {
                throw new FileNotFoundException("File not found on classpath");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String json = reader.lines().collect(Collectors.joining("\n"));
            reader.close();

            JSONObject jsonObject = new JSONObject(json);
            instrumentationKey = jsonObject.getString("instrumentationKey");

            System.out.println(instrumentationKey);

        } catch (Exception e) {
            throw new RuntimeException("Could not read the instrumentation key from the applicationinsights.json file.");
        }
        telemetry.getContext().setInstrumentationKey(instrumentationKey);
        return telemetry;
    }
}
