package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base class for checking the usage of the updateCheckpointAsync() method call in the code.
 */
public abstract class AbstractUpdateCheckpointAsyncChecker extends LocalInspectionTool {

    /**
     * Abstract method to build the specific visitor for the inspection.
     *
     * @param holder     The holder for the problems found
     * @param isOnTheFly boolean to check if the inspection is on the fly
     * @return The visitor for the inspection
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return createVisitor(holder, isOnTheFly);
    }

    /**
     * Creates the specific visitor for the inspection.
     *
     * @param holder     The holder for the problems found
     * @param isOnTheFly boolean to check if the inspection is on the fly
     * @return The specific visitor for the inspection
     */
    protected abstract PsiElementVisitor createVisitor(ProblemsHolder holder, boolean isOnTheFly);

    /**
     * This abstract class is a base visitor used to visit Java elements in the code.
     */
    static abstract class UpdateCheckpointAsyncVisitorBase extends JavaElementVisitor {

        // Define the holder for the problems found
        protected final ProblemsHolder holder;

        // Define constants for string literals
        protected static final RuleConfig RULE_CONFIG;
        protected static final String BLOCK_METHOD = "block";
        protected static final String BLOCK_WITH_TIMEOUT_METHOD = "block_with_timeout";

        static {
            final String ruleName = "AbstractUpdateCheckpointAsyncChecker";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            RULE_CONFIG = centralRuleConfigLoader.getRuleConfig(ruleName);
        }

        /**
         * Constructs a new instance of the UpdateCheckpointAsyncVisitorBase.
         *
         * @param holder     The holder for the problems found
         * @param isOnTheFly boolean to check if the inspection is on the fly
         */
        UpdateCheckpointAsyncVisitorBase(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
        }

        /**
         * This method checks if the method call is following an updateCheckpointAsync() method call.
         * If the method call is following an updateCheckpointAsync(), the method name is returned.
         *
         * @param expression The method call expression
         * @return The method name if the method call is following a specific method call, null otherwise
         */
        protected String getFollowingMethodName(PsiMethodCallExpression expression) {
            // Get the parent of the method call expression
            PsiElement parent = expression.getParent();

            if (!(parent instanceof PsiReferenceExpression)) {
                return null;
            }
            PsiElement grandParent = parent.getParent();

            if (grandParent instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression parentCall = (PsiMethodCallExpression) grandParent;

                // Get the method name from the parent call
                String methodName = parentCall.getMethodExpression().getReferenceName();

                // Check if the method name is in the list of methods to check
                if (RULE_CONFIG.getMethodsToCheck().contains(methodName)) {
                    if (BLOCK_METHOD.equals(methodName)) {
                        PsiExpressionList arguments = parentCall.getArgumentList();
                        if (arguments.getExpressions().length == 1 && arguments.getExpressions()[0].getType() != null && arguments.getExpressions()[0].getType().equalsToText("java.time.Duration")) {
                            return BLOCK_WITH_TIMEOUT_METHOD;
                        }
                    }
                }
                return methodName;
            }
            return null;
        }

        /**
         * This method checks if the method call is called on an EventBatchContext object from the Azure SDK.
         *
         * @param expression The method call expression
         * @return True if the method call is called on an EventBatchContext object from the Azure SDK, false otherwise
         */
        protected boolean isCalledOnEventBatchContext(PsiMethodCallExpression expression) {
            // Get the qualifier expression from the method call expression
            PsiExpression qualifier = expression.getMethodExpression().getQualifierExpression();

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
            PsiType parameterType = parameter.getType();

            // Check if the parameter type is an EventBatchContext object from the Azure SDK
            if (!"EventBatchContext".equals(parameterType.getPresentableText())) {
                return false;
            }

            if (!(parameterType instanceof PsiClassType)) {
                return false;
            }

            PsiClassType classType = (PsiClassType) parameterType;
            PsiClass psiClass = classType.resolve();

            if (psiClass != null) {
                String qualifiedName = psiClass.getQualifiedName();

                // Check if the qualified name starts with the Azure package name
                return qualifiedName != null && qualifiedName.startsWith(RuleConfig.AZURE_PACKAGE_NAME);
            }
            return false;
        }
    }
}
