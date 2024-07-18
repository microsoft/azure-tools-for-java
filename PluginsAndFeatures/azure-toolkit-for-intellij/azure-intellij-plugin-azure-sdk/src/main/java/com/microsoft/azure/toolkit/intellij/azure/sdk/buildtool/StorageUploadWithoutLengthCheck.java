package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

/**
 * This class extends the LocalInspectionTool and is used to inspect the usage of Azure Storage upload APIs in the code.
 * It checks if the upload methods are being called without a 'length' parameter of type 'long'.
 * The methods to check are defined in a JSON configuration file.
 * If such a method call is detected, a problem is registered with the ProblemsHolder.
 */

public class StorageUploadWithoutLengthCheck extends LocalInspectionTool {
    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Ensure Storage APIs use Length Parameter";
    }

    private static final RuleConfig RULE_CONFIG;
    private static final String LENGTH_TYPE = "long";
    private static final boolean SKIP_WHOLE_RULE;


    static {
        final String ruleName = "StorageUploadWithoutLengthCheck";
        RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

        // Get the RuleConfig object for the rule
        RULE_CONFIG = centralRuleConfigLoader.getRuleConfig(ruleName);
        SKIP_WHOLE_RULE = RULE_CONFIG == RuleConfig.EMPTY_RULE || RULE_CONFIG.getMethodsToCheck().isEmpty();
    }

    /**
     * This method is used to build a PsiElementVisitor that will be used to visit the method calls in the code.
     *
     * @param holder     ProblemsHolder object to register the problem
     * @param isOnTheFly boolean to check if the inspection is on the fly -- Not in use
     * @return PsiElementVisitor object to visit the elements in the code
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaRecursiveElementWalkingVisitor() {

            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);
                String methodName = expression.getMethodExpression().getReferenceName();

                if (!RULE_CONFIG.getMethodsToCheck().contains(methodName)) {
                    return;
                }

                if (SKIP_WHOLE_RULE) {
                    return;
                }

                boolean hasLengthArg = false;

                PsiExpressionList argumentList = expression.getArgumentList();
                PsiExpression[] arguments = argumentList.getExpressions();

                for (PsiExpression arg : arguments) {
                    // Check if the argument is of type 'long'
                    if (arg.getType() != null && arg.getType().toString().equals(LENGTH_TYPE)) {
                        hasLengthArg = true;
                        break;
                    }
                    // Check if the argument is a method call
                    if (arg instanceof PsiMethodCallExpression) {
                        PsiMethodCallExpression argMethodCall = (PsiMethodCallExpression) arg;

                        // Analysing arguments that are method calls to check for 'long' type arguments
                        hasLengthArg = checkMethodCallChain(argMethodCall);
                    }
                }
                if (!hasLengthArg) {
                    holder.registerProblem(expression, RULE_CONFIG.getAntiPatternMessageMap().get("anti_pattern_message"));
                }
            }
        };
    }

    /**
     * Analysing arguments that are chained method calls
     * This method is used to check if the method call chain has a constructor with 'long' type arguments.
     * The iteration starts from the end of the chain and goes up the chain.
     * The qualifier of the method call is checked for a constructor with 'long' type arguments.
     *
     * @param expression - The method call expression
     * @return boolean - true if the constructor has 'long' type arguments
     */
    public boolean checkMethodCallChain(PsiMethodCallExpression expression) {

        // Iterating up the chain of method calls
        PsiExpression qualifier = expression.getMethodExpression().getQualifierExpression();

        while (qualifier instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression qualifierMethodCall = (PsiMethodCallExpression) qualifier;

            qualifier = qualifierMethodCall.getMethodExpression().getQualifierExpression();

            // Checking for constructor with 'long' type arguments
            if (qualifier instanceof PsiNewExpression) {
                return isLengthArgumentInCall(qualifier);
            }
        }
        return false;
    }

    /**
     * This method is used to check if the constructor of the new expression has a 'long' type argument.
     *
     * @param qualifier - The qualifier of the method call
     * @return boolean
     */
    private boolean isLengthArgumentInCall(PsiExpression qualifier) {
        PsiNewExpression newExpression = (PsiNewExpression) qualifier;

        // Getting the arguments of the constructor
        PsiExpressionList newExpressionArgumentList = newExpression.getArgumentList();
        PsiExpression[] newExpressionArguments = newExpressionArgumentList.getExpressions();

        // Checking if the arguments are of type 'long'
        for (PsiExpression newExpressionArgument : newExpressionArguments) {
            if (newExpressionArgument.getType().toString().equals(LENGTH_TYPE)) {
                return true;
            }
        }
        return false;
    }
}
