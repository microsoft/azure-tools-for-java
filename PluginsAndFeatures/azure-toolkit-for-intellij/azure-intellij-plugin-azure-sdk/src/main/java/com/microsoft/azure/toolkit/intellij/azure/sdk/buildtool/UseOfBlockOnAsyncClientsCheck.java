package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;

import com.intellij.psi.PsiType;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * Inspection to check for the use of block() method on async clients in Azure SDK.
 * This inspection will check for the use of block() method on reactive types like Mono, Flux, etc.
 */
public class UseOfBlockOnAsyncClientsCheck extends LocalInspectionTool {

    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new UseOfBlockOnAsyncClientsVisitor(holder, isOnTheFly);
    }

    /**
     * Visitor to check for the use of block() method on async clients in Azure SDK.
     */
    public static class UseOfBlockOnAsyncClientsVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private final boolean isOnTheFly;


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
         * Constructor to initialize the visitor with the holder and isOnTheFly flag.
         *
         * @param holder     ProblemsHolder
         * @param isOnTheFly boolean
         */
        UseOfBlockOnAsyncClientsVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
            this.isOnTheFly = isOnTheFly;
        }

        /**
         * Visit method call expressions to check for the use of block() method on async clients.
         * This method will check if the method call is block() and if it is used in an async context.
         *
         * @param expression
         */
        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);

            // Check if the element is a method call expression
            if (expression instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression methodCall = expression;

                // Check if the method call is block() or a variant of block() method
                if (methodCall.getMethodExpression().getReferenceName().startsWith("block")) {
                    boolean isAsyncContext = checkIfAsyncContext(methodCall);
                    System.out.println("isAsyncContext: " + isAsyncContext);
                    if (isAsyncContext) {
                        // Report the problem
                        holder.registerProblem(expression, RULE_CONFIG.getAntiPatternMessageMap().get("antiPatternMessage"));
                    }
                }
            }
        }

        /**
         * Helper method to check if the method call is within an async context.
         * This method will check if the method call is on a reactive type like Mono, Flux, etc.
         *
         * @param methodCall
         * @return true if the method call is on a reactive type, false otherwise
         */
        private boolean checkIfAsyncContext(@NotNull PsiMethodCallExpression methodCall) {
            PsiExpression qualifierExpression = methodCall.getMethodExpression().getQualifierExpression();

            System.out.println("qualifierExpression: " + qualifierExpression);

            if (qualifierExpression instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression qualifierMethodCall = (PsiMethodCallExpression) qualifierExpression;
                System.out.println("qualifierMethodCall: " + qualifierMethodCall);

                // Get the type of the qualifier method call
                PsiType qualifierReturnType = qualifierMethodCall.getType();
                System.out.println("qualifierReturnType: " + qualifierReturnType);

                if (qualifierReturnType instanceof PsiClassType) {
                    PsiClass qualifierReturnTypeClass = ((PsiClassType) qualifierReturnType).resolve();
                    System.out.println("qualifierReturnTypeClass: " + qualifierReturnTypeClass);

                    if (qualifierReturnTypeClass != null && isSubclassOfMonoOrFlux(qualifierReturnTypeClass)) {

                        if (isAzureAsyncClient(qualifierMethodCall)) {
                            System.out.println("isAzureAsyncClient: " + qualifierMethodCall);

                            // Report the problem
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        private boolean isAzureAsyncClient(PsiMethodCallExpression qualifierMethodCall) {

            // Get the expression that calls the method returning a reactive type
            PsiExpression clientExpression = qualifierMethodCall.getMethodExpression().getQualifierExpression();
            System.out.println("clientExpression: " + clientExpression);

            if (clientExpression != null) {
                PsiType clientType = clientExpression.getType();
                System.out.println("clientType: " + clientType);

                if (clientType instanceof PsiClassType) {
                    PsiClass clientClass = ((PsiClassType) clientType).resolve();
                    System.out.println("clientClass: " + clientClass);

                    if (clientClass != null && clientClass.getQualifiedName().startsWith("com.azure.") && clientClass.getQualifiedName().endsWith("AsyncClient")) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Helper method to check if the class is a subclass of Mono or Flux.
         *
         * @param psiClass
         * @return true if the class is a subclass of Mono or Flux, false otherwise
         */
        private boolean isSubclassOfMonoOrFlux(PsiClass psiClass) {
            System.out.println("psiClass: " + psiClass);
            return InheritanceUtil.isInheritor(psiClass, "reactor.core.publisher.Mono") ||
                    InheritanceUtil.isInheritor(psiClass, "reactor.core.publisher.Flux");
        }
    }
}