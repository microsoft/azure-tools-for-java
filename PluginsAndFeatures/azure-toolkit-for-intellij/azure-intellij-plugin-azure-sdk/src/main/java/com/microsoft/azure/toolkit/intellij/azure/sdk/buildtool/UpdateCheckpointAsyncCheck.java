package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.JavaRecursiveElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UpdateCheckpointAsyncCheck extends LocalInspectionTool {

    /**
     * Build the visitor for the inspection. This visitor will be used to traverse the PSI tree.
     *
     * @param holder The holder for the problems found
     * @return The visitor for the inspection. This is not used anywhere else in the code.
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new UpdateCheckpointAsyncCheck.UpdateCheckpointAsyncVisitor(holder, isOnTheFly);
    }


    /**
     * This class extends the JavaElementVisitor and is used to visit the Java elements in the code.
     * It checks for the usage of Azure SDK ServiceBusReceiver & ServiceBusProcessor clients and
     * whether the auto-complete feature is disabled.
     * If the auto-complete feature is not disabled, a problem is registered with the ProblemsHolder.
     */
    static class UpdateCheckpointAsyncVisitor extends JavaElementVisitor {

        // Define the holder for the problems found
        private final ProblemsHolder holder;

        /**
         * Constructor for the visitor
         *
         * @param holder     The holder for the problems found
         * @param isOnTheFly boolean to check if the inspection is on the fly -- This is not in use
         */
        public UpdateCheckpointAsyncVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
        }


        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);

            if ("updateCheckpointAsync".equals(expression.getMethodExpression().getReferenceName())) {
                String followingMethod = getFollowingMethodName(expression);
                System.out.println("Following method: " + followingMethod);

                if (followingMethod == null) {   // flag for not using block()
                    System.out.println("Antipattern detected: updateCheckpointAsync() used without block() or block(timeout) or subscribe(): " + expression.getText());
                    if (isCalledOnEventBatchContext(expression)) {
                        System.out.println("updateCheckpointAsync() is called on an EventBatchContext object from the Azure SDK: " + expression.getText());
                        // Add your custom logic here
                        holder.registerProblem(expression, "The updateCheckpointAsync() without block() will not do anything, use block() operator with a timeout or consider using the synchronous version updateCheckpoint()");
                    }
                } else if ("subscribe".equals(followingMethod)) {  // if WITH subscribe -- FLAG for using subscribe & for not using block()
                    System.out.println("Antipattern detected: updateCheckpointAsync() used with subscribe(): ");
                    if (isCalledOnEventBatchContext(expression)) {
                        System.out.println("updateCheckpointAsync() is called on an EventBatchContext object from the Azure SDK: " + expression.getText());
                        // Add your custom logic here
                        holder.registerProblem(expression, "Instead of subscribe(), use block() or block() with timeout or use the synchronous version updateCheckpoint().");
                    }
                }
            }
        }

        private String getFollowingMethodName(PsiMethodCallExpression expression) {

//            System.out.println("Expression: " + expression.getText());

            PsiElement parent = expression.getParent();
            System.out.println("Parent: " + parent);
//            System.out.println("Parent getText: " + parent.getText());

            if (parent instanceof PsiReferenceExpression) {
                PsiElement grandParent = parent.getParent();
                System.out.println("Grandparent: " + grandParent);
//                System.out.println("Grandparent getText: " + grandParent.getText());
                if (grandParent instanceof PsiMethodCallExpression) {
                    System.out.println("Grandparent is PsiMethodCallExpression");
                    System.out.println("Grandparent method: " + ((PsiMethodCallExpression) grandParent).getMethodExpression().getReferenceName());
                    PsiMethodCallExpression parentCall = (PsiMethodCallExpression) grandParent;
//                    System.out.println("Grandparent call: " + parentCall.getText());

                    String methodName = parentCall.getMethodExpression().getReferenceName();
                    System.out.println("Grandparent method name: " + methodName);
                    if ("block".equals(methodName) || "subscribe".equals(methodName)) {
                        return methodName;
                    }
                }
            }
            return null;
        }

        private boolean isCalledOnEventBatchContext(PsiMethodCallExpression expression) {
            System.out.println("Checking if updateCheckpointAsync() is called on an EventBatchContext object: " + expression.getText());

            PsiExpression qualifier = expression.getMethodExpression().getQualifierExpression();
            System.out.println("Qualifier: " + qualifier);

            if (qualifier instanceof PsiReferenceExpression) {
                PsiElement resolvedElement = ((PsiReferenceExpression) qualifier).resolve();
                System.out.println("Resolved element: " + resolvedElement);

                if (resolvedElement instanceof PsiParameter) {
                    PsiParameter parameter = (PsiParameter) resolvedElement;
                    System.out.println("Parameter: " + parameter);

                    PsiType parameterType = parameter.getType();
                    System.out.println("Parameter type: " + parameterType);

                    if ("EventBatchContext".equals(parameterType.getPresentableText())) {
                        if (parameterType instanceof PsiClassType) {
                            System.out.println("Parameter type is PsiClassType");
                            PsiClassType classType = (PsiClassType) parameterType;
                            System.out.println("Class type: " + classType);
                            PsiClass psiClass = classType.resolve();
                            System.out.println("PsiClass: " + psiClass);

                            if (psiClass != null) {
                                String qualifiedName = psiClass.getQualifiedName();
                                System.out.println("Qualified name: " + qualifiedName);

                                return qualifiedName != null && qualifiedName.startsWith("com.azure");
                            }
                        }
                    }
                }
            } return false;
        }
    }
}
