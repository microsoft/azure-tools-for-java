package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiNewExpression;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * This class is a custom inspection tool that checks for hardcoded API keys and tokens in the code.
 * It extends the LocalInspectionTool class and overrides the buildVisitor method to create a visitor for the inspection.
 * The visitor checks for the use of specific methods that are known to be used for API keys and tokens.
 * These are AzureKeyCredential and AccessToken.
 *
 * These are some instances that a flag would be raised.
 * 1. TextAnalyticsClient client = new TextAnalyticsClientBuilder()
 *         .endpoint(endpoint)
 *         .credential(new AzureKeyCredential(apiKey))
 *         .buildClient();
 *
 * 2. TokenCredential credential = request -> {
 *         AccessToken token = new AccessToken("<your-hardcoded-token>", OffsetDateTime.now().plusHours(1));
 *     }
 */
public class HardcodedAPIKeysAndTokensCheck extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new APIKeysAndTokensVisitor(holder, isOnTheFly);
    }

    // This class is a visitor that checks for the use of specific methods that authenticate API keys and tokens.
    public static class APIKeysAndTokensVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private final boolean isOnTheFly;

        // Load rule configurations from a JSON file
        static Map<String, Object> RULE_CONFIGURATIONS = loadRuleConfigurations();
        static String ANTI_PATTERN_MESSAGE = (String) RULE_CONFIGURATIONS.get("anti_pattern_message");
        static List<String> CLIENTS_TO_CHECK = (List<String>) RULE_CONFIGURATIONS.get("clients_to_check");

        // This constructor is used to create a visitor for the inspection
        public APIKeysAndTokensVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
            this.isOnTheFly = isOnTheFly;
        }

        @Override
        public void visitElement(@NotNull PsiElement element) {
            super.visitElement(element);

            // Check if the element is a new expression -- i.e., a constructor call
            if (element instanceof PsiNewExpression) {

                // Cast the element to a new expression
                PsiNewExpression newExpression = (PsiNewExpression) element;

                // Get the class reference name from the new expression
                String classReference = newExpression.getClassReference().getReferenceName();

                // Check if the class reference is not null, the qualifier name starts with "com.azure" and
                // the class reference is in the list of clients to check
                if (newExpression.getClassReference() != null
                        && newExpression.getClassReference().getQualifiedName().startsWith("com.azure")
                        && CLIENTS_TO_CHECK.contains(classReference)) {
                    this.holder.registerProblem(newExpression, ANTI_PATTERN_MESSAGE);
                }
            }
        }


        // Helper method to load rule configurations from a JSON file
        private static Map<String, Object> loadRuleConfigurations() {

            // Define constants for string literals
            final String RULE_KEY = "HardcodedAPIKeysAndTokensCheck";
            final String ANTI_PATTERN_MESSAGE_KEY = "anti_pattern_message";
            final String CLIENTS_TO_CHECK_KEY = "clients_to_check";
            final String RULE_CONFIGURATION = "META-INF/ruleConfigs.json";

            // Load the rule configurations from the JSON file
            try (InputStream inputStream = HardcodedAPIKeysAndTokensCheck.class.getClassLoader().getResourceAsStream(RULE_CONFIGURATION)) {
                if (inputStream == null) {
                    throw new FileNotFoundException("Configuration file not found");
                }
                // Read the JSON object from the input stream
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                    final JSONObject jsonObject = new JSONObject(bufferedReader.lines().collect(Collectors.joining()));
                    final JSONObject apiKeysAndTokensObject = jsonObject.getJSONObject(RULE_KEY);
                    final String antiPatternMessage = apiKeysAndTokensObject.getString(ANTI_PATTERN_MESSAGE_KEY);
                    final List<String> clientsToCheck = apiKeysAndTokensObject.getJSONArray(CLIENTS_TO_CHECK_KEY).toList().stream().map(Object::toString).collect(Collectors.toList());
                    return Map.of(ANTI_PATTERN_MESSAGE_KEY, antiPatternMessage, CLIENTS_TO_CHECK_KEY, clientsToCheck);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.emptyMap();
            }
        }
    }
}
