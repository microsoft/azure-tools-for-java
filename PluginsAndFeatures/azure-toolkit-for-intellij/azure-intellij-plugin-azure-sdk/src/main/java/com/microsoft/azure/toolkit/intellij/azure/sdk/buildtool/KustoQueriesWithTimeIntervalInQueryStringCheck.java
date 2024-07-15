package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl;

import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
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
    static class KustoQueriesVisitor extends JavaElementVisitor {

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
            PsiExpression initializer = polyadicExpression instanceof PsiPolyadicExpression ? polyadicExpression : null;
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

                // case methodcall to PsiMethodCallExpression
                PsiMethodCallExpression methodCallExpression = methodCall;

                System.out.println("methodCallExpression: " + methodCallExpression);
//                System.out.println("methodCall: " + methodCall.getMethodExpression().getText());
                if (isAzureClient(methodCallExpression)) {
                    System.out.println("Registering problem for method call: " + methodCall);
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

                System.out.println("Found anti-pattern in expression: " + text);
                System.out.println("Element: " + element);
                System.out.println("ElementText: " + element.getText());

                PsiElement parentElement = element.getParent();

                System.out.println("Parent element: " + parentElement);

                if (parentElement instanceof PsiLocalVariable) {
                    PsiLocalVariable variable = (PsiLocalVariable) parentElement;
                    String variableName = variable.getName();
                    timeIntervalParameters.add(variableName);
                    System.out.println("Time interval parameters2: " + timeIntervalParameters);
                }
            }
        }


        /**
         * This method checks if the method call is an Azure client method call
         * by checking the type of the variable that the method is called on
         *
         * @param methodCall
         * @return boolean
         */
        private boolean isAzureClient(PsiMethodCallExpression methodCall) {

            PsiReferenceExpression methodExpression = methodCall.getMethodExpression();
            System.out.println("Method expression: " + methodExpression);

            PsiExpression qualifierExpression = methodExpression.getQualifierExpression();
            System.out.println("Qualifier expression: " + qualifierExpression);

            PsiClass containingClass = PsiTreeUtil.getParentOfType(methodCall, PsiClass.class);
            System.out.println("Containing class: " + containingClass);

            if (containingClass != null) {
                String className = containingClass.getQualifiedName();
                System.out.println("Class name: " + className);
                // Check if the class name belongs to the com.azure namespace or any specific Azure SDK namespace
                return className != null && className.startsWith("com.azure");
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