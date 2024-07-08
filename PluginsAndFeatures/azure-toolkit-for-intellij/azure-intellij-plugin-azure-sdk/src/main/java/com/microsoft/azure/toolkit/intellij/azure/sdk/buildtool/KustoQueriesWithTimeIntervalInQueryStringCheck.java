package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
    class KustoQueriesVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private final boolean isOnTheFly; // boolean to indicate if the inspection is done on the fly

        // Define constants for string literals
        private static final String RULE_CONFIGURATION = "META-INF/ruleConfigs.json";
        private static final String ANTI_PATTERN_KEY = "antipattern_message";
        private static final String REGEX_PATTERNS_KEY = "regex_patterns";
        private static final String RULE_NAME = "KustoQueriesWithTimeIntervalInQueryStringCheck";
        private static final Map<String, Pattern> regexPatterns = new HashMap<>();
        private static String ANTI_PATTERN_MESSAGE = null;

        // Load the regex patterns from the configuration file
        static {
            try {
                loadAndCompileRegexPatterns();
            } catch (IOException e) {
                throw new RuntimeException("Failed to load and compile regex patterns", e);
            }
        }

        // empty list to store time interval parameter names
        private List<String> timeIntervalParameters = new ArrayList<>();

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

            if (!(element instanceof PsiPolyadicExpression
                    || element instanceof PsiLocalVariable
                    || element instanceof PsiMethodCallExpressionImpl)) {
                return;
            }

            if (element instanceof PsiMethodCallExpressionImpl) {
                handleMethodCall((PsiMethodCallExpressionImpl) element);
            }

            if (element instanceof PsiLocalVariable) {
                PsiLocalVariable variable = (PsiLocalVariable) element;
                handleLocalVariable(variable);
            }

            // if element is a polyadic expression, extract the value and replace the variable with the value
            // PsiPolyadicExpressions are used to represent expressions with multiple operands
            // eg ("datetime" + startDate), where startDate is a variable
            if (element instanceof PsiPolyadicExpression) {
                handlePolyadicExpression((PsiPolyadicExpression) element);
            }
        }

        /**
         * This method handles the local variable by checking the initializer of the variable
         * If the initializer is a PsiLiteralExpression, the method checks the expression for the anti-patterns
         * by matching regex patterns with the expression text
         * @param variable
         */
        private void handleLocalVariable(PsiLocalVariable variable) {
            PsiExpression initializer = variable.getInitializer();
            String variableName = variable.getName();
            if (initializer != null && initializer instanceof PsiLiteralExpression) {
                checkExpression(initializer, variable);
            }
        }

        /**
         * This method handles the polyadic expression by processing the expression to replace the variables with their values
         * The method then checks the expression for the anti-patterns by matching regex patterns with the expression text
         * @param polyadicExpression
         */
        private void handlePolyadicExpression(PsiPolyadicExpression polyadicExpression) {

            // Process the polyadic expression to replace the variables with their values
            PsiElement processedElement = processPsiPolyadicExpressions(polyadicExpression.copy());
            PsiExpression initializer = processedElement instanceof PsiPolyadicExpression ? (PsiPolyadicExpression) processedElement : null;
            checkExpression(initializer, polyadicExpression);
        }

        /**
         * This method handles the method call by checking the parameters of the method call
         * If the parameter is a reference to a variable, the method checks the variable name
         * If the variable name is in the list of time interval parameters, the method checks if the method call is an Azure client method call
         * If the method call is an Azure client method call, the method registers a problem
         * @param methodCall
         */
        private void handleMethodCall(PsiMethodCallExpressionImpl methodCall) {
            // check the parameters of the method call
            PsiExpressionList argumentList = methodCall.getArgumentList();
            PsiExpression[] arguments = argumentList.getExpressions();

            // for each argument in the method call, check if the argument is a reference to a variable
            for (PsiExpression argument : arguments) {
                if (!(argument instanceof PsiReferenceExpression)) {
                    continue;
                }
                PsiReferenceExpression referenceExpression = (PsiReferenceExpression) argument;
                PsiElement resolvedElement = referenceExpression.resolve();

                if (!(resolvedElement instanceof PsiVariable)) {
                    continue;
                }
                PsiVariable variable = (PsiVariable) resolvedElement;
                String variableName = variable.getName();

                // check if the variable name is in the list of time interval parameters
                // if the variable name is in the list, check if the method call is an Azure client method call
                if (!(timeIntervalParameters.contains(variableName))) {
                    continue;
                }
                if (isAzureClient(methodCall)) {
                    holder.registerProblem(methodCall, "KQL queries with time intervals in the query string detected.");
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

            // Check if the expression text contains any of the regex patterns
            boolean foundAntiPattern = regexPatterns.values().stream()
                    .anyMatch(pattern -> pattern.matcher(text).find());

            // If an anti-pattern is detected, register a problem
            if (foundAntiPattern) {

                PsiElement parentElement = element.getParent();
                PsiLocalVariable variable = (PsiLocalVariable) parentElement;
                String variableName = variable.getName();

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


        /**
         * This method checks if the method call is an Azure client method call
         * by checking the type of the variable that the method is called on
         *
         * @param methodCall
         * @return boolean
         */
        private boolean isAzureClient(PsiMethodCallExpressionImpl methodCall) {

            // Get the qualifier expression of the method call
            PsiExpression qualifierExpression = methodCall.getMethodExpression().getQualifierExpression();

            // If the qualifier expression is a reference expression, resolve the reference
            if (qualifierExpression instanceof PsiReferenceExpression) {
                PsiReferenceExpression referenceExpression = (PsiReferenceExpression) qualifierExpression;
                PsiElement resolvedElement = referenceExpression.resolve();

                // If the resolved element is a variable, check if the type of the variable is an Azure client type
                if (resolvedElement instanceof PsiVariable) {
                    PsiVariable variable = (PsiVariable) resolvedElement;
                    PsiType variableType = variable.getType();

                    return variableType.getCanonicalText().startsWith("com.azure");
                }
            }
            return false;
        }

        /**
         * This method loads the regex patterns from the configuration file and compiles them
         * The method reads the JSON configuration file and extracts the regex patterns
         * The method then compiles the regex patterns and stores them in a map
         * The method also extracts the anti-pattern message from the configuration file
         * and stores it in a constant
         * @throws IOException
         */
        private static void loadAndCompileRegexPatterns() throws IOException {
            final JSONObject jsonObject = LoadJsonConfigFile.getInstance().getJsonObject();
            final JSONObject kustoCheckObject = jsonObject.getJSONObject(RULE_NAME);
            final JSONObject regexPatternsJson = kustoCheckObject.getJSONObject(REGEX_PATTERNS_KEY);

            for (String key : regexPatternsJson.keySet()) {
                String patternStr = regexPatternsJson.getString(key);
                regexPatterns.put(key, Pattern.compile(patternStr));
            }
            ANTI_PATTERN_MESSAGE = kustoCheckObject.getString(ANTI_PATTERN_KEY);
        }
    }
}