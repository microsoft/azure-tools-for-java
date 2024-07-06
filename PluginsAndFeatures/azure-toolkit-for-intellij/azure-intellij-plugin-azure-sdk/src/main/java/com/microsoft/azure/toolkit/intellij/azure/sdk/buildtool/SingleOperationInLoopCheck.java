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

/**
 * Inspection to check if there is a single Azure client operation inside a loop.
 */
public class SingleOperationInLoopCheck extends LocalInspectionTool {

    /**
     * Build the visitor for the inspection. This visitor will be used to traverse the PSI tree.
     * @param holder The holder for the problems found
     * @param isOnTheFly Whether the inspection is running on the fly. If true, the inspection is running as you type.
     * @return
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new SingleOperationInLoopVisitor(holder, isOnTheFly);
    }

    /**
     * Visitor class to traverse the PSI tree and check for single Azure client operation inside a loop.
     * The visitor will check for loops of type for, foreach, while, and do-while.
     * The visitor will check for single Azure client operation inside the loop body.
     * If a single Azure client operation is found, a problem will be registered.
     */
    public static class SingleOperationInLoopVisitor extends JavaElementVisitor {

        // Define the holder for the problems found and whether the inspection is running on the fly
        private final ProblemsHolder holder;
        private final boolean isOnTheFly;

//        private static int numOfAzureClientOperations = 0;


        /**
         * Constructor for the visitor
         *
         * @param holder     The holder for the problems found
         * @param isOnTheFly Whether the inspection is running on the fly. If true, the inspection is running as you type.
         */
        public SingleOperationInLoopVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
            this.isOnTheFly = isOnTheFly;
        }

        /**
         * Visit the for statement and check for single Azure client operation inside the loop.
         *
         * @param statement The for statement to check
         */
        @Override
        public void visitForStatement(@NotNull PsiForStatement statement) {
            checkLoopForSingleClientOperation(statement);
        }

        /**
         * Visit the foreach statement and check for single Azure client operation inside the loop.
         *
         * @param statement
         */
        @Override
        public void visitForeachStatement(@NotNull PsiForeachStatement statement) {
            checkLoopForSingleClientOperation(statement);
        }

        /**
         * Visit the while statement and check for single Azure client operation inside the loop.
         *
         * @param statement
         */
        @Override
        public void visitWhileStatement(@NotNull PsiWhileStatement statement) {
            checkLoopForSingleClientOperation(statement);
        }

        /**
         * Visit the do-while statement and check for single Azure client operation inside the loop.
         *
         * @param statement
         */
        @Override
        public void visitDoWhileStatement(@NotNull PsiDoWhileStatement statement) {
            System.out.println("do while statement: " + statement);
            checkLoopForSingleClientOperation(statement);
        }


        /**
         * Check if there is a single Azure client operation inside the loop.
         * A single Azure client operation is defined as a method call on a class that is part of the Azure SDK.
         * If a single Azure client operation is found, a problem will be registered.
         * @param loopStatement The loop statement to check
         */
        private void checkLoopForSingleClientOperation(PsiStatement loopStatement) {

            int numOfAzureClientOperations = countAzureClientOperations(loopStatement);
            System.out.println("StartnumOfAzureClientOperations: " + numOfAzureClientOperations);
            if (numOfAzureClientOperations == 1) {
                holder.registerProblem(loopStatement,
                        "Single operation found in loop. If the SDK provides a batch operation API, use it to perform multiple actions in a single request.");
            }
        }


        /**
         * Count the number of Azure client operations inside the loop.
         * A single Azure client operation is defined as a method call on a class that is part of the Azure SDK.
         * If an Azure client operation is found, the count will be incremented.
         * @param loopStatement The loop statement to check
         */
        private int countAzureClientOperations(PsiStatement loopStatement) {

            int numOfAzureClientOperations = 0;

            System.out.println("loopStatement: " + loopStatement);
            // extract body of the loop
            PsiStatement loopBody = getLoopBody(loopStatement);

            System.out.println("loopBody: " + loopBody);

            // Extract the code block from the block statement
            PsiBlockStatement blockStatement = (PsiBlockStatement) loopBody;
            PsiCodeBlock codeBlock = blockStatement.getCodeBlock();



            if (loopBody == null) {
                return 0;
            }

            System.out.println("loopBody: " + loopBody);

            System.out.println("statements: " + codeBlock.getStatements());

            // extract statements in the loop body

            for (PsiStatement statement : codeBlock.getStatements()) {


                System.out.println("statement: " + statement);
                System.out.println("BeforenumOfAzureClientOperationsPsiExpressionStatement: " + numOfAzureClientOperations);
                if (statement instanceof PsiExpressionStatement && isExpressionAzureClientOperation(statement)) {

                    numOfAzureClientOperations++;
                    System.out.println("AfternumOfAzureClientOperationsPsiExpressionStatement: " + numOfAzureClientOperations);
//                    System.out.println("numOfAzureClientOperations: " + numOfAzureClientOperations);
                }

                System.out.println("BeforenumOfAzureClientOperationsPsiDeclarationStatement: " + numOfAzureClientOperations);
                if (statement instanceof PsiDeclarationStatement && isDeclarationAzureClientOperation((PsiDeclarationStatement) statement)) {
                    numOfAzureClientOperations++;
                    System.out.println("AfternumOfAzureClientOperationsPsiDeclarationStatement: " + numOfAzureClientOperations);

                }
            }
            return numOfAzureClientOperations;
        }


        public static PsiStatement getLoopBody(PsiStatement loopStatement) {
            if (loopStatement instanceof PsiForStatement) return ((PsiForStatement) loopStatement).getBody();
            if (loopStatement instanceof PsiForeachStatement) return ((PsiForeachStatement) loopStatement).getBody();
            if (loopStatement instanceof PsiWhileStatement) return ((PsiWhileStatement) loopStatement).getBody();
            if (loopStatement instanceof PsiDoWhileStatement) return ((PsiDoWhileStatement) loopStatement).getBody();
            return null;
        }

        private boolean isExpressionAzureClientOperation(PsiStatement statement) {
//            PsiExpression expression = ((PsiExpressionStatement) statement).getExpression();
            PsiExpression expression = ((PsiExpressionStatement) statement).getExpression();
            System.out.println("expression: " + expression);

            if (!(expression instanceof PsiMethodCallExpression)) {
                System.out.println("expression not instanceof PsiMethodCallExpression");
                return false;
            }
            if (isAzureClientOperation((PsiMethodCallExpression) expression)) {
                return true;
            }
            return false;
        }

        private boolean isDeclarationAzureClientOperation(PsiDeclarationStatement statement) {

            // getDeclaredElements() returns the variables declared in the statement
            // eg. int a = 5, b = 6; -> getDeclaredElements() returns a and b
            // eg. int a = 5; -> getDeclaredElements() returns a, a list of size 1
            for (PsiElement element :  statement.getDeclaredElements()) {
                System.out.println("element: " + element);

                if (!(element instanceof PsiVariable)) {
                    continue;
                }
                PsiExpression initializer = ((PsiVariable) element).getInitializer();
                System.out.println("initializer: " + initializer);

                if (!(initializer instanceof PsiMethodCallExpression)) {
                    continue;
                }
                if (isAzureClientOperation((PsiMethodCallExpression) initializer)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Check if the method call is an Azure client operation.
         * Check the containing class of the method call and see if it is part of the Azure SDK.
         * If the class is part of the Azure SDK, increment the count of Azure client operations.
         * @param methodCall The method call expression to check
         * @return True if the method call is an Azure client operation, false otherwise
         */
        private static boolean isAzureClientOperation(PsiMethodCallExpression methodCall) {
            PsiClass containingClass = PsiTreeUtil.getParentOfType(methodCall, PsiClass.class);

            System.out.println("containingClass: " + containingClass);

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