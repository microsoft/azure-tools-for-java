package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;

import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;


/**
 * Inspection to check for the use of block() method on async clients in Azure SDK.
 * This inspection will check for the use of block() method on reactive types like Mono, Flux, etc.
 */
public class UseOfBlockOnAsyncClientsCheck extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new UseOfBlockOnAsyncClientsVisitor(holder, isOnTheFly);
    }

    /**
     * Visitor to check for the use of block() method on async clients in Azure SDK.
     */
    public static class UseOfBlockOnAsyncClientsVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private final boolean isOnTheFly;

        public UseOfBlockOnAsyncClientsVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
            this.isOnTheFly = isOnTheFly;
        }

        /**
         * Visit method call expressions to check for the use of block() method on async clients.
         * This method will check if the method call is block() and if it is used in an async context.
         * @param expression
         */
        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);

            // Check if the element is a method call expression
            if (expression instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression methodCall = expression;

                System.out.println("methodCall.getMethodExpression().getReferenceName()): " + methodCall.getMethodExpression().getReferenceName());

                // Check if the method call is block()
                if (methodCall.getMethodExpression().getReferenceName().startsWith("block")) {
                    boolean isAsyncContext = checkIfAsyncContext(methodCall);
                    if (isAsyncContext && isAzureClient(methodCall)) {
                        holder.registerProblem(expression, "Using block() method in an async client can lead to performance issues.");
                    } else {
                        holder.registerProblem(expression, "Consider using a synchronous client instead of block().");
                    }
                }
            }
        }

        /**
         * Helper method to check if the method call is within an async context.
         * This method will check if the method call is on a reactive type like Mono, Flux, etc.
         * @param methodCall
         * @return true if the method call is on a reactive type, false otherwise
         */
        private boolean checkIfAsyncContext(@NotNull PsiMethodCallExpression methodCall) {
            PsiExpression expression = methodCall.getMethodExpression().getQualifierExpression();
            System.out.println("Expression: " + expression);

            if (expression != null) {
                PsiType type = expression.getType();
                System.out.println("Type: " + type);

                if (type != null) {
                    String typeName = type.getCanonicalText();
                    System.out.println("TypeName: " + typeName);

                    // Check for common async/reactive types directly
                    return typeName.startsWith("reactor.core.publisher.Mono") ||
                        typeName.startsWith("reactor.core.publisher.Flux") ||
                        typeName.startsWith("com.azure.core.util.paging.PagedFlux") ||
                        typeName.startsWith("com.azure.core.util.polling.PollerFlux");
                }
            }
            return false;
        }

        /**
         * Helper method to check if the method call is on an Azure SDK client.
         * This method will check if the method call is on a class that is part of the Azure SDK.
         * @param methodCall
         * @return true if the method call is on an Azure SDK client, false otherwise
         */
        private boolean isAzureClient(@NotNull PsiMethodCallExpression methodCall) {
            PsiClass containingClass = PsiTreeUtil.getParentOfType(methodCall, PsiClass.class);
            System.out.println("ContainingClass: " + containingClass);

            if (containingClass != null) {
                String className = containingClass.getQualifiedName();
                System.out.println("ClassName: " + className);

                // Check if the class is part of the Azure SDK
                if (className != null && className.startsWith("com.azure.")) {
                    return true;
                }
            }
            return false;
        }
    }
}