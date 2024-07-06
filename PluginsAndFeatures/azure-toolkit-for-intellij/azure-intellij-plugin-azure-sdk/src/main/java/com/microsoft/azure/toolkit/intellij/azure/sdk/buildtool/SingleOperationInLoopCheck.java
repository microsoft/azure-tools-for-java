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
import org.json.JSONObject;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Inspection to check if there is a single Azure client operation inside a loop.
 * A single Azure client operation is defined as a method call on a class that is part of the Azure SDK.
 * If a single Azure client operation is found inside a loop, a problem will be registered.
 *
 * THis is an example of a situation where the inspection should register a problem:
 *
 * 1. With a single PsiDeclarationStatement inside a while loop
 * // While loop
 *         int i = 0;
 *         while (i < 10) {
 *
 *             BlobAsyncClient blobAsyncClient = new BlobClientBuilder()
 *                 .endpoint("https://<your-storage-account-name>.blob.core.windows.net")
 *                 .sasToken("<your-sas-token>")
 *                 .containerName("<your-container-name>")
 *                 .blobName("<your-blob-name>")
 *                 .buildAsyncClient();
 *
 *             i++;
 *         }
 *
 * 2. With a single PsiExpressionStatement inside a for loop
 * for (String documentPath : documentPaths) {
 *
 *             blobAsyncClient.uploadFromFile(documentPath)
 *                 .doOnSuccess(response -> System.out.println("Blob uploaded successfully in enhanced for loop."))
 *                 .subscribe();
 *         }
 */
public class SingleOperationInLoopCheck extends LocalInspectionTool {

    /**
     * Build the visitor for the inspection. This visitor will be used to traverse the PSI tree.
     * @param holder The holder for the problems found
     * @param isOnTheFly Whether the inspection is running on the fly. If true, the inspection is running as you type.
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
     * The visitor will check for single Azure client operation inside the loop body.
     * If a single Azure client operation is found, a problem will be registered.
     */
    public static class SingleOperationInLoopVisitor extends JavaElementVisitor {

        // Define the holder for the problems found and whether the inspection is running on the fly
        private final ProblemsHolder holder;
        private final boolean isOnTheFly;

        // Define the logger for the visitor
        private static final Logger LOGGER = Logger.getLogger(SingleOperationInLoopCheck.class.getName());

        // Define the suggestion message for the problem
        private static String SUGGESTION = "";

        static {
            try {
                SUGGESTION = getRuleConfigs();
            } catch (IOException e) {
                LOGGER.severe("Failed to load rule configurations");
            }
        }

        /**
         * Constructor for the visitor
         * @param holder     The holder for the problems found
         * @param isOnTheFly Whether the inspection is running on the fly. If true, the inspection is running as you type.
         */
        public SingleOperationInLoopVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
            this.isOnTheFly = isOnTheFly;
        }

        /**
         * Visit the for statement and check for single Azure client operation inside the loop.
         * @param statement The for statement to check
         */
        @Override
        public void visitForStatement(@NotNull PsiForStatement statement) {
            checkLoopForSingleClientOperation(statement);
        }

        /**
         * Visit the foreach statement and check for single Azure client operation inside the loop.
         * @param statement The foreach statement to check
         */
        @Override
        public void visitForeachStatement(@NotNull PsiForeachStatement statement) {
            checkLoopForSingleClientOperation(statement);
        }

        /**
         * Visit the while statement and check for single Azure client operation inside the loop.
         * @param statement The while statement to check
         */
        @Override
        public void visitWhileStatement(@NotNull PsiWhileStatement statement) {
            checkLoopForSingleClientOperation(statement);
        }

        /**
         * Visit the do-while statement and check for single Azure client operation inside the loop.
         * @param statement The do-while statement to check
         */
        @Override
        public void visitDoWhileStatement(@NotNull PsiDoWhileStatement statement) {
            checkLoopForSingleClientOperation(statement);
        }

        /**
         * Check if there is a single Azure client operation inside the loop.
         * A single Azure client operation is defined as a method call on a class that is part of the Azure SDK.
         * If a single Azure client operation is found, a problem will be registered.
         * @param loopStatement The loop statement to check
         */
        private void checkLoopForSingleClientOperation(PsiStatement loopStatement) {

            // Count the number of Azure client operations inside the loop
            int numOfAzureClientOperations = countAzureClientOperations(loopStatement);
            if (numOfAzureClientOperations == 1) {
                holder.registerProblem(loopStatement, SUGGESTION);
            }
        }

        /**
         * A single Azure client operation is defined as a method call on a class that is part of the Azure SDK.
         * If an Azure client operation is found, the count will be incremented.
         * @param loopStatement The loop statement to check
         */
        private int countAzureClientOperations(PsiStatement loopStatement) {

            int numOfAzureClientOperations = 0;

            // extract body of the loop
            PsiStatement loopBody = getLoopBody(loopStatement);

            if (loopBody == null) {
                return 0;
            }

            // Extract the code block from the block statement
            PsiBlockStatement blockStatement = (PsiBlockStatement) loopBody;
            PsiCodeBlock codeBlock = blockStatement.getCodeBlock();

            // extract statements in the loop body
            for (PsiStatement statement : codeBlock.getStatements()) {

                // Check if the statement is an expression statement and is an Azure client operation
                if (statement instanceof PsiExpressionStatement &&
                        isExpressionAzureClientOperation(statement)) {

                    // Increment the count of Azure client operations if the expression statement is an Azure client operation
                    numOfAzureClientOperations++;
                }

                // Check if the statement is a declaration statement and is an Azure client operation
                if (statement instanceof PsiDeclarationStatement &&
                        isDeclarationAzureClientOperation((PsiDeclarationStatement) statement)) {

                    // Increment the count of Azure client operations if the declaration statement is an Azure client operation
                    numOfAzureClientOperations++;
                }
            }
            return numOfAzureClientOperations;
        }


        /**
         * Get the body of the loop statement.
         * The body of the loop statement is the statement that is executed in the loop.
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
         * @param statement The statement to check
         * @return True if the statement is an Azure client operation, false otherwise
         */
        private boolean isExpressionAzureClientOperation(PsiStatement statement) {

            // Get the expression from the statement
            PsiExpression expression = ((PsiExpressionStatement) statement).getExpression();

            if (!(expression instanceof PsiMethodCallExpression)) {
                return false;
            }
            // Check if the expression is an Azure client operation
            if (isAzureClientOperation((PsiMethodCallExpression) expression)) {
                return true;
            }
            return false;
        }

        /**
         * If the statement is a declaration statement, check if the initializer is an Azure client operation.
         * @param statement The declaration statement to check
         * @return True if the declaration statement is an Azure client operation, false otherwise
         */
        private boolean isDeclarationAzureClientOperation(PsiDeclarationStatement statement) {

            // getDeclaredElements() returns the variables declared in the statement
            for (PsiElement element :  statement.getDeclaredElements()) {

                if (!(element instanceof PsiVariable)) {
                    continue;
                }
                // Get the initializer of the variable
                PsiExpression initializer = ((PsiVariable) element).getInitializer();

                if (!(initializer instanceof PsiMethodCallExpression)) {
                    continue;
                }
                // Check if the initializer is an Azure client operation
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

            // Get the containing class of the method call
            PsiClass containingClass = PsiTreeUtil.getParentOfType(methodCall, PsiClass.class);

            // Check if the method call is on a class
            if (containingClass != null) {
                String className = containingClass.getQualifiedName();

                // Check if the class is part of the Azure SDK
                if (className != null && className.startsWith("com.azure.")) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Get the rule configurations from the JSON file.
         * @return The rule configurations
         * @throws IOException
         */
        private static String getRuleConfigs() throws IOException {

            final String ruleName = "SingleOperationInLoopCheck";
            final String antiPatternMessageKey = "antipattern_message";

            final JSONObject jsonObject =  LoadJsonConfigFile.getInstance().getJsonObject();
            final String antiPatternMessage = jsonObject.getJSONObject(ruleName).getString(antiPatternMessageKey);
            return antiPatternMessage;
        }
    }
}