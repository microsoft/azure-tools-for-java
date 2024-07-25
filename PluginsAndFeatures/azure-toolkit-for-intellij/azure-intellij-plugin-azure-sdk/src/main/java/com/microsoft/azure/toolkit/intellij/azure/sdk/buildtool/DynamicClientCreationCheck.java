package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiBlockStatement;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiForStatement;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiStatement;
import org.jetbrains.annotations.NotNull;

/**
 * This class is used to check for the dynamic creation of clients in the code.
 * It extends the LocalInspectionTool class, which is used to create custom code inspections.
 * The visitor inspects a for loop and checks for methodcall expressions that create clients using the buildClient or buildAsyncClient methods.
 * If a client creation method is found building a client from the com.azure package, a problem is registered.
 */
public class DynamicClientCreationCheck extends LocalInspectionTool {


    /**
     * This method builds the visitor that checks for the dynamic creation of clients in the code.
     *
     * @param holder The holder for the problems found
     * @return PsiElementVisitor
     */
    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new DynamicClientCreationVisitor(holder, isOnTheFly);
    }


    /**
     * This class extends the JavaElementVisitor to check for the dynamic creation of clients in the code.
     */
    static class DynamicClientCreationVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;

        // Define constants for string literals
        private static final RuleConfig RULE_CONFIG;
        private static boolean SKIP_WHOLE_RULE;

        static {
            final String ruleName = "DynamicClientCreationCheck";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            RULE_CONFIG = centralRuleConfigLoader.getRuleConfig(ruleName);
            SKIP_WHOLE_RULE = RULE_CONFIG == RuleConfig.EMPTY_RULE || RULE_CONFIG.getMethodsToCheck().isEmpty();
        }

        /**
         * This constructor initializes the ProblemsHolder object.
         * It is used to register problems found in the code.
         * <p>
         * isOnTheFly is a boolean that indicates if the inspection is run on the fly.
         * It is not in use in this implementation, but present by default in the method signature.
         *
         * @param holder
         */
        public DynamicClientCreationVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
        }

        /**
         * This method checks for the dynamic creation of clients in the code.
         *
         * @param statement The for loop statement to visit
         */
        @Override
        public void visitForStatement(@NotNull PsiForStatement statement) {
            super.visitForStatement(statement);

            if (SKIP_WHOLE_RULE) {
                return;
            }

            // Extract the body of the for loop
            PsiStatement body = statement.getBody();

            if (!(body instanceof PsiBlockStatement)) {
                return;
            }

            // Extract the code block from the block statement
            PsiBlockStatement blockStatement = (PsiBlockStatement) body;
            PsiCodeBlock codeBlock = blockStatement.getCodeBlock();

            // Traverse the statements within the code block
            for (PsiStatement blockChild : codeBlock.getStatements()) {
                checkClientCreation(blockChild);
            }
        }

        /**
         * This method checks for the dynamic creation of clients in the code block of a for loop.
         * Each assignment statement and declaration statement is checked for the creation of clients.
         *
         * @param blockChild The statement to check for client creation
         */

        private void checkClientCreation(PsiStatement blockChild) {

            // This is a check for the expression statement
            if (blockChild instanceof PsiExpressionStatement) {
                PsiExpression expression = ((PsiExpressionStatement) blockChild).getExpression();

                if (!(expression instanceof PsiAssignmentExpression)) {
                    return;
                }

                // Extract the assignment expression
                PsiAssignmentExpression assignment = (PsiAssignmentExpression) expression;

                // Extract the right-hand side of the assignment
                PsiExpression rhs = assignment.getRExpression();

                // Check if the right-hand side is a method call expression
                if (rhs != null && isClientCreationMethod((PsiMethodCallExpression) rhs)) {
                    holder.registerProblem(rhs, RULE_CONFIG.getAntiPatternMessageMap().get("antiPatternMessage"));
                }
            } else if (blockChild instanceof PsiDeclarationStatement) {    // This is a check for the declaration statement

                PsiDeclarationStatement declarationStatement = (PsiDeclarationStatement) blockChild;

                // Traverse the declared elements within the declaration statement
                for (PsiElement declaredElement : declarationStatement.getDeclaredElements()) {

                    // Check if the declared element is a local variable
                    if (!(declaredElement instanceof PsiLocalVariable)) {
                        continue;
                    }

                    // Extract the local variable and its initializer
                    PsiLocalVariable localVariable = (PsiLocalVariable) declaredElement;
                    PsiExpression initializer = localVariable.getInitializer();

                    if (!(initializer instanceof PsiMethodCallExpression)) {
                        continue;
                    }

                    // Check if the initializer is a method call expression
                    PsiMethodCallExpression methodCallExpression = (PsiMethodCallExpression) initializer;
                    if (isClientCreationMethod(methodCallExpression)) {
                        holder.registerProblem(methodCallExpression, RULE_CONFIG.getAntiPatternMessageMap().get("antiPatternMessage"));
                    }
                }
            }
        }

        /**
         * This method checks if the method call expression is a client creation method.
         * It checks the method name and the type of the qualifier expression.
         * If the method name is buildClient or AsyncBuildClient and the qualifier expression is of type com.azure,
         * then it is considered a client creation method.
         *
         * @param methodCallExpression The method call expression to check
         * @return boolean - true if the method call expression is a client creation method
         */
        public boolean isClientCreationMethod(PsiMethodCallExpression methodCallExpression) {

            // Extract the method expression from the method call expression
            PsiReferenceExpression methodExpression = methodCallExpression.getMethodExpression();

            // Extract the method name
            String methodName = methodExpression.getReferenceName();

            // Check if the method name is buildClient or AsyncBuildClient
            if (methodName != null && RULE_CONFIG.getMethodsToCheck().contains(methodName)) {

                // Extract the qualifier expression
                PsiExpression qualifierExpression = methodExpression.getQualifierExpression();

                if (qualifierExpression == null || qualifierExpression.getType() == null) {
                    return false;
                }

                // Check if the qualifier expression is of type com.azure
                return qualifierExpression.getType().getCanonicalText().startsWith(RuleConfig.AZURE_PACKAGE_NAME);
            }
            return false;
        }
    }
}
