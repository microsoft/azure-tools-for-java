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

public class ServiceBusReceiveModeCheck extends LocalInspectionTool {

    /**
     * Build the visitor for the inspection. This visitor will be used to traverse the PSI tree.
     *
     * @param holder The holder for the problems found
     * @return The visitor for the inspection. This is not used anywhere else in the code.
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new ServiceBusReceiveModeVisitor(holder, isOnTheFly);
    }

    static class ServiceBusReceiveModeVisitor extends JavaElementVisitor {


        // Define the holder for the problems found
        private static ProblemsHolder holder;

        /**
         * Constructor for the visitor
         *
         * @param holder The holder for the problems found
         */
        public ServiceBusReceiveModeVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
        }


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

        private void processVariableDeclaration(PsiVariable variable) {

            // Retrieve the client name (left side of the declaration)
            PsiType clientType = variable.getType();
            System.out.println("clientType: " + clientType);

            // Check the assignment part (right side)
            PsiExpression initializer = variable.getInitializer();
            System.out.println("initializer: " + initializer);
            System.out.println("clientType.getCanonicalText(): " + clientType.getCanonicalText());

            // Check if the client type is an Azure ServiceBus client
            if (!clientType.getCanonicalText().contains("servicebus")){
                return;
            }

            if (!(initializer instanceof PsiMethodCallExpression)) {
                return;
            }

            // Process the new expression initialization
            determineReceiveMode((PsiMethodCallExpression) initializer);

        }

        private void determineReceiveMode(PsiMethodCallExpression methodCall) {

            OptionalInt prefetchCountValue = OptionalInt.empty();
            boolean isReceiveModePeekLock = false;
            PsiMethodCallExpression prefetchCountQualifier = null; // Declaration at the method level

            // Iterating up the chain of method calls
            PsiExpression qualifier = methodCall.getMethodExpression().getQualifierExpression();
            System.out.println("qualifier1: " + qualifier);

            // Check if the method call chain has the method to check
            while (qualifier instanceof PsiMethodCallExpression) {

                if (qualifier instanceof PsiMethodCallExpression) {

                    System.out.println("Next qualifier pass");
                    System.out.println("qualifier2: " + qualifier);

                    // Get the method expression
                    PsiReferenceExpression methodExpression = ((PsiMethodCallExpression) qualifier).getMethodExpression();
                    System.out.println("methodExpression: " + methodExpression);

                    // Get the method name
                    String methodName = methodExpression.getReferenceName();

                    System.out.println("methodName: " + methodName);

                    // Check if the method name is the method to check
                    if ("receiveMode".equals(methodName)) {
                        // get its parameters

                        isReceiveModePeekLock = receiveModePeekLockCheck((PsiMethodCallExpression) qualifier);
                        System.out.println("isReceiveModePeekLock: " + isReceiveModePeekLock);

                        // access its parameters
                    }
                    else if ("prefetchCount".equals(methodName)) {
                        System.out.println("prefetchCount method detected");

                        prefetchCountValue = getPrefetchCount((PsiMethodCallExpression) qualifier);
                        prefetchCountQualifier = (PsiMethodCallExpression) qualifier; // Assigning the current qualifier
                        System.out.println("prefetchCountValue.getAsInt(): " + prefetchCountValue.getAsInt());
                        System.out.println("prefetchCountQualifier: " + prefetchCountQualifier);
                    }
                }

                qualifier = ((PsiMethodCallExpression) qualifier).getMethodExpression().getQualifierExpression();

                if (prefetchCountValue.isPresent() && prefetchCountValue.getAsInt() > 1 && isReceiveModePeekLock) {
                    System.out.println("prefetchCountValue.getAsInt()whol: " + prefetchCountValue.getAsInt());
                    System.out.println("prefetchCountQualifierwhol: " + prefetchCountQualifier);
                    System.out.println("Problem detected");
                    holder.registerProblem(prefetchCountQualifier, "A high prefetch value in PEEK_LOCK detected. We recommend a prefetch value of 0 or 1 for efficient message retrieval.");
                }
            }
        }

        private boolean receiveModePeekLockCheck (PsiMethodCallExpression qualifier) {

            System.out.println("qualifier at receiveModePeekLockCheck: " + qualifier);
            PsiExpression[] arguments = qualifier.getArgumentList().getExpressions();
            System.out.println("arguments: " + arguments);

            for (PsiExpression argument : arguments) {
                System.out.println("argument: " + argument);
                String argumentText = argument.getText();
                System.out.println("argumentText: " + argumentText);

                if (argumentText.contains("PEEK_LOCK")) {
                    System.out.println("Found .PEEK_LOCK in parameters");
                    return true;
                }
            }
            return false;
        }

        private OptionalInt getPrefetchCount(PsiMethodCallExpression qualifier) {

            System.out.println("qualifiergetPrefetchCount: " + qualifier);

            PsiExpression[] arguments = qualifier.getArgumentList().getExpressions();
            System.out.println("arguments: " + arguments);
            System.out.println("arguments.length: " + arguments.length);
            if (arguments.length > 0) {
                String argumentText = arguments[0].getText();
                System.out.println("argumentText: " + argumentText);
                System.out.println("argument: " + arguments[0]);
                try {
                    System.out.println("prefetchCountValue: " + Integer.parseInt(argumentText));
                    return OptionalInt.of(Integer.parseInt(argumentText));
                } catch (NumberFormatException e) {
                    // Handle the case where the argument text is not a valid integer
                }
            }

            // optionalInt is a wrapper for an integer value that may or may not be present
            // This is because can't return null from a method that returns an int
            // and returning 0 is not a good idea because 0 is a valid value for prefetchCount
            return OptionalInt.empty();
        }
    }
}
