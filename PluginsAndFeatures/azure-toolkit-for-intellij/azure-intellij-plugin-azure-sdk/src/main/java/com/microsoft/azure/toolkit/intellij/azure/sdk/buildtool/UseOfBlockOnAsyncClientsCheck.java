package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;

import com.intellij.psi.PsiType;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * Inspection to check for the use of block() method on async clients in Azure SDK.
 * This inspection will check for the use of block() method on reactive types like Mono, Flux, etc.
 */
public class UseOfBlockOnAsyncClientsCheck extends LocalInspectionTool {

    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new UseOfBlockOnAsyncClientsVisitor(holder, isOnTheFly);
    }

    /**
     * Visitor to check for the use of block() method on async clients in Azure SDK.
     */
    public static class UseOfBlockOnAsyncClientsVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private final boolean isOnTheFly;

        private static final String RULE_NAME = "UseOfBlockOnAsyncClientsCheck";
        private static final Map<String, Object> RULE_CONFIGS_MAP;

        private static final String ANTI_PATTERN_MESSAGE_KEY = "antipattern_message";
        private static final String ASYNC_RETURN_TYPES_KEY = "async_return_types_to_check";
        private static final List<String> ASYNC_RETURN_TYPES;
        static final String ANTI_PATTERN_MESSAGE;

        // Load the config file
        static {
            try {
                RULE_CONFIGS_MAP = getAsyncReturnTypesToCheck();

                // extract the async return types to check
                ASYNC_RETURN_TYPES = (List<String>) RULE_CONFIGS_MAP.get(ASYNC_RETURN_TYPES_KEY);

                ANTI_PATTERN_MESSAGE = (String) RULE_CONFIGS_MAP.get(ANTI_PATTERN_MESSAGE_KEY);

            } catch (IOException e) {
                throw new RuntimeException("Error loading config file", e);
            }
        }

        /**
         * Constructor to initialize the visitor with the holder and isOnTheFly flag.
         *
         * @param holder     ProblemsHolder
         * @param isOnTheFly boolean
         */
        public UseOfBlockOnAsyncClientsVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
            this.isOnTheFly = isOnTheFly;
        }

        /**
         * Visit method call expressions to check for the use of block() method on async clients.
         * This method will check if the method call is block() and if it is used in an async context.
         *
         * @param expression
         */
        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);

            // Check if the element is a method call expression
            if (expression instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression methodCall = expression;

                // Check if the method call is block() or a variant of block() method
                if (methodCall.getMethodExpression().getReferenceName().startsWith("block")) {
                    boolean isAsyncContext = checkIfAsyncContext(methodCall);
                    System.out.println("isAsyncContext: " + isAsyncContext);
                    if (isAsyncContext) {
                        // Report the problem
                        holder.registerProblem(expression, ANTI_PATTERN_MESSAGE);
                    }
                }
            }
        }

        /**
         * Helper method to check if the method call is within an async context.
         * This method will check if the method call is on a reactive type like Mono, Flux, etc.
         *
         * @param methodCall
         * @return true if the method call is on a reactive type, false otherwise
         */
        private boolean checkIfAsyncContext(@NotNull PsiMethodCallExpression methodCall) {
            PsiExpression qualifierExpression = methodCall.getMethodExpression().getQualifierExpression();

            System.out.println("qualifierExpression: " + qualifierExpression);

            if (qualifierExpression instanceof PsiMethodCallExpression) {
                PsiMethodCallExpression qualifierMethodCall = (PsiMethodCallExpression) qualifierExpression;
                System.out.println("qualifierMethodCall: " + qualifierMethodCall);

                // Get the type of the qualifier method call
                PsiType qualifierReturnType = qualifierMethodCall.getType();
                System.out.println("qualifierReturnType: " + qualifierReturnType);

                if (qualifierReturnType instanceof PsiClassType) {
                    PsiClass qualifierReturnTypeClass = ((PsiClassType) qualifierReturnType).resolve();
                    System.out.println("qualifierReturnTypeClass: " + qualifierReturnTypeClass);

                    if (qualifierReturnTypeClass != null && isSubclassOfMonoOrFlux(qualifierReturnTypeClass)) {

                        if (isAzureAsyncClient(qualifierMethodCall)) {
                            System.out.println("isAzureAsyncClient: " + qualifierMethodCall);

                            // Report the problem
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        private boolean isAzureAsyncClient(PsiMethodCallExpression qualifierMethodCall) {

            // Get the expression that calls the method returning a reactive type
            PsiExpression clientExpression = qualifierMethodCall.getMethodExpression().getQualifierExpression();
            System.out.println("clientExpression: " + clientExpression);

            if (clientExpression != null) {
                PsiType clientType = clientExpression.getType();
                System.out.println("clientType: " + clientType);

                if (clientType instanceof PsiClassType) {
                    PsiClass clientClass = ((PsiClassType) clientType).resolve();
                    System.out.println("clientClass: " + clientClass);

                    if (clientClass != null && clientClass.getQualifiedName().startsWith("com.azure.") && clientClass.getQualifiedName().endsWith("AsyncClient")) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Helper method to check if the class is a subclass of Mono or Flux.
         *
         * @param psiClass
         * @return true if the class is a subclass of Mono or Flux, false otherwise
         */
        private boolean isSubclassOfMonoOrFlux(PsiClass psiClass) {
            System.out.println("psiClass: " + psiClass);
            return InheritanceUtil.isInheritor(psiClass, "reactor.core.publisher.Mono") ||
                    InheritanceUtil.isInheritor(psiClass, "reactor.core.publisher.Flux");
        }


        /**
         * helper method to load config from json
         *
         * @return Map<String, Object>
         * @throws IOException
         */
        private static Map<String, Object> getAsyncReturnTypesToCheck() throws IOException {

            //load json object
            JSONObject jsonObject = LoadJsonConfigFile.getInstance().getJsonObject();

            //get the async return types to check
            JSONArray asyncReturnTypes = jsonObject.getJSONObject(RULE_NAME).getJSONArray(ASYNC_RETURN_TYPES_KEY);

            // extract string from json object
            String antiPatternMessage = jsonObject.getJSONObject(RULE_NAME).getString(ANTI_PATTERN_MESSAGE_KEY);

            return Map.of(ASYNC_RETURN_TYPES_KEY, asyncReturnTypes.toList(), ANTI_PATTERN_MESSAGE_KEY, antiPatternMessage.toString());
        }
    }
}