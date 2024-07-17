package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;


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
     * It checks for the usage of the updateCheckpointAsync() method call in the code.
     * The method call should not be followed by a subscribe method call
     * and instead should be followed by a block() method call.
     */
    static class UpdateCheckpointAsyncVisitor extends JavaElementVisitor {

        // Define the holder for the problems found
        private final ProblemsHolder holder;

        // Define constants for string literals
        private static final RuleConfig RULE_CONFIG;
        private static final boolean SKIP_WHOLE_RULE;

        static {
            final String ruleName = "UpdateCheckpointAsyncCheck";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            RULE_CONFIG = centralRuleConfigLoader.getRuleConfig(ruleName);

            // Get the methods to check from the RuleConfig object
            SKIP_WHOLE_RULE = RULE_CONFIG.skipRuleCheck() || RULE_CONFIG.getMethodsToCheck().isEmpty();
        }

        /**
         * Constructor for the visitor
         *
         * @param holder     The holder for the problems found
         * @param isOnTheFly boolean to check if the inspection is on the fly -- This is not in use
         */
        UpdateCheckpointAsyncVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
        }

        /**
         * This method is used to visit the method call expressions in the code.
         * It checks for the usage of the updateCheckpointAsync() method call in the code.
         * The method call should not be followed by a subscribe method call
         * and instead should be followed by a block() method call.
         *
         * @param expression The method call expression
         */
        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);

            // Check if the rule should be skipped
            if (SKIP_WHOLE_RULE) {
                return;
            }

            // Check if the method call is updateCheckpointAsync()
            if ("updateCheckpointAsync".equals(expression.getMethodExpression().getReferenceName())) {

                // Get the method name following the updateCheckpointAsync() method call
                String followingMethod = getFollowingMethodName(expression);

                // Check if the updateCheckpointAsync() method call is called on an EventBatchContext object
                if (followingMethod == null && isCalledOnEventBatchContext(expression)) {
                    holder.registerProblem(expression, RULE_CONFIG.getAntiPatternMessageMap().get("no_block"));

                    // Check if the following method is `subscribe` and
                    //  Check if the updateCheckpointAsync() method call is called on an EventBatchContext object
                } else if ("subscribe".equals(followingMethod) && isCalledOnEventBatchContext(expression)) {
                    holder.registerProblem(expression, RULE_CONFIG.getAntiPatternMessageMap().get("with_subscribe"));
                }
            }
        }

        /**
         * This method checks if the method call is following a updateCheckpointAsync() method call.
         * If the method call is following a updateCheckpointAsync(), the method name is returned.
         *
         * @param expression The method call expression
         * @return The method name if the method call is following a specific method call, null otherwise
         */
        private String getFollowingMethodName(PsiMethodCallExpression expression) {

            // Get the parent of the method call expression
            PsiElement parent = expression.getParent();

            if (!(parent instanceof PsiReferenceExpression)) {
                return null;
            }

            // Get the grandparent of the method call expression
            PsiElement grandParent = parent.getParent();

            // Check if the grandparent is a method call expression
            if (grandParent instanceof PsiMethodCallExpression) {

                // Cast the grandparent to a method call expression
                PsiMethodCallExpression parentCall = (PsiMethodCallExpression) grandParent;

                // Get the method name from the parent call
                String methodName = parentCall.getMethodExpression().getReferenceName();

                // Check if the method name is in the list of methods to check
                if (RULE_CONFIG.getMethodsToCheck().contains(methodName)) {
                    return methodName;
                }
            }
            return null;
        }

        /**
         * This method checks if the method call is called on an EventBatchContext object from the Azure SDK.
         *
         * @param expression The method call expression
         * @return True if the method call is called on an EventBatchContext object from the Azure SDK, false otherwise
         */
        private boolean isCalledOnEventBatchContext(PsiMethodCallExpression expression) {

            // Get the qualifier expression from the method call expression
            PsiExpression qualifier = expression.getMethodExpression().getQualifierExpression();

            // Check if the qualifier is a reference expression
            if (!(qualifier instanceof PsiReferenceExpression)) {
                return false;
            }

            // Resolve the qualifier element
            PsiElement resolvedElement = ((PsiReferenceExpression) qualifier).resolve();

            // Check if the resolved element is a parameter
            if (!(resolvedElement instanceof PsiParameter)) {
                return false;
            }

            // Check if the resolved element is a class type
            PsiParameter parameter = (PsiParameter) resolvedElement;

            // Get the type of the parameter
            PsiType parameterType = parameter.getType();

            // Check if the parameter type is a EventBatchContext object from the Azure SDK
            if (!("EventBatchContext".equals(parameterType.getPresentableText()))) {
                return false;
            }

            if (!(parameterType instanceof PsiClassType)) {
                return false;
            }

            // Resolve the parameter type to a class type and get the class
            PsiClassType classType = (PsiClassType) parameterType;
            PsiClass psiClass = classType.resolve();

            if (psiClass != null) {
                // Get the qualified name of the class
                String qualifiedName = psiClass.getQualifiedName();

                // Check if the qualified name starts with the Azure package name
                return qualifiedName != null && qualifiedName.startsWith(RuleConfig.AZURE_PACKAGE_NAME);
            }
            return false;
        }
    }
}
