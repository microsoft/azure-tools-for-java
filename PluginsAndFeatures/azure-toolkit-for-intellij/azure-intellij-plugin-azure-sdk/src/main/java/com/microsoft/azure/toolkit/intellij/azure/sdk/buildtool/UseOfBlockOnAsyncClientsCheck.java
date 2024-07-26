package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;

import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

/**
 * Inspection tool to check for the use of block() method on async clients in Azure SDK.
 */
public class UseOfBlockOnAsyncClientsCheck extends LocalInspectionTool {

    /**
     * This method is used to get the visitor for the inspection tool.
     * The visitor is used to check for the use of block() method on async clients in Azure SDK.
     * The visitor checks if the method call is a block() method call on a reactive type
     * and if the reactive type is an async client in Azure SDK.
     * If the method call is a block() method call on an async client, it reports a problem.
     */
    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new UseOfBlockOnAsyncClientsVisitor(holder);
    }

    /**
     * Visitor to check for the use of block() method on async clients in Azure SDK.
     * The visitor checks if the method call is a block() method call on a reactive type
     * and if the reactive type is an async client in Azure SDK.
     * If the method call is a block() method call on an async client, it reports a problem.
     */
    static class UseOfBlockOnAsyncClientsVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;

        // Define constants for string literals
        private static final RuleConfig RULE_CONFIG;
        private static final boolean SKIP_WHOLE_RULE;

        static {
            final String ruleName = "UseOfBlockOnAsyncClientsCheck";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            RULE_CONFIG = centralRuleConfigLoader.getRuleConfig(ruleName);
            SKIP_WHOLE_RULE = RULE_CONFIG.skipRuleCheck() || RULE_CONFIG.getListedItemsToCheck().isEmpty();
        }

        /**
         * Constructor to initialize the visitor with the holder
         *
         * @param holder ProblemsHolder - the holder to report the problems
         */
        UseOfBlockOnAsyncClientsVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        /**
         * This method is used to visit the method call expression.
         * The method call expression is checked to see if it is a block() method call on an async client.
         * If the method call is a block() method call on an async client, it reports a problem.
         *
         * @param expression PsiMethodCallExpression - the method call expression to visit
         */
        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);

            if (SKIP_WHOLE_RULE) {
                return;
            }

            // Check if the method call is block() or a variant of block() method
            if (expression.getMethodExpression().getReferenceName().startsWith(RULE_CONFIG.getMethodsToCheck().get(0))) {

                // Check if the method call is on a reactive type
                boolean isAsyncContext = checkIfAsyncContext(expression);

                if (isAsyncContext) {
                    holder.registerProblem(expression, RULE_CONFIG.getAntiPatternMessageMap().get("antiPatternMessage"));
                }
            }
        }

        /**
         * This method is used to check if the method call is a block() method call on a reactive type.
         * The method call is checked to see if it is a block() method call on a reactive type
         * and if the reactive type is an async client in Azure SDK.
         *
         * @param methodCall PsiMethodCallExpression - the method call expression to check
         * @return true if the method call is a block() method call on an async client, false otherwise
         */
        private boolean checkIfAsyncContext(@NotNull PsiMethodCallExpression methodCall) {

            // Get the qualifier expression of the method call -- the expression before the 'block' method call
            PsiExpression qualifierExpression = methodCall.getMethodExpression().getQualifierExpression();

            if (qualifierExpression instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression qualifierMethodCall = (PsiMethodCallExpression) qualifierExpression;

                // Get the return type of the qualifier method call
                PsiType qualifierReturnType = qualifierMethodCall.getType();

                if (qualifierReturnType instanceof PsiClassType) {
                    PsiClass qualifierReturnTypeClass = ((PsiClassType) qualifierReturnType).resolve();

                    // Check if the return type is a subclass of Mono or Flux
                    if (qualifierReturnTypeClass != null && isReactiveType(qualifierReturnTypeClass)) {

                        return isAzureAsyncClient(qualifierMethodCall);
                    }
                }
            }
            return false;
        }

        /**
         * This method is used to check if the method call is on an async client in Azure SDK.
         * The method call is checked to see if it is on an async client in Azure SDK.
         *
         * @param qualifierMethodCall PsiMethodCallExpression - the method call expression to check
         * @return true if the method call is on an async client in Azure SDK, false otherwise
         */
        private boolean isAzureAsyncClient(PsiMethodCallExpression qualifierMethodCall) {

            // Get the expression that calls the method returning a reactive type
            PsiExpression clientExpression = qualifierMethodCall.getMethodExpression().getQualifierExpression();

            // Travel up the method call chain to get the client expression
            while (clientExpression instanceof PsiMethodCallExpression) {

                clientExpression = ((PsiMethodCallExpression) clientExpression).getMethodExpression().getQualifierExpression();
            }

            // a ReferenceExpression is the last expression in the chain - the client object
            if (clientExpression instanceof PsiReferenceExpression) {
                PsiType clientType = clientExpression.getType();

                if (clientType instanceof PsiClassType) {
                    PsiClass clientClass = ((PsiClassType) clientType).resolve();

                    return clientClass != null && clientClass.getQualifiedName().startsWith(RuleConfig.AZURE_PACKAGE_NAME) && clientClass.getQualifiedName().endsWith("AsyncClient");
                }
            }
            return false;
        }

        /**
         * Helper method to check if the class / return type is a subclass of Mono or Flux.
         *
         * @param psiClass PsiClass - the class to check
         * @return true if the class is a subclass of Mono or Flux, false otherwise
         */
        private boolean isReactiveType(PsiClass psiClass) {
            return RULE_CONFIG.getListedItemsToCheck().contains(psiClass.getQualifiedName());
        }
    }
}
