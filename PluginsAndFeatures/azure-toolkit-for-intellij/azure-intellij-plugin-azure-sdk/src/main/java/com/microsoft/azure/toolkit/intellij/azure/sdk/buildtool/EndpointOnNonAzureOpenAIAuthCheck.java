package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiVariable;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class EndpointOnNonAzureOpenAIAuthCheck extends LocalInspectionTool {

    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new EndpointOnNonAzureOpenAIAuthVisitor(holder, isOnTheFly);
    }


    static class EndpointOnNonAzureOpenAIAuthVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;

        EndpointOnNonAzureOpenAIAuthVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
        }


        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);

            PsiReferenceExpression methodExpression = expression.getMethodExpression();
            String methodName = methodExpression.getReferenceName();

            System.out.println("methodName: " + methodName);

            if ("endpoint".equals(methodName)) {
                if (isUsingKeyCredential(expression)) {
                    holder.registerProblem(expression, "Endpoint should not be used with KeyCredential for non-Azure OpenAI clients");
                }
            }


        }

        private static boolean isUsingKeyCredential(PsiMethodCallExpression expression) {

            // Iterating up the chain of method calls
            PsiExpression qualifier = expression.getMethodExpression().getQualifierExpression();

            System.out.println("qualifier: " + qualifier);

            // Check if the method call chain has the method to check -- need to traverse up and down
            while (qualifier instanceof PsiMethodCallExpression) {

                // Get the method expression
                PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) qualifier).getMethodExpression();
                System.out.println("methodExpression: " + methodExpression);

                PsiMethodCallExpression methodCall = (PsiMethodCallExpression) qualifier;
                System.out.println("methodCall: " + methodCall);

                // Get the method name
                String methodName = methodExpression.getReferenceName();
                System.out.println("methodName: " + methodName);

                if ("credential".equals(methodName)) {

                    System.out.println("credential method found");
                    PsiExpressionList argumentList = methodCall.getArgumentList();
                    System.out.println("argumentList: " + argumentList);
                    System.out.println("methodCall.getArgumentList(): " + methodCall.getArgumentList());
                    System.out.println("methodCall: " + methodCall);
                    PsiExpression[] arguments = methodCall.getArgumentList().getExpressions();
                    System.out.println("arguments: " + arguments);
                    if (arguments.length == 1 && isKeyCredential(arguments[0])) {
                        return isNonAzureOpenAIClient(methodCall);
                    }
                    return false;
                }

                // Iterating up the chain of method calls
                qualifier = methodCall.getMethodExpression().getQualifierExpression();
            }
            return false;
        }

        private static boolean isKeyCredential(PsiExpression expression) {

            System.out.println("expression.getType().getCanonicalText()): " + expression.getType().getCanonicalText());
            System.out.println("expression.getType()); " + expression.getType());

            return expression.getType()!= null && expression.getType().getCanonicalText().equals("com.azure.core.credential.KeyCredential");
        }

        private static boolean isNonAzureOpenAIClient(PsiMethodCallExpression expression) {
            PsiElement parent = expression.getParent();
            System.out.println("parent: " + parent);

            while (parent != null) {
                System.out.println("parent: " + parent);
                if (parent instanceof PsiVariable) {

                    PsiType type = ((PsiVariable) parent).getType();
                    System.out.println("type: " + type);
                    System.out.println("type.getCanonicalText(): " + type.getCanonicalText());
                    if(type != null){
                        return type.getCanonicalText().startsWith("com.azure.ai.openai");
                    }
                }
                parent = parent.getParent();
            }
            return false;
        }
    }
}
