package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl;

import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

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
 * <p>
 * When the anti-patterns are detected as parameters of Azure client method calls, a problem is registered.
 */
public class KustoQueriesWithTimeIntervalInQueryStringCheck extends LocalInspectionTool {

    /**
     * This method builds the visitor for the inspection tool
     *
     * @param holder     ProblemsHolder is used to register problems found in the code
     * @param isOnTheFly boolean to indicate if the inspection is done on the fly -
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

        // // Define constants for string literals
        private static final RuleConfig ruleConfig;
        private static final String ANTI_PATTERN_MESSAGE;
        private static final Map<String, Pattern> REGEX_PATTERNS = new HashMap<>();
        private static boolean SKIP_WHOLE_RULE;

        static {
            final String ruleName = "KustoQueriesWithTimeIntervalInQueryStringCheck";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            ruleConfig = centralRuleConfigLoader.getRuleConfig(ruleName);

            Map<String, String> regexPatterns = ruleConfig.getMappedItemsToCheck();
            ANTI_PATTERN_MESSAGE = ruleConfig.getAntiPatternMessage();

            for (String key : regexPatterns.keySet()) {
                String patternStr = regexPatterns.get(key);
                REGEX_PATTERNS.put(key, Pattern.compile(patternStr));
            }

            SKIP_WHOLE_RULE = ruleConfig == RuleConfig.EMPTY_RULE || REGEX_PATTERNS.isEmpty();
        }

        // empty list to store time interval parameter names
        private List<String> timeIntervalParameters = new ArrayList<>();

        /**
         * Constructor for the KustoQueriesVisitor class
         * The constructor initializes the ProblemsHolder and isOnTheFly variables
         *
         * @param holder     - ProblemsHolder is used to register problems found in the code
         * @param isOnTheFly - boolean to indicate if the inspection is done on the fly - This is not used in this implementation
         */
        public KustoQueriesVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
        }

        /**
         * This method visits the element and checks for the anti-patterns
         * The method checks if the element is a PsiPolyadicExpression or a PsiLocalVariable
         * If the element is a PsiLocalVariable, the method checks the initializer of the variable
         * If the element is a PsiPolyadicExpression, the method processes the expression to replace the variables with their values
         * The method then checks the expression for the anti-patterns by matching regex patterns with the expression text
         * and registers a problem if an anti-pattern is detected
         *
         * @param element - the element to visit
         */
        @Override
        public void visitElement(@NotNull PsiElement element) {
            super.visitElement(element);

            // Skip the whole rule if the rule configuration is empty
            if (SKIP_WHOLE_RULE) {
                return;
            }

            if (!(element instanceof PsiPolyadicExpression || element instanceof PsiLocalVariable || element instanceof PsiMethodCallExpressionImpl)) {
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
         *
         * @param variable - the local variable to check
         */
        private void handleLocalVariable(PsiLocalVariable variable) {
            PsiExpression initializer = variable.getInitializer();
            if (initializer != null && initializer instanceof PsiLiteralExpression) {
                checkExpression(initializer, variable);
            }
        }

        /**
         * This method handles the polyadic expression by processing the expression to replace the variables with their values
         * The method then checks the expression for the anti-patterns by matching regex patterns with the expression text
         *
         * @param polyadicExpression - the polyadic expression to check
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
         *
         * @param methodCall - the method call to check
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
                    holder.registerProblem(methodCall, ANTI_PATTERN_MESSAGE);
                }
            }
        }

        /**
         * This method checks the expression for the anti-patterns by matching regex patterns with the expression text
         * and registers a problem if an anti-pattern is detected
         *
         * @param expression - the expression to check
         * @param element    - the element to check
         */
        void checkExpression(PsiExpression expression, PsiElement element) {
            if (expression == null) {
                return;
            }
            String text = expression.getText();

            // Check if the expression text contains any of the regex patterns
            boolean foundAntiPattern = REGEX_PATTERNS.values().stream().anyMatch(pattern -> pattern.matcher(text).find());


            // If an anti-pattern is detected, register a problem
            if (foundAntiPattern) {
                PsiElement parentElement = element.getParent();

                if (parentElement instanceof PsiLocalVariable) {
                    PsiLocalVariable variable = (PsiLocalVariable) parentElement;
                    String variableName = variable.getName();
                    timeIntervalParameters.add(variableName);
                }
            }
        }


        /**
         * This method checks if the method call is an Azure client method call
         * by checking the containing class of the method call
         *
         * @param methodCall - the method call to check
         * @return boolean - true if the method call is an Azure client method call, false otherwise
         */
        private boolean isAzureClient(PsiMethodCallExpression methodCall) {

            // Get the containing class of the method call
            PsiClass containingClass = PsiTreeUtil.getParentOfType(methodCall, PsiClass.class);

            if (containingClass != null) {
                String className = containingClass.getQualifiedName();
                // Check if the class name belongs to the com.azure namespace or any specific Azure SDK namespace
                return className != null && className.startsWith(RuleConfig.AZURE_PACKAGE_NAME);
            }
            return false;
        }
    }
}
