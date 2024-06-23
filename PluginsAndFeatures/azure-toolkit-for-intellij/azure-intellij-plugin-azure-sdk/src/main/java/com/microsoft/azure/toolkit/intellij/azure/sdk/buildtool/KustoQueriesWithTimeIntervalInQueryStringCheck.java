package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiVariable;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class is an inspection tool that checks for Kusto queries with time intervals in the query string.
 * This approach makes queries less flexible and harder to troubleshoot.
 * This inspection tool checks for the following anti-patterns:
 * 1. Queries that use the "ago" function with a time interval.
 * 2. Queries that use the "datetime" function with a specific datetime.
 * 3. Queries that use the "now" function to get the current timestamp.
 * 4. Queries that use the "startofday", "startofmonth", or "startofyear" functions.
 * 5. Queries that use the "between" function with datetime values.
 */
public class KustoQueriesWithTimeIntervalInQueryStringCheck extends LocalInspectionTool {

    // This method builds the visitor for the inspection tool

    /**
     * This method builds the visitor for the inspection tool
     * @param holder ProblemsHolder is used to register problems found in the code
     * @param isOnTheFly boolean to indicate if the inspection is done on the fly
     * @return PsiElementVisitor
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new KustoQueriesVisitor(holder, isOnTheFly);
    }

    /**
     * This class defines the visitor for the inspection tool
     * The visitor checks for Kusto queries with time intervals in the query string
     * and registers a problem if an anti-pattern is detected
     * To check for the anti-patterns, the visitor uses regex patterns to match the query string
     * Processing of polyadic expressions is also done to replace the variables with their values in the query string
     * before checking for the anti-patterns.
     */
    public static class KustoQueriesVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private final boolean isOnTheFly;


        // Define constants for string literals
        private static final String RULE_CONFIGURATION = "META-INF/ruleConfigs.json";
        private static final String ANTI_PATTERN_KEY = "antipattern_message";
        private static final String REGEX_PATTERNS_KEY = "regex_patterns";
        private static final Map<String, List<String>> ruleConfigurations = loadRuleConfigurations();

        private static final String ANTI_PATTERN = ruleConfigurations.get(ANTI_PATTERN_KEY).get(0);

        // Define your patterns
        Pattern KQL_ANTI_PATTERN_AGO = Pattern.compile(loadRuleConfigurations().get(REGEX_PATTERNS_KEY).get(0));
        Pattern KQL_ANTI_PATTERN_DATETIME = Pattern.compile(loadRuleConfigurations().get(REGEX_PATTERNS_KEY).get(1));
        Pattern KQL_ANTI_PATTERN_NOW = Pattern.compile(loadRuleConfigurations().get(REGEX_PATTERNS_KEY).get(2));
        Pattern KQL_ANTI_PATTERN_START_OF_PERIOD = Pattern.compile(loadRuleConfigurations().get(REGEX_PATTERNS_KEY).get(3));
        Pattern KQL_ANTI_PATTERN_BETWEEN = Pattern.compile(loadRuleConfigurations().get(REGEX_PATTERNS_KEY).get(4));

        /**
         * Constructor for the KustoQueriesVisitor class
         * The constructor initializes the ProblemsHolder and isOnTheFly variables
         * @param holder
         * @param isOnTheFly
         */
        public KustoQueriesVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
            this.isOnTheFly = isOnTheFly;
        }

        /**
         * This method visits the element and checks for the anti-patterns
         * The method checks if the element is a PsiPolyadicExpression or a PsiLocalVariable
         * If the element is a PsiLocalVariable, the method checks the initializer of the variable
         * If the element is a PsiPolyadicExpression, the method processes the expression to replace the variables with their values
         * The method then checks the expression for the anti-patterns by matching regex patterns with the expression text
         * and registers a problem if an anti-pattern is detected
         * @param element
         */
        @Override
        public void visitElement(@NotNull PsiElement element) {
            super.visitElement(element);

            if (!(element instanceof PsiPolyadicExpression || element instanceof PsiLocalVariable)) {
                return;
            }
            PsiExpression initializer = null;
            PsiPolyadicExpression originalExpression = null;

            if (!(element instanceof PsiPolyadicExpression)) {
                PsiLocalVariable variable = (PsiLocalVariable) element;
                initializer = variable.getInitializer();
                if (initializer != null && initializer instanceof PsiLiteralExpression) {
                    String text = initializer.toString();
                    checkExpression(initializer, element, holder);
                }
            }
            // if element is a polyadic expression, extract the value.
            // PsiPolyadicExpressions are used to represent expressions with multiple operands
            // eg ("datetime" + startDate), where startDate is a variable
             else if (element instanceof PsiPolyadicExpression) {
                // process the polyadic expression to replace the variables with their values
                PsiElement processedElement = processPsiPolyadicExpressions(element.copy());
                initializer = processedElement instanceof PsiPolyadicExpression ? (PsiPolyadicExpression) processedElement : null;
                checkExpression(initializer, element, holder);
            }
        }

        /** This method checks the expression for the anti-patterns by matching regex patterns with the expression text
         * and registers a problem if an anti-pattern is detected
         * @param expression
         * @param element
         * @param holder
         */
        void checkExpression(PsiExpression expression, PsiElement element, ProblemsHolder holder) {
            if (expression == null) {
                return;
            }
            String text = expression.getText();
            if (
                    KQL_ANTI_PATTERN_AGO.matcher(text).find() ||
                    KQL_ANTI_PATTERN_DATETIME.matcher(text).find() ||
                    KQL_ANTI_PATTERN_NOW.matcher(text).find() ||
                    KQL_ANTI_PATTERN_START_OF_PERIOD.matcher(text).find() ||
                    KQL_ANTI_PATTERN_BETWEEN.matcher(text).find()
            ) {
                holder.registerProblem(element, ANTI_PATTERN);
            }
        }

        /** This method processes the polyadic expressions to replace the variables with their values
         * @param element
         * @return PsiElement
         */
        public PsiElement processPsiPolyadicExpressions(PsiElement element) {

            // Extract the operands from the polyadic expression
            PsiPolyadicExpression expression = (PsiPolyadicExpression) element;
            PsiExpression[] operands = expression.getOperands();

            // Iterate through the operands
            for (int i = 0; i < operands.length; i++) {
                PsiExpression operand = operands[i];

                // PsiReferenceExpressions are used to represent references to variables
                if (!(operand instanceof PsiReferenceExpression)) {
                    continue;
                }
                PsiReferenceExpression reference = (PsiReferenceExpression) operand;
                PsiElement resolved = reference.resolve();
                if (!(resolved instanceof PsiVariable)) {
                    continue;
                }
                // Extract the value of the variable
                // Resolve means to find the declaration of the variable
                // Initializer is the value assigned to the variable
                PsiVariable variable = (PsiVariable) resolved;
                PsiExpression initializer = variable.getInitializer();
                if (initializer == null) {
                    continue;
                }
                // Initializer.getText() returns the value of the variable in string format
                // Create a new expression from the variable value and replace the variable with the value
                String variableValue = initializer.getText();
                PsiElementFactory factory = JavaPsiFacade.getInstance(element.getProject()).getElementFactory();
                PsiExpression newExpression = factory.createExpressionFromText(variableValue, null);
                if (newExpression != null) {
                    operands[i] = newExpression;
                }
            }
            // Create a new expression from the operands after replacing the variables with their values
            // Return the new expression if it is not null, else return the original element
            PsiElementFactory factory = JavaPsiFacade.getInstance(element.getProject()).getElementFactory();
            String newExpressionText = StringUtil.join(operands, PsiElement::getText, "+");
            PsiExpression newExpression = factory.createExpressionFromText(newExpressionText, null);
            return newExpression != null ? newExpression : element;
        }


        /** This method loads the rule configurations from the configuration file
         * The configuration file contains the regex patterns for the anti-patterns
         * and the message to display when an anti-pattern is detected
         * @return Map<String, List<String>>
         */
        private static Map<String, List<String>> loadRuleConfigurations() {

            // move magic strings to constants
            final String KQL_ANTI_PATTERN_AGO = "KQL_ANTI_PATTERN_AGO";
            final String KQL_ANTI_PATTERN_DATETIME = "KQL_ANTI_PATTERN_DATETIME";
            final String KQL_ANTI_PATTERN_NOW = "KQL_ANTI_PATTERN_NOW";
            final String KQL_ANTI_PATTERN_START_OF_PERIOD = "KQL_ANTI_PATTERN_START_OF_PERIOD";
            final String KQL_ANTI_PATTERN_BETWEEN = "KQL_ANTI_PATTERN_BETWEEN";

            try {
                final InputStream inputStream = KustoQueriesWithTimeIntervalInQueryStringCheck.class.getClassLoader().getResourceAsStream(RULE_CONFIGURATION);
                if (inputStream == null) {
                    throw new FileNotFoundException("Configuration file not found");
                }
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                    final JSONObject jsonObject = new JSONObject(bufferedReader.lines().collect(Collectors.joining()));
                    final JSONObject kustoCheckObject = jsonObject.getJSONObject("KustoQueriesWithTimeIntervalInQueryStringCheck");
                    final JSONObject regexPatterns = kustoCheckObject.getJSONObject(REGEX_PATTERNS_KEY);

                    final Map<String, List<String>> ruleConfigurations = new HashMap<>();
                    ruleConfigurations.put(REGEX_PATTERNS_KEY, Arrays.asList(
                            regexPatterns.getString(KQL_ANTI_PATTERN_AGO),
                            regexPatterns.getString(KQL_ANTI_PATTERN_DATETIME),
                            regexPatterns.getString(KQL_ANTI_PATTERN_NOW),
                            regexPatterns.getString(KQL_ANTI_PATTERN_START_OF_PERIOD),
                            regexPatterns.getString(KQL_ANTI_PATTERN_BETWEEN)
                    ));
                    ruleConfigurations.put(ANTI_PATTERN_KEY, Collections.singletonList(jsonObject.getJSONObject("KustoQueriesWithTimeIntervalInQueryStringCheck").getString(ANTI_PATTERN_KEY)));
                    return ruleConfigurations;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return Collections.emptyMap();
            }
        }
    }
}

