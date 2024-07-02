package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.monitor.query.LogsQueryClient;
import com.azure.monitor.query.LogsQueryClientBuilder;
import com.azure.monitor.query.models.LogsQueryResult;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.impl.source.PsiParameterImpl;
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.DayOfWeek;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalQuery;
import java.util.ArrayList;
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
 *
 * When the anti-patterns are detected as parameters of Azure client method calls, a problem is registered.
 */
public class KustoQueriesWithTimeIntervalInQueryStringCheck extends LocalInspectionTool {

    // This method builds the visitor for the inspection tool

    /**
     * This method builds the visitor for the inspection tool
     *
     * @param holder     ProblemsHolder is used to register problems found in the code
     * @param isOnTheFly boolean to indicate if the inspection is done on the fly
     * @return PsiElementVisitor
     */
    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
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

        // empty list to store time interval parameters
        private List<String> timeIntervalParameters = new ArrayList<>();

        // Define your patterns
        Pattern KQL_ANTI_PATTERN_AGO = Pattern.compile(loadRuleConfigurations().get(REGEX_PATTERNS_KEY).get(0));
        Pattern KQL_ANTI_PATTERN_DATETIME = Pattern.compile(loadRuleConfigurations().get(REGEX_PATTERNS_KEY).get(1));
        Pattern KQL_ANTI_PATTERN_NOW = Pattern.compile(loadRuleConfigurations().get(REGEX_PATTERNS_KEY).get(2));
        Pattern KQL_ANTI_PATTERN_START_OF_PERIOD = Pattern.compile(loadRuleConfigurations().get(REGEX_PATTERNS_KEY).get(3));
        Pattern KQL_ANTI_PATTERN_BETWEEN = Pattern.compile(loadRuleConfigurations().get(REGEX_PATTERNS_KEY).get(4));

        /**
         * Constructor for the KustoQueriesVisitor class
         * The constructor initializes the ProblemsHolder and isOnTheFly variables
         *
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
         *
         * @param element
         */


        @Override
        public void visitElement(@NotNull PsiElement element) {
            super.visitElement(element);

            // Check if the element is a PsiPolyadicExpression or a PsiLocalVariable
            if (!(element instanceof PsiPolyadicExpression
                    || element instanceof PsiLocalVariable
                    || element instanceof PsiMethodCallExpressionImpl)) {
                return;
            }

            if (element instanceof PsiMethodCallExpressionImpl) {
                System.out.println("Element is a PsiMethodCallExpressionImpl");
                handleMethodCall((PsiMethodCallExpressionImpl) element);
            }

            if (element instanceof PsiLocalVariable) {
                System.out.println("Element is a PsiLocalVariable");

                PsiLocalVariable variable = (PsiLocalVariable) element;
                handleLocalVariable(variable);
            }

            // if element is a polyadic expression, extract the value.
            // PsiPolyadicExpressions are used to represent expressions with multiple operands
            // eg ("datetime" + startDate), where startDate is a variable
            if (element instanceof PsiPolyadicExpression) {
                System.out.println("Element is a PsiPolyadicExpression");
                handlePolyadicExpression((PsiPolyadicExpression) element);
            }
        }

        private void handleLocalVariable(PsiLocalVariable variable) {
            System.out.println("Element is a PsiLocalVariable");

            PsiExpression initializer = variable.getInitializer();
            String variableName = variable.getName();
            System.out.println("Variable name: " + variableName);
            if (initializer != null && initializer instanceof PsiLiteralExpression) {
                checkExpression(initializer, variable);
            }
        }

        private void handlePolyadicExpression(PsiPolyadicExpression polyadicExpression) {
            System.out.println("Element is a PsiPolyadicExpression");

            // Process the polyadic expression to replace the variables with their values
            PsiElement processedElement = processPsiPolyadicExpressions(polyadicExpression.copy());
            PsiExpression initializer = processedElement instanceof PsiPolyadicExpression ? (PsiPolyadicExpression) processedElement : null;
            String variableName = polyadicExpression.getText();
            System.out.println("Variable name: " + variableName);
            checkExpression(initializer, polyadicExpression);
        }

        private void handleMethodCall(PsiMethodCallExpressionImpl methodCall) {
//            System.out.println("Method call: " + methodCall.getText());

            // check the parameters of the method call
            PsiExpressionList argumentList = methodCall.getArgumentList();
            PsiExpression[] arguments = argumentList.getExpressions();
            System.out.println("Arguments: " + Arrays.toString(arguments));

            for (PsiExpression argument : arguments) {
                if (argument instanceof PsiReferenceExpression) {
                    PsiReferenceExpression referenceExpression = (PsiReferenceExpression) argument;
                    PsiElement resolvedElement = referenceExpression.resolve();
                    System.out.println("Resolved element: " + resolvedElement);

                    if (resolvedElement instanceof PsiVariable) {
                        PsiVariable variable = (PsiVariable) resolvedElement;
                        System.out.println("Variable: " + variable);

                        String variableName = variable.getName();
                        System.out.println("Variable name: " + variableName);
                        if (timeIntervalParameters.contains(variableName)) {
                            if (isAzureClient(methodCall)) {
                                System.out.println("Azure client method call found:");
                                holder.registerProblem(methodCall, "KQL queries with time intervals in the query string detected.");
                            }
                        }
                    }
                }
            }
        }

        /**
         * This method checks the expression for the anti-patterns by matching regex patterns with the expression text
         * and registers a problem if an anti-pattern is detected
         *
         * @param expression
         * @param element
         */
        void checkExpression(PsiExpression expression, PsiElement element) {
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

                PsiElement parentElement = element.getParent();
                PsiLocalVariable variable = (PsiLocalVariable) parentElement;
                String variableName = variable.getName();
                System.out.println("Variable name: " + variableName);

                timeIntervalParameters.add(variableName);
            }
        }

        /**
         * This method processes the polyadic expressions to replace the variables with their values
         *
         * @param element
         * @return PsiElement
         */
        public PsiElement processPsiPolyadicExpressions(PsiElement element) {

            // Extract the operands from the polyadic expression
            PsiPolyadicExpression expression = (PsiPolyadicExpression) element;
            System.out.println("Polyadic expression: " + expression);

            PsiExpression[] operands = expression.getOperands();
            System.out.println("Operands: " + Arrays.toString(operands));

            // Iterate through the operands
            for (int i = 0; i < operands.length; i++) {
                PsiExpression operand = operands[i];
                System.out.println("Operand: " + operand);

                // PsiReferenceExpressions are used to represent references to variables
                if (!(operand instanceof PsiReferenceExpression)) {
                    continue;
                }
                PsiReferenceExpression reference = (PsiReferenceExpression) operand;
                System.out.println("Reference: " + reference);

                PsiElement resolved = reference.resolve();
                System.out.println("Resolved: " + resolved);

                if (!(resolved instanceof PsiVariable)) {
                    continue;
                }
                // Extract the value of the variable
                // Resolve means to find the declaration of the variable
                // Initializer is the value assigned to the variable
                PsiVariable variable = (PsiVariable) resolved;
                System.out.println("Variable: " + variable);

                PsiExpression initializer = variable.getInitializer();
                System.out.println("Initializer: " + initializer);

                if (initializer == null) {
                    continue;
                }
                // Initializer.getText() returns the value of the variable in string format
                // Create a new expression from the variable value and replace the variable with the value
                String variableValue = initializer.getText();
                System.out.println("Variable value: " + variableValue);
                System.out.println("element.getProject()" + element.getProject());
                PsiElementFactory factory = JavaPsiFacade.getInstance(element.getProject()).getElementFactory();
                System.out.println("Factory: " + factory);
                PsiExpression newExpression = factory.createExpressionFromText(variableValue, null);
                System.out.println("New expression: " + newExpression);
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


        /**
         * This method checks if the method call is an Azure client method call
         * by checking the type of the variable that the method is called on
         *
         * @param methodCall
         * @return boolean
         */
        private boolean isAzureClient(PsiMethodCallExpressionImpl methodCall) {

            System.out.println("timeIntervalParameters" + timeIntervalParameters);
            PsiExpression qualifierExpression = methodCall.getMethodExpression().getQualifierExpression();
            System.out.println("Qualifier expression: " + qualifierExpression);
//            System.out.println("Qualifier expressionText: " + qualifierExpression.getText());

            if (qualifierExpression instanceof PsiReferenceExpression) {
                PsiReferenceExpression referenceExpression = (PsiReferenceExpression) qualifierExpression;
                System.out.println("Reference expression: " + referenceExpression);

                PsiElement resolvedElement = referenceExpression.resolve();
                System.out.println("Resolved element: " + resolvedElement);

                if (resolvedElement instanceof PsiVariable) {
                    System.out.println("Resolved element is a variable");

                    PsiVariable variable = (PsiVariable) resolvedElement;
//                    System.out.println("Variable: " + variable.getName());

                    PsiType variableType = variable.getType();
                    System.out.println("Variable type: " + variableType.getCanonicalText());

                    return isAzureClientType(variableType);
                }
            }
            return false;
        }

        /**
         * This method checks if the type of the client is an Azure client type
         *
         * @param type
         * @return boolean
         */
        private boolean isAzureClientType(PsiType type) {
            String qualifiedName = type.getCanonicalText();
            System.out.println("Qualified name: " + qualifiedName);
            // You can extend this check to include more Azure client types
            return qualifiedName.startsWith("com.azure");
        }


        /**
         * This method loads the rule configurations from the configuration file
         * The configuration file contains the regex patterns for the anti-patterns
         * and the message to display when an anti-pattern is detected
         *
         * @return Map<String, List < String>>
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

