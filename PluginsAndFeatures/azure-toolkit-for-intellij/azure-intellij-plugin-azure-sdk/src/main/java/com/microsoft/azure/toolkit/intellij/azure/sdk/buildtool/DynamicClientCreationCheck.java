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
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * This class is used to check for the dynamic creation of clients in the code.
 * It extends the LocalInspectionTool class, which is used to create custom code inspections.
 * The visitor inspects a for loop and checks for methodcall expressions that create clients using the buildClient or buildAsyncClient methods.
 * If a client creation method is found building a client from the com.azure package, a problem is registered.
 */
public class DynamicClientCreationCheck extends LocalInspectionTool {


    /**
     * ]
     * This method builds the visitor that checks for the dynamic creation of clients in the code.
     *
     * @param holder
     * @param isOnTheFly
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
    public static class DynamicClientCreationVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private final boolean isOnTheFly;

        // Constants
        private static final Map<String, Object> RULE_CONFIGS_MAP;
        private static final String RULE_NAME = "DynamicClientCreationCheck";
        private static final String METHODS_TO_CHECK_KEY = "methods_to_check";
        private static final String ANTI_PATTERN_MESSAGE_KEY = "antipattern_message";
        private static final List<String> METHODS_TO_CHECK;
        private static final String ANTI_PATTERN_MESSAGE;


        // Load the config file
        static {
            try {
                RULE_CONFIGS_MAP = getClientCreationMethodsToCheck();

                // extract the methods to check and the anti-pattern message from the config file
                METHODS_TO_CHECK = (List<String>) RULE_CONFIGS_MAP.get(METHODS_TO_CHECK_KEY);
                ANTI_PATTERN_MESSAGE = (String) RULE_CONFIGS_MAP.get(ANTI_PATTERN_MESSAGE_KEY);

            } catch (IOException e) {
                throw new RuntimeException("Error loading config file", e);
            }
        }

        /**
         * This constructor initializes the ProblemsHolder and isOnTheFly variables.
         *
         * @param holder
         * @param isOnTheFly
         */
        public DynamicClientCreationVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
            this.isOnTheFly = isOnTheFly;
        }

        /**
         * This method checks for the dynamic creation of clients in the code.
         *
         * @param statement
         */
        @Override
        public void visitForStatement(@NotNull PsiForStatement statement) {
            super.visitForStatement(statement);

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
         * @param blockChild
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
                if (rhs != null) {
                    if (isClientCreationMethod((PsiMethodCallExpression) rhs)) {
                        holder.registerProblem(rhs, ANTI_PATTERN_MESSAGE);
                    }
                }
            }

            // This is a check for the declaration statement
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
                    holder.registerProblem(methodCallExpression, ANTI_PATTERN_MESSAGE);
                }
            }
        }

        /**
         * This method checks if the method call expression is a client creation method.
         * It checks the method name and the type of the qualifier expression.
         * If the method name is buildClient or AsyncBuildClient and the qualifier expression is of type com.azure,
         * then it is considered a client creation method.
         * @param methodCallExpression
         * @return
         */
        public boolean isClientCreationMethod (PsiMethodCallExpression methodCallExpression){

            // Extract the method expression from the method call expression
            PsiReferenceExpression methodExpression = methodCallExpression.getMethodExpression();

            // Extract the method name
            String methodName = methodExpression.getReferenceName();

            // Check if the method name is buildClient or AsyncBuildClient
            if (methodName != null && METHODS_TO_CHECK.contains(methodName)) {

                // Extract the qualifier expression
                PsiExpression qualifierExpression = methodExpression.getQualifierExpression();

                // Check if the qualifier expression is of type com.azure
                if (qualifierExpression.getType().getCanonicalText().startsWith("com.azure")) {
                    return true;
                }
            }
            return false;
        }

        /**
         * This method loads the JSON configuration file and extracts the methods to check for dynamic client creation.
         * @return Map<String, Object>
         * @throws IOException
         */
        private static Map<String, Object> getClientCreationMethodsToCheck() throws IOException {

            //load json object
            JSONObject jsonObject = LoadJsonConfigFile.getInstance().getJsonObject();

            //get the async return types to check
            JSONArray asyncReturnTypes = jsonObject.getJSONObject(RULE_NAME).getJSONArray(METHODS_TO_CHECK_KEY);

            // extract string from json object
            String antiPatternMessage = jsonObject.getJSONObject(RULE_NAME).getString(ANTI_PATTERN_MESSAGE_KEY);

            return Map.of(METHODS_TO_CHECK_KEY, asyncReturnTypes.toList(), ANTI_PATTERN_MESSAGE_KEY, antiPatternMessage.toString());
        }
    }
}
