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


public class GetSyncPollerOnPollerFluxCheck extends LocalInspectionTool {

    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new GetSyncPollerOnPollerFluxVisitor(holder, isOnTheFly);
    }


    public static class GetSyncPollerOnPollerFluxVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private final boolean isOnTheFly;


        public GetSyncPollerOnPollerFluxVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
            this.isOnTheFly = isOnTheFly;
        }

        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);

            // Check if the element is a method call expression
            if (expression instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression methodCall = expression;

                // Check if the method call is getSyncPoller
                if (methodCall.getMethodExpression().getReferenceName().startsWith("getSyncPoller")) {
                    System.out.println("methodCall: " + methodCall);
                    System.out.println("methodCall.getReferenceName(): " + methodCall.getMethodExpression().getReferenceName());
                    boolean isAsyncContext = checkIfAsyncContext(methodCall);

                    if(isAsyncContext && isAzureClient(methodCall)) {
                        holder.registerProblem(expression, "Use of getSyncPoller() on a PollerFlux detected. Directly use SyncPoller to handle synchronous polling tasks");
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
            PsiExpression expression = methodCall.getMethodExpression().getQualifierExpression();

            // Check if the method call is on a reactive type
            if (expression != null) {
                PsiType type = expression.getType();

                // Check if the type is a reactive type
                if (type != null) {
                    String typeName = type.getCanonicalText();
                    System.out.println("typeName: " + typeName);

                    // Check for PollerFlux type
                    if (typeName != null && typeName.contains("PollerFlux")) {
                        return true;
                    }
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
        private boolean isAzureClient (@NotNull PsiMethodCallExpression methodCall){

            PsiClass containingClass = PsiTreeUtil.getParentOfType(methodCall, PsiClass.class);

            // Check if the method call is on a class
            if (containingClass != null) {
                String className = containingClass.getQualifiedName();
                System.out.println("className: " + className);

                // Check if the class is part of the Azure SDK
                if (className != null && className.startsWith("com.azure.")) {
                    return true;
                }
            }
            return false;
        }

    }
}


