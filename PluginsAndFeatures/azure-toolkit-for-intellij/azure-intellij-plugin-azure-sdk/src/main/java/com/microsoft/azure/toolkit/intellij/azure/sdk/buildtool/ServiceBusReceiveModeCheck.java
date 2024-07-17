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

import java.util.OptionalInt;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class extends the LocalInspectionTool and is used to inspect the usage of Azure SDK ServiceBus & ServiceBusProcessor clients in the code.
 * It checks if the receive mode is set to PEEK_LOCK and the prefetch count is set to a value greater than 1.
 * If the receive mode is set to PEEK_LOCK and the prefetch count is set to a value greater than 1, a problem is registered with the ProblemsHolder.
 */
public class ServiceBusReceiveModeCheck extends LocalInspectionTool {

    /**
     * Build the visitor for the inspection. This visitor will be used to traverse the PSI tree.
     *
     * @param holder     The holder for the problems found
     * @param isOnTheFly Whether the inspection is on the fly -- not in use
     * @return The visitor for the inspection. This is not used anywhere else in the code.
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new ServiceBusReceiveModeVisitor(holder, isOnTheFly);
    }

    /**
     * This class extends the JavaElementVisitor and is used to visit the Java elements in the code.
     * It checks for the usage of Azure SDK ServiceBusReceiver & ServiceBusProcessor clients and
     * whether the receive mode is set to PEEK_LOCK and the prefetch count is set to a value greater than 1.
     * If the receive mode is set to PEEK_LOCK and the prefetch count is set to a value greater than 1,
     * a problem is registered with the ProblemsHolder.
     */
    static class ServiceBusReceiveModeVisitor extends JavaElementVisitor {

        // Define the holder for the problems found
        private final ProblemsHolder holder;

        // Define constants for string literals
        private static final RuleConfig RULE_CONFIG;
        private static final boolean SKIP_WHOLE_RULE;

        private static final Logger LOGGER = Logger.getLogger(ServiceBusReceiveModeCheck.class.getName());

        static {
            final String ruleName = "ServiceBusReceiveModeCheck";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            RULE_CONFIG = centralRuleConfigLoader.getRuleConfig(ruleName);
            SKIP_WHOLE_RULE = RULE_CONFIG.skipRuleCheck() || RULE_CONFIG.getMethodsToCheck().isEmpty();
        }

        /**
         * Constructor for the visitor
         *
         * @param holder     The holder for the problems found
         * @param isOnTheFly Whether the inspection is on the fly -- not in use
         */
        ServiceBusReceiveModeVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
        }

        /**
         * This method is used to visit the declaration statement in the code.
         * It checks for the declaration of the Azure SDK ServiceBusReceiver & ServiceBusProcessor clients
         * and whether the receive mode is set to PEEK_LOCK and the prefetch count is set to a value greater than 1.
         * If the receive mode is set to PEEK_LOCK and the prefetch count is set to a value greater than 1,
         * a problem is registered with the ProblemsHolder.
         *
         * @param statement The declaration statement to visit
         */
        @Override
        public void visitDeclarationStatement(PsiDeclarationStatement statement) {
            super.visitDeclarationStatement(statement);

            if (SKIP_WHOLE_RULE) {
                return;
            }

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
         * It checks if the client type is an Azure ServiceBus client and
         * retrieves the client name (left side of the declaration).
         *
         * @param variable The variable to process
         */
        private void processVariableDeclaration(PsiVariable variable) {

            // Retrieve the client name (left side of the declaration)
            PsiType clientType = variable.getType();

            // Check if the client type is an Azure ServiceBus client
            if (!clientType.getCanonicalText().startsWith(RuleConfig.AZURE_PACKAGE_NAME) && !RULE_CONFIG.getClientsToCheck().contains(clientType.getCanonicalText())) {
                return;
            }

            // Check the assignment part (right side)
            PsiExpression initializer = variable.getInitializer();

            if (!(initializer instanceof PsiMethodCallExpression)) {
                return;
            }

            // Process the new expression initialization
            determineReceiveMode((PsiMethodCallExpression) initializer);

        }

        /**
         * This method is used to determine the receive mode of the Azure ServiceBus client.
         * It checks if the receive mode is set to PEEK_LOCK and the prefetch count is set to a value greater than 1.
         * If the receive mode is set to PEEK_LOCK and the prefetch count is set to a value greater than 1,
         * a problem is registered with the ProblemsHolder.
         *
         * @param methodCall The method call expression to check
         */
        private void determineReceiveMode(PsiMethodCallExpression methodCall) {

            OptionalInt prefetchCountValue = OptionalInt.empty();
            boolean isReceiveModePeekLock = false;
            PsiElement prefetchCountMethod = null;

            // Iterating up the chain of method calls
            PsiExpression qualifier = methodCall.getMethodExpression().getQualifierExpression();

            // Check if the method call chain has the method to check
            while (qualifier instanceof PsiMethodCallExpression) {

                // Get the method expression
                PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) qualifier).getMethodExpression();

                // Get the method name
                String methodName = methodExpression.getReferenceName();

                // Check if the method name is the method to check
                if (!(RULE_CONFIG.getMethodsToCheck().contains(methodName))) {
                    return;
                }
                if ("receiveMode".equals(methodName)) {
                    isReceiveModePeekLock = receiveModePeekLockCheck((PsiMethodCallExpression) qualifier);
                } else if ("prefetchCount".equals(methodName)) {
                    prefetchCountValue = getPrefetchCount((PsiMethodCallExpression) qualifier);
                    prefetchCountMethod = ((PsiMethodCallExpression) qualifier).getMethodExpression().getReferenceNameElement();
                }

                // Get the qualifier of the method call expression -- the next method call in the chain
                qualifier = ((PsiMethodCallExpression) qualifier).getMethodExpression().getQualifierExpression();

                // If the receive mode is set to PEEK_LOCK and the prefetch count is set to a value greater than 1, register a problem
                if (prefetchCountValue.isPresent() && prefetchCountValue.getAsInt() > 1 && isReceiveModePeekLock && prefetchCountMethod != null) {
                    holder.registerProblem(prefetchCountMethod, RULE_CONFIG.getAntiPatternMessage());
                    return;
                }
            }
        }

        /**
         * This method is used to check if the receive mode is set to PEEK_LOCK.
         *
         * @param qualifier The method call expression to check
         * @return true if the receive mode is set to PEEK_LOCK, false otherwise
         */
        private boolean receiveModePeekLockCheck(PsiMethodCallExpression qualifier) {

            String peekLockArgument = "PEEK_LOCK";

            // Get the arguments of the method call expression
            PsiExpression[] arguments = qualifier.getArgumentList().getExpressions();

            for (PsiExpression argument : arguments) {
                String argumentText = argument.getText();

                // Check if the argument is set to PEEK_LOCK
                if (argumentText.contains(peekLockArgument)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * This method is used to get the prefetch count value.
         *
         * @param qualifier The method call expression to check
         * @return The prefetch count value
         */
        private OptionalInt getPrefetchCount(PsiMethodCallExpression qualifier) {

            // Get the arguments of the method call expression
            PsiExpression[] arguments = qualifier.getArgumentList().getExpressions();
            if (arguments.length > 0) {

                // Get the argument text
                String argumentText = arguments[0].getText();
                try {
                    // Parse the argument text to an integer to get the prefetch count value
                    return OptionalInt.of(Integer.parseInt(argumentText));
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.SEVERE, "Failed to parse prefetch count: " + argumentText, e);
                }
            }
            // OptionalInt is used here as a container that can either hold an integer value or indicate that no value is present.
            // This approach is necessary because the method signature specifies an int return type, which cannot be null
            // Using OptionalInt avoids the ambiguity of returning a special value (like 0) to indicate absence, especially since 0 is a legitimate value for prefetchCount.
            return OptionalInt.empty();
        }
    }
}
