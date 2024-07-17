package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiBlockStatement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiDoWhileStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiForStatement;
import com.intellij.psi.PsiForeachStatement;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.PsiWhileStatement;
import com.intellij.psi.util.PsiTreeUtil;

import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * Inspection to check if there is a Text Analytics client operation inside a loop.
 * If a Text Analytics client operation is found inside a loop,
 * and the API has a batch alternative, a problem will be registered.
 * <p>
 * This is an example of a situation where the inspection should register a problem:
 * <p>
 * // Loop through the list of texts and detect the language for each text
 * 1. for (String text : texts) {
 * DetectedLanguage detectedLanguage = textAnalyticsClient.detectLanguage(text);
 * System.out.println("Text: " + text + " | Detected Language: " + detectedLanguage.getName() + " | Confidence Score: " + detectedLanguage.getConfidenceScore());
 * }
 * <p>
 *     // Traditional for-loop to recognize entities for each text
 * for (int i = 0; i < texts.size(); i++) {
 *     String text = texts.get(i);
 *     textAnalyticsClient.recognizeEntities(text);
 *     // Process recognized entities if needed
 * }
 */
public class SingleOperationInLoopCheck extends LocalInspectionTool {

    /**
     * Build the visitor for the inspection. This visitor will be used to traverse the PSI tree.
     *
     * @param holder The holder for the problems found
     * @return The visitor for the inspection
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new SingleOperationInLoopVisitor(holder, isOnTheFly);
    }

    /**
     * Visitor class to traverse the PSI tree and check for single Azure client operation inside a loop.
     * The visitor will check for loops of type for, foreach, while, and do-while.
     * The visitor will check for a Text Analytics client operation inside the loop.
     * If a Text Analytics client operation is found inside the loop, and the API has a batch alternative, a problem will be registered.
     */
    static class SingleOperationInLoopVisitor extends JavaElementVisitor {

        // Define the holder for the problems found and whether the inspection is running on the fly
        private final ProblemsHolder holder;

        // // Define constants for string literals
        private static final RuleConfig RULE_CONFIG;
        private static final String SUGGESTION;
        private static final List<String> AVAILABLE_BATCH_METHODS;
        private static final boolean SKIP_WHOLE_RULE;

        static {
            final String ruleName = "SingleOperationInLoopCheck";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            RULE_CONFIG = centralRuleConfigLoader.getRuleConfig(ruleName);

            AVAILABLE_BATCH_METHODS = RULE_CONFIG.getMethodsToCheck();
            SUGGESTION = RULE_CONFIG.getAntiPatternMessage();
            SKIP_WHOLE_RULE = RULE_CONFIG == RuleConfig.EMPTY_RULE || AVAILABLE_BATCH_METHODS.isEmpty();
        }

        /**
         * Constructor for the visitor
         *
         * @param holder     The holder for the problems found
         * @param isOnTheFly Whether the inspection is running on the fly. If true, the inspection is running as you type.
         */
        public SingleOperationInLoopVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
        }

        /**
         * Visit the for statement and check for single Azure client operation inside the loop.
         *
         * @param statement The for statement to check
         */
        @Override
        public void visitForStatement(@NotNull PsiForStatement statement) {
            if (SKIP_WHOLE_RULE) {
                return;
            }
            checkLoopForTextAnalyticsClientOperation(statement);
        }

        /**
         * Visit the foreach statement and check for single Azure client operation inside the loop.
         *
         * @param statement The foreach statement to check
         */
        @Override
        public void visitForeachStatement(@NotNull PsiForeachStatement statement) {
            if (SKIP_WHOLE_RULE) {
                return;
            }
            checkLoopForTextAnalyticsClientOperation(statement);
        }

        /**
         * Visit the while statement and check for single Azure client operation inside the loop.
         *
         * @param statement The while statement to check
         */
        @Override
        public void visitWhileStatement(@NotNull PsiWhileStatement statement) {
            if (SKIP_WHOLE_RULE) {
                return;
            }
            checkLoopForTextAnalyticsClientOperation(statement);
        }

        /**
         * Visit the do-while statement and check for single Azure client operation inside the loop.
         *
         * @param statement The do-while statement to check
         */
        @Override
        public void visitDoWhileStatement(@NotNull PsiDoWhileStatement statement) {
            if (SKIP_WHOLE_RULE) {
                return;
            }
            checkLoopForTextAnalyticsClientOperation(statement);
        }


        /**
         * Check the loop statement for a single Text Analytics Azure client operation inside the loop.
         *
         * @param loopStatement The loop statement to check
         */
        private boolean checkLoopForTextAnalyticsClientOperation(PsiStatement loopStatement) {

            // extract body of the loop
            PsiStatement loopBody = getLoopBody(loopStatement);

            if (loopBody == null) {
                return false;
            }

            if (!(loopBody instanceof PsiBlockStatement)) {
                return false;
            }

            // Extract the code block from the block statement
            PsiBlockStatement blockStatement = (PsiBlockStatement) loopBody;
            PsiCodeBlock codeBlock = blockStatement.getCodeBlock();

            // extract statements in the loop body
            for (PsiStatement statement : codeBlock.getStatements()) {

                // Check if the statement is an expression statement and is an Azure client operation
                if (statement instanceof PsiExpressionStatement) {
                    isExpressionAzureClientOperation(statement);
                }

                // Check if the statement is a declaration statement and is an Azure client operation
                if (statement instanceof PsiDeclarationStatement) {
                    isDeclarationAzureClientOperation((PsiDeclarationStatement) statement);
                }
            }
            return true;
        }


        /**
         * Get the body of the loop statement.
         * The body of the loop statement is the statement that is executed in the loop.
         *
         * @param loopStatement The loop statement to get the body from
         * @return The body of the loop statement
         */
        public static PsiStatement getLoopBody(PsiStatement loopStatement) {

            // Check the type of the loop statement and return the body of the loop statement
            if (loopStatement instanceof PsiForStatement) return ((PsiForStatement) loopStatement).getBody();
            if (loopStatement instanceof PsiForeachStatement) return ((PsiForeachStatement) loopStatement).getBody();
            if (loopStatement instanceof PsiWhileStatement) return ((PsiWhileStatement) loopStatement).getBody();
            if (loopStatement instanceof PsiDoWhileStatement) return ((PsiDoWhileStatement) loopStatement).getBody();
            return null;
        }

        /**
         * If the statement is an expression statement, check if the expression is an Azure client operation.
         *
         * @param statement The statement to check
         */
        private void isExpressionAzureClientOperation(PsiStatement statement) {

            // Get the expression from the statement
            PsiExpression expression = ((PsiExpressionStatement) statement).getExpression();

            if (expression instanceof PsiMethodCallExpression) {
                // Check if the expression is an Azure client operation
                if (isAzureTextAnalyticsClientOperation((PsiMethodCallExpression) expression)) {

                    // get the method name
                    String methodName = ((PsiMethodCallExpression) expression).getMethodExpression().getReferenceName();
                    holder.registerProblem(expression, (SUGGESTION + methodName + "Batch"));
                }
            }
        }

        /**
         * If the statement is a declaration statement, check if the initializer is an Azure client operation.
         *
         * @param statement The declaration statement to check
         */
        private void isDeclarationAzureClientOperation(PsiDeclarationStatement statement) {

            // getDeclaredElements() returns the variables declared in the statement
            for (PsiElement element : statement.getDeclaredElements()) {

                if (!(element instanceof PsiVariable)) {
                    continue;
                }
                // Get the initializer of the variable
                PsiExpression initializer = ((PsiVariable) element).getInitializer();

                if (!(initializer instanceof PsiMethodCallExpression)) {
                    continue;
                }
                // Check if the initializer is an Azure client operation
                if (isAzureTextAnalyticsClientOperation((PsiMethodCallExpression) initializer)) {
                    // get the method name
                    String methodName = ((PsiMethodCallExpression) initializer).getMethodExpression().getReferenceName();
                    holder.registerProblem(initializer, (SUGGESTION + methodName + "Batch"));
                }
            }
        }

        /**
         * Check if the method call is an Azure client operation.
         * Check the containing class of the method call and see if it is part of the Azure SDK.
         * If the class is part of the Azure SDK, increment the count of Azure client operations.
         *
         * @param methodCall The method call expression to check
         * @return True if the method call is an Azure client operation, false otherwise
         */
        private static boolean isAzureTextAnalyticsClientOperation(PsiMethodCallExpression methodCall) {

            String packageName = "com.azure.ai.textanalytics";

            // Get the containing class of the method call
            PsiClass containingClass = PsiTreeUtil.getParentOfType(methodCall, PsiClass.class);

            // Check if the method call is on a class
            if (containingClass != null) {
                String className = containingClass.getQualifiedName();

                // Check if the class is part of the Azure SDK
                if (className != null && className.startsWith(packageName)) {

                    if (AVAILABLE_BATCH_METHODS.contains((methodCall.getMethodExpression().getReferenceName()) + "Batch")) {
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
