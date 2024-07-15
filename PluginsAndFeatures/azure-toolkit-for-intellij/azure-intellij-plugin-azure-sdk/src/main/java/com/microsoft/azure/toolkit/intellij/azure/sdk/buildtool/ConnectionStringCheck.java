package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;

import org.jetbrains.annotations.NotNull;


/**
 * This class extends the LocalInspectionTool to check for the use of connection strings in the code.
 * If the method is called from an Azure client class, a problem is registered with the suggestion message.
 */
public class ConnectionStringCheck extends LocalInspectionTool {


    /**
     * This method builds the visitor for the inspection tool.
     *
     * @param holder     ProblemsHolder to register problems
     * @param isOnTheFly boolean to check if the inspection is on the fly. If true, the inspection is performed as you type.
     * @return PsiElementVisitor visitor to inspect elements in the code
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new ConnectionStringVisitor(holder, isOnTheFly);
    }

    /**
     * This class extends the JavaElementVisitor to visit the elements in the code.
     * It checks if the method call is a connection string call and if the class is an Azure client.
     * If both conditions are met, a problem is registered with the suggestion message.
     */
    static class ConnectionStringVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;

        // // Define constants for string literals
        private static final RuleConfig ruleConfig;
        private static final String SUGGESTION;
        private static final String METHOD_TO_CHECK;
        private static boolean SKIP_WHOLE_RULE;

        static {
            final String ruleName = "ConnectionStringCheck";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            ruleConfig = centralRuleConfigLoader.getRuleConfig(ruleName);

            METHOD_TO_CHECK = ruleConfig.getMethodsToCheck().get(0);
            SUGGESTION = ruleConfig.getAntiPatternMessage();
            SKIP_WHOLE_RULE = ruleConfig == RuleConfig.EMPTY_RULE;
        }

        /**
         * Constructor to initialize the visitor with the holder and isOnTheFly flag.
         *
         * @param holder     ProblemsHolder to register problems
         * @param isOnTheFly boolean to check if the inspection is on the fly
         */
        public ConnectionStringVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
        }

        /**
         * This method visits the element in the code.
         * It checks if the method call is a connection string call and if the class is an Azure client.
         * If both conditions are met, a problem is registered with the suggestion message.
         *
         * @param element PsiElement to visit
         */
        @Override
        public void visitElement(@NotNull PsiElement element) {
            super.visitElement(element);

            // skip the whole rule if the rule is empty
            if (SKIP_WHOLE_RULE) {
                return;
            }

            // check if an element is an azure client
            if (element instanceof PsiMethodCallExpression) {

                PsiMethodCallExpression methodCallExpression = (PsiMethodCallExpression) element;
                PsiReferenceExpression methodExpression = methodCallExpression.getMethodExpression();

                // resolvedMethod is the method that is being called
                PsiElement resolvedMethod = methodExpression.resolve();

                // check if the method is a connectionString call
                if (resolvedMethod != null && resolvedMethod instanceof PsiMethod && ((PsiMethod) resolvedMethod).getName().equals(METHOD_TO_CHECK)) {
                    PsiMethod method = (PsiMethod) resolvedMethod;

                    // containingClass is the client class that is being called. check if the class is an azure client
                    PsiClass containingClass = method.getContainingClass();

                    // compare the package name of the containing class to the azure package name from the configuration file
                    if (containingClass != null && containingClass.getQualifiedName() != null && containingClass.getQualifiedName().startsWith(RuleConfig.AZURE_PACKAGE_NAME)) {
                        holder.registerProblem(element, SUGGESTION);
                    }
                }
            }
        }
    }
}
