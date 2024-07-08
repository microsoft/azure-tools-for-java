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

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {

        // Reset the methodCounts map for each new visitor
        TelemetryClientProviderVisitor.methodCounts.clear();
        return new TelemetryClientProviderVisitor(holder, isOnTheFly);
    }


    public static class TelemetryClientProviderVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private final boolean isOnTheFly;

        static TelemetryClient telemetryClient = getTelemetryClient();
        private static Project project;

        static Map<String, Map<String, Integer>> methodCounts = new HashMap<>();


        // This constructor is used to create a visitor for the inspection
        public TelemetryClientProviderVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
            this.isOnTheFly = isOnTheFly;

            if (project == null) {
                System.out.println("Project is null. startTelemetryService() called.");

                startTelemetryService();
            }
            project = holder.getProject();

            System.out.println("Building visitor.");
        }

        //  will only track the methods that are being called in the code
        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);

            PsiReferenceExpression methodExpression = expression.getMethodExpression();
            String methodName = methodExpression.getReferenceName();
            String clientName = getClientName(expression); // Method to get client name

            if (methodName == null) {
                return;
            }

            if (clientName != null) {
                synchronized (methodCounts) {

                    // Increment the count of the method call for the client
                    methodCounts
                            .computeIfAbsent(clientName, k -> new HashMap<>())
                            .put(methodName, methodCounts.get(clientName).getOrDefault(methodName, 0) + 1);
                }
            }
        }

        private String getClientName(PsiMethodCallExpression expression) {
            // Logic to extract client name from the method call expression
            PsiExpression qualifier = expression.getMethodExpression().getQualifierExpression();
            if (qualifier != null) {
                PsiType type = qualifier.getType();
                if (type != null && type.getCanonicalText().startsWith("com.azure")) {
                    return type.getPresentableText();
                }
            }
            return null;
        }

        static void startTelemetryService() {

            System.out.println("Starting telemetry service.");

            // Using a single-threaded ScheduledExecutorService
            ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

            executorService.scheduleAtFixedRate(() ->
                    TelemetryClientProviderVisitor.sendTelemetryData(), 1, 2, TimeUnit.MINUTES);
        }

        /**
         * This method reads the instrumentation key from the applicationinsights.json file
         * and returns a TelemetryClient object with the instrumentation key set.
         * This object is used to send telemetry data to Application Insights.
         *
         * @return
         */
        public static TelemetryClient getTelemetryClient() {

            // Create a new TelemetryClient object
            TelemetryClient telemetry;
            telemetry = new TelemetryClient();

            String instrumentationKey = null;

            // Read the instrumentation key from the applicationinsights.json file
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

        static void sendTelemetryData() {
            System.out.println("Sending telemetry data sent at " + new Date().toString());

            System.out.println("methodCounts: " + methodCounts);

            // Outer loop: Iterate over each client entry in the methodCounts map
            for (Map.Entry<String, Map<String, Integer>> clientEntry : methodCounts.entrySet()) {
                String clientName = clientEntry.getKey();  // Extract the client name
                Map<String, Integer> methods = clientEntry.getValue();  // Extract the methods map for this client

                // Inner loop: Iterate over each method entry in the methods map
                for (Map.Entry<String, Integer> methodEntry : methods.entrySet()) {
                    String methodName = methodEntry.getKey();  // Extract the method name
                    int count = methodEntry.getValue();  // Extract the call count for this method

                    // Report as metric
                    telemetryClient.trackMetric("azure_sdk_usage_frequency", (double) count);

                    // Create custom dimensions map
                    Map<String, String> customDimensions = new HashMap<>();
                    customDimensions.put("clientName", clientName);
                    customDimensions.put("methodName", methodName);

                    /// Convert count to a double and create a properties map
                    Map<String, Double> properties = new HashMap<>();
                    properties.put("count", (double) count);

                    // Report as event
                    telemetryClient.trackEvent("azure_sdk_usage_frequency", customDimensions, properties);
                }
            }
            telemetryClient.flush();
        }
    }
}