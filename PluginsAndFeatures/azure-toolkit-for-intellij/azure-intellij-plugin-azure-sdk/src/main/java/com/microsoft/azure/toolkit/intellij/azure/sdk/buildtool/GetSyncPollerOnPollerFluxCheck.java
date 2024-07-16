package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Inspection tool to detect the use of getSyncPoller() on a PollerFlux.
 * The inspection will check if the method call is on a PollerFlux and if the method call is on an Azure SDK client.
 * If both conditions are met, the inspection will register a problem with the suggestion to use SyncPoller instead.
 * <p>
 * This is an example of an anti-pattern that would be detected by the inspection tool.
 * public void exampleUsage() {
 * PollerFlux<String> pollerFlux = createPollerFlux();
 * <p>
 * // Anti-pattern: Using getSyncPoller() on PollerFlux
 * SyncPoller<String, Void> syncPoller = pollerFlux.getSyncPoller();
 * }
 */
public class GetSyncPollerOnPollerFluxCheck extends LocalInspectionTool {

    /**
     * Method to build the visitor for the inspection tool.
     *
     * @param holder Holder for the problems found by the inspection
     * @return JavaElementVisitor a visitor to visit the method call expressions
     */
    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new GetSyncPollerOnPollerFluxVisitor(holder, isOnTheFly);
    }

    /**
     * Visitor class to visit the method call expressions and check for the use of getSyncPoller() on a PollerFlux.
     * The visitor will check if the method call is on a PollerFlux and if the method call is on an Azure SDK client.
     */
    static class GetSyncPollerOnPollerFluxVisitor extends JavaElementVisitor {

        // Instance variables
        private final ProblemsHolder holder;

        // Define constants for string literals
        private static final RuleConfig ruleConfig;
        private static final String ANTI_PATTERN_MESSAGE;
        private static final String METHOD_TO_CHECK;
        private static boolean SKIP_WHOLE_RULE;

        static {
            final String ruleName = "GetSyncPollerOnPollerFluxCheck";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            ruleConfig = centralRuleConfigLoader.getRuleConfig(ruleName);

            METHOD_TO_CHECK = ruleConfig.getMethodsToCheck().get(0);
            ANTI_PATTERN_MESSAGE = ruleConfig.getAntiPatternMessage();
            SKIP_WHOLE_RULE = ruleConfig == RuleConfig.EMPTY_RULE;
        }

        /**
         * Constructor to initialize the visitor with the holder and isOnTheFly flag.
         *
         * @param holder     Holder for the problems found by the inspection
         * @param isOnTheFly Flag to indicate if the inspection is running on the fly -- not used in this inspection
         */
        public GetSyncPollerOnPollerFluxVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
        }

        /**
         * Method to visit the method call expressions and check for the use of getSyncPoller() on a PollerFlux.
         *
         * @param expression Method call expression to visit
         */
        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);

            // Check if the whole rule should be skipped
            if (SKIP_WHOLE_RULE) {
                return;
            }

            // Check if the element is a method call expression
            if (!(expression instanceof PsiMethodCallExpression)) {
                return;
            }

            PsiMethodCallExpression methodCall = expression;

            // Check if the method call is getSyncPoller
            if (methodCall.getMethodExpression().getReferenceName().startsWith(METHOD_TO_CHECK)) {
                boolean isAsyncContext = checkIfAsyncContext(methodCall);

                if (isAsyncContext && isAzureClient(methodCall)) {
                    holder.registerProblem(expression, ANTI_PATTERN_MESSAGE);
                }
            }
        }

        /**
         * Helper method to check if the method call is within an async context.
         * This method will check if the method call is on a PollerFlux type.
         *
         * @param methodCall Method call expression to check
         * @return true if the method call is on a reactive type, false otherwise
         */
        private boolean checkIfAsyncContext(@NotNull PsiMethodCallExpression methodCall) {

            String pollerFluxType = "PollerFlux";

            PsiExpression expression = methodCall.getMethodExpression().getQualifierExpression();

            if (expression == null) {
                return false;
            }
            PsiType type = expression.getType();

            // Check if the type is a reactive type
            if (type == null) {
                return false;
            }
            String typeName = type.getCanonicalText();

            // Check for PollerFlux type
            if (typeName != null && typeName.contains(pollerFluxType)) {
                return true;
            }
            return false;
        }

        /**
         * Helper method to check if the method call is on an Azure SDK client.
         * This method will check if the method call is on a class that is part of the Azure SDK.
         *
         * @param methodCall Method call expression to check
         * @return true if the method call is on an Azure SDK client, false otherwise
         */
        private boolean isAzureClient(@NotNull PsiMethodCallExpression methodCall) {

            PsiClass containingClass = PsiTreeUtil.getParentOfType(methodCall, PsiClass.class);

            // Check if the method call is on a class
            if (containingClass == null) {
                return false;
            }
            String className = containingClass.getQualifiedName();

            // Check if the class is part of the Azure SDK
            if (className != null && className.startsWith(ruleConfig.AZURE_PACKAGE_NAME)) {
                return true;
            }
            return false;
        }
    }
}
