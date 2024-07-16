package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;

import java.io.FileNotFoundException;

import com.intellij.psi.PsiVariable;
import com.microsoft.applicationinsights.TelemetryClient;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class reads the instrumentation key from the applicationInsights.json file
 * and returns a TelemetryClient object with the instrumentation key set.
 * This object is used to send telemetry data to Application Insights.
 */
public class TelemetryClientProvider extends LocalInspectionTool {

    /**
     * This method is called by the IntelliJ platform to build a visitor for the inspection.
     *
     * @param holder     The ProblemsHolder object that holds the problems found in the code.
     * @param isOnTheFly A boolean that indicates if the inspection is running on the fly.
     * @return A PsiElementVisitor object that visits the method calls in the code.
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {

        // Reset the methodCounts map for each new visitor
        TelemetryClientProviderVisitor.methodCounts.clear();
        TelemetryClientProviderVisitor visitor = new TelemetryClientProviderVisitor(holder, isOnTheFly);

        // Set the visitor instance in TelemetryToggleAction
        TelemetryToggleAction.setTelemetryService(visitor);

        return visitor;
    }

    /**
     * This class is a visitor that visits the method calls in the code and tracks the method calls.
     */
    static class TelemetryClientProviderVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private static boolean running = false; // Flag to indicate if the telemetry service is running
        private static ScheduledExecutorService executorService; // Executor service to schedule telemetry data sending

        // Create a TelemetryClient object
        // not final because the test involves Injecting the mock telemetry client to telemetryClient
        // Package-private to allow access from tests in the same package
        static TelemetryClient telemetryClient = getTelemetryClient();
        // Create a map to store the method counts
        static Map<String, Map<String, Integer>> methodCounts = new HashMap<>();

        // Create a Project object
        private static Project project;

        // Create a list of prefixes for Azure service method calls
        // source: https://azure.github.io/azure-sdk/java_introduction.html#service-methods
        private static final List<String> AZURE_METHOD_PREFIXES = Arrays.asList("upsert", "set", "create", "update", "replace", "delete", "add", "get", "list", "upload");

        // Create a logger object
        private static final Logger LOGGER = Logger.getLogger(TelemetryClientProvider.class.getName());

        // Create a RuleConfig object
        private static final RuleConfig ruleConfig;


        static {
            final String ruleName = "TelemetryClientProvider";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            // There is no rule associated with this inspection, so the ruleConfig is set to an empty RuleConfig object
            // to access RuleConfig.AZURE_PACKAGE_NAME
            ruleConfig = centralRuleConfigLoader.getRuleConfig(ruleName);
        }

        /**
         * This constructor is used to create a visitor for the inspection
         * It initializes the holder and isOnTheFly fields.
         *
         * @param holder     The ProblemsHolder object that holds the problems found in the code.
         * @param isOnTheFly A boolean that indicates if the inspection is running on the fly. - This is not used in this implementation.
         */
        TelemetryClientProviderVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;

            // Initialize start telemetry service when project is null
            // This is to ensure that the telemetry service is started only once
            if (project == null) {
                startTelemetryService();
            }
            project = holder.getProject();
        }

        /**
         * This method is called by the IntelliJ platform to visit the elements in the code.
         * It visits the method calls in the code and tracks the method calls.
         *
         * @param element The element to be visited.
         */
        @Override
        public void visitElement(PsiElement element) {
            super.visitElement(element);

            // Handle method call expressions
            if (element instanceof PsiMethodCallExpression) {

                PsiMethodCallExpression methodCall = (PsiMethodCallExpression) element;

                PsiReferenceExpression methodExpression = methodCall.getMethodExpression();
                String methodName = methodExpression.getReferenceName();
                String clientName = getClientName(methodCall); // Method to get client name

                if (clientName == null) {
                    return;
                }

                if (methodName == null || !isAzureServiceMethodCall(methodName)) {
                    return;
                }

                synchronized (methodCounts) {

                    // Increment the count of the method call for the client
                    methodCounts.computeIfAbsent(clientName, k -> new HashMap<>()).put(methodName, methodCounts.get(clientName).getOrDefault(methodName, 0) + 1);
                }
            }
        }


        /**
         * This method is used to get the client name from the method call expression.
         * It extracts the client name from the method call expression.
         *
         * @param expression The method call expression from which the client name is extracted.
         * @return The client name extracted from the method call expression.
         */
        private String getClientName(PsiMethodCallExpression expression) {

            PsiExpression qualifier = expression.getMethodExpression().getQualifierExpression();

            if (!(qualifier instanceof PsiReferenceExpression)) {
                return null;
            }

            PsiElement resolvedElement = ((PsiReferenceExpression) qualifier).resolve();

            if (!(resolvedElement instanceof PsiVariable)) {
                return null;
            }

            PsiVariable variable = (PsiVariable) resolvedElement;
            PsiType type = variable.getType();

            if (!(type instanceof PsiClassType)) {
                return null;
            }
            PsiClassType classType = (PsiClassType) type;

            PsiClass psiClass = classType.resolve();

            if (psiClass == null) {
                return null;
            }
            String className = psiClass.getQualifiedName();

            if (isAzureSdkClient(className)) {
                return classType.getPresentableText();
            }
            return null;
        }


        /**
         * This method checks if the class name is an Azure SDK client.
         * It checks if the class name starts with "com.azure." and ends with "Client".
         *
         * @param className The class name to be checked.
         * @return A boolean indicating if the class name is an Azure SDK client.
         */
        private boolean isAzureSdkClient(String className) {
            return className != null && className.startsWith(RuleConfig.AZURE_PACKAGE_NAME) && className.endsWith("Client");
        }

        /**
         * This method checks if the method call is an Azure service method call.
         * It checks if the method name starts with any of the prefixes in the AZURE_METHOD_PREFIXES list.
         *
         * @param methodName The method name to be checked.
         * @return A boolean indicating if the method call is an Azure service method call.
         */
        private boolean isAzureServiceMethodCall(String methodName) {
            for (String prefix : AZURE_METHOD_PREFIXES) {
                if (methodName.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * This method starts the telemetry service.
         * It creates a single-threaded ScheduledExecutorService and
         * schedules the telemetry data to be sent every 2 minutes.
         */
        static void startTelemetryService() {

            if (!running) {
                running = true;
                executorService = Executors.newSingleThreadScheduledExecutor();

                executorService.scheduleAtFixedRate(() -> TelemetryClientProviderVisitor.sendTelemetryData(), 2, 3, TimeUnit.MINUTES);
            }
        }

        /**
         * This method stops the telemetry service.
         * It shuts down the executor service.
         */
        static void stopTelemetryService() {
            if (running) {
                running = false;
                if (executorService != null) {
                    executorService.shutdown();
                }
            }
        }

        public static boolean isRunning() {
            return running;
        }

        /**
         * This method sends the telemetry data to Application Insights.
         * It sends the method counts as events.
         */
        static void sendTelemetryData() {

            // Outer loop: Iterate over each client entry in the methodCounts map
            for (Map.Entry<String, Map<String, Integer>> clientEntry : methodCounts.entrySet()) {
                String clientName = clientEntry.getKey();  // Extract the client name
                Map<String, Integer> methods = clientEntry.getValue();  // Extract the methods map for this client

                // Inner loop: Iterate over each method entry in the methods map
                for (Map.Entry<String, Integer> methodEntry : methods.entrySet()) {
                    String methodName = methodEntry.getKey();  // Extract the method name
                    int count = methodEntry.getValue();  // Extract the call count for this method

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

        /**
         * This method reads the instrumentation key from the applicationInsights.json file
         * and returns a TelemetryClient object with the instrumentation key set.
         * This object is used to send telemetry data to Application Insights.
         *
         * @return A TelemetryClient object with the instrumentation key set.
         */
        static TelemetryClient getTelemetryClient() {

            String configFilePath = "META-INF/applicationInsights.json";
            String instrumentationKeyJsonKey = "instrumentationKey";

            // Create a new TelemetryClient object
            TelemetryClient telemetry = new TelemetryClient();
            StringBuilder jsonBuilder = new StringBuilder();

            // Read the instrumentation key from the applicationInsights.json file
            try (InputStream inputStream = TelemetryClient.class.getClassLoader().getResourceAsStream(configFilePath); BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                if (inputStream == null) {
                    LOGGER.log(Level.SEVERE, "Configuration file not found at path: " + configFilePath + ". Please ensure the file exists and is accessible.", new FileNotFoundException());
                    return telemetry; // Return the telemetry client even if the config file is not found
                }

                // while loop to read the json file
                // this is more memory efficient than reading the entire file at once and holding it in memory
                String line;
                while ((line = reader.readLine()) != null) {
                    jsonBuilder.append(line).append("\n");
                }

                JSONObject jsonObject = new JSONObject(jsonBuilder.toString());
                String instrumentationKey = jsonObject.getString(instrumentationKeyJsonKey);
                telemetry.getContext().setInstrumentationKey(instrumentationKey);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error while loading instrumentation key" + ". Please investigate further.", e);
            }
            return telemetry;
        }
    }
}
