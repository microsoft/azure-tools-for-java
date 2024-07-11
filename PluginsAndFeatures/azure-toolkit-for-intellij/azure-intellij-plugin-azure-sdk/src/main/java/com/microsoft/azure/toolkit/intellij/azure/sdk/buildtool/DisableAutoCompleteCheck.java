package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This class extends the LocalInspectionTool and is used to inspect the usage of Azure SDK ServiceBusReceiver & ServiceBusProcessor clients in the code.
 * It checks if the auto-complete feature is disabled for the clients.
 * If the auto-complete feature is not disabled, a problem is registered with the ProblemsHolder.
 */
public class DisableAutoCompleteCheck extends LocalInspectionTool {

    /**
     * Build the visitor for the inspection. This visitor will be used to traverse the PSI tree.
     *
     * @param holder The holder for the problems found
     * @return The visitor for the inspection. This is not used anywhere else in the code.
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new DisableAutoCompleteVisitor(holder, isOnTheFly);
    }


    /**
     * This class extends the JavaElementVisitor and is used to visit the Java elements in the code.
     * It checks for the usage of Azure SDK ServiceBusReceiver & ServiceBusProcessor clients and
     * whether the auto-complete feature is disabled.
     * If the auto-complete feature is not disabled, a problem is registered with the ProblemsHolder.
     */
    static class DisableAutoCompleteVisitor extends JavaElementVisitor {

        // Define constants for string literals
        private static String SUGGESTION;
        private static List<String> CLIENTS_TO_CHECK;
        private static String METHOD_TO_CHECK;
        private static final Logger LOGGER = Logger.getLogger(ServiceBusReceiverAsyncClientCheck.class.getName());

        static {
            try {
                getRuleConfigData();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error loading client data", e);
            }
        }

        // Define the holder for the problems found
        private static ProblemsHolder holder;

        /**
         * Constructor for the visitor
         *
         * @param holder     The holder for the problems found
         */
        public DisableAutoCompleteVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
        }

        /**
         * This method is used to visit the declaration statements in the code.
         * It checks for the declaration of Azure SDK ServiceBusReceiver & ServiceBusProcessor clients and
         * whether the auto-complete feature is disabled.
         * If the auto-complete feature is not disabled, a problem is registered with the ProblemsHolder.
         *
         * @param statement The declaration statement to visit
         */
        @Override
        public void visitDeclarationStatement(PsiDeclarationStatement statement) {
            super.visitDeclarationStatement(statement);

            // Get the declared elements
            PsiElement[] elements = statement.getDeclaredElements();

            // Get the variable declaration
            if (elements.length > 0 && elements[0] instanceof PsiVariable) {
                PsiVariable variable = (PsiVariable) elements[0];

                // Process the variable declaration
                processVariableDeclaration(variable);
            }
        }

        /**
         * This method is used to process the variable declaration.
         * It checks for the declaration of Azure SDK ServiceBusReceiver & ServiceBusProcessor clients and
         * whether the auto-complete feature is disabled.
         * If the auto-complete feature is not disabled, a problem is registered with the ProblemsHolder.
         *
         * @param variable The variable to process
         */
        private void processVariableDeclaration(PsiVariable variable) {

            // Retrieve the client name (left side of the declaration)
            PsiType clientType = variable.getType();

            // Check the assignment part (right side)
            PsiExpression initializer = variable.getInitializer();

            // Check if the client type is an Azure SDK client
            if (!clientType.getCanonicalText().startsWith("com.azure")) {
                return;
            }

            // Check if the client type is in the list of clients to check
            if (CLIENTS_TO_CHECK.contains(clientType.getPresentableText())) {

                if (!(initializer instanceof PsiMethodCallExpression)) {
                    return;
                }

                // Process the new expression initialization
                if (!isAutoCompleteDisabled((PsiMethodCallExpression) initializer)){

                    // Register a problem if the auto-complete feature is not disabled
                    holder.registerProblem(initializer, SUGGESTION);
                }
            }
        }

        /**
         * This method is used to check if the auto-complete feature is disabled.
         * It iterates up the chain of method calls to check if the auto-complete feature is disabled.
         *
         * @param methodCallExpression The method call expression to check
         * @return true if the auto-complete feature is disabled, false otherwise
         */
        private static boolean isAutoCompleteDisabled(PsiMethodCallExpression methodCallExpression) {

            // Iterating up the chain of method calls
            PsiExpression qualifier = methodCallExpression.getMethodExpression().getQualifierExpression();

            // Check if the method call chain has the method to check
            while (qualifier instanceof PsiMethodCallExpression) {

                qualifier = ((PsiMethodCallExpression) qualifier).getMethodExpression().getQualifierExpression();

                if (qualifier instanceof PsiMethodCallExpression) {

                    // Get the method expression
                    PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) qualifier).getMethodExpression();

                    // Get the method name
                    String methodName = methodExpression.getReferenceName();

                    // Check if the method name is the method to check
                    if (METHOD_TO_CHECK.equals(methodName)) {
                        return true;
                    }
                }
            }
            // When the chain has been traversed and the method to check is not found
            return false;
        }


        /**
         * This method is used to get the rule configuration data from the JSON configuration file.
         *
         * @throws IOException if an error occurs while loading the JSON configuration file
         */
        private static void getRuleConfigData() throws IOException {

            final String ruleName = "DisableAutoCompleteCheck";
            final String methodToCheckKey = "methods_to_check";
            final String suggestionKey = "antipattern_message";
            final String clientsToCheckKey = "clients_to_check";

            final JSONObject jsonObject = LoadJsonConfigFile.getInstance().getJsonObject();
            METHOD_TO_CHECK = jsonObject.getJSONObject(ruleName).getString(methodToCheckKey);
            SUGGESTION = jsonObject.getJSONObject(ruleName).getString(suggestionKey);
            CLIENTS_TO_CHECK = jsonObject.getJSONObject(ruleName).getJSONArray(clientsToCheckKey).toList().stream().map(Object::toString).collect(Collectors.toList());
        }
    }
}