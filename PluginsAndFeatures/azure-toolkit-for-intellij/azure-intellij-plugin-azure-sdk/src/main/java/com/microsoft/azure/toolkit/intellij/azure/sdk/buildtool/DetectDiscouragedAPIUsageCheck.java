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

import java.util.Map;
/**
 * This class extends the LocalInspectionTool to check for the use of discouraged APIs in the code.
 * If the method is called from an Azure client class, a problem is registered with the suggestion message.
 */
public class DetectDiscouragedAPIUsageCheck extends LocalInspectionTool {


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
        return new DetectDiscouragedAPIUsageVisitor(holder, isOnTheFly);
    }

    /**
     * This class extends the JavaElementVisitor to visit the elements in the code.
     * It checks if the method call is a discouraged API call and if the class is an Azure client.
     * If both conditions are met, a problem is registered with the suggestion message.
     */
    static class DetectDiscouragedAPIUsageVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;

        // // Define constants for string literals
        private static final RuleConfig ruleConfig;
        private static final Map<String, String> METHODS_TO_CHECK;
        private static boolean SKIP_WHOLE_RULE;

        static {
            final String ruleName = "DetectDiscouragedAPIUsageCheck";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            ruleConfig = centralRuleConfigLoader.getRuleConfig(ruleName);
            METHODS_TO_CHECK = ruleConfig.getDiscouragedIdentifiersMap();

            SKIP_WHOLE_RULE = ruleConfig == RuleConfig.EMPTY_RULE || METHODS_TO_CHECK.isEmpty();
        }

        /**
         * Constructor to initialize the visitor with the holder and isOnTheFly flag.
         *
         * @param holder     ProblemsHolder to register problems
         * @param isOnTheFly boolean to check if the inspection is on the fly - This is not in use
         */
        public DetectDiscouragedAPIUsageVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
        }

        /**
         * This method visits the element in the code.
         * It checks if the method call is a discouraged API call and if the class is an Azure client.
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

                // check if the method is a discouraged API call by accessing the keys of the map stored in the configuration file
                if (resolvedMethod != null && resolvedMethod instanceof PsiMethod
                        && METHODS_TO_CHECK.containsKey(((PsiMethod) resolvedMethod).getName())) {

                    PsiMethod method = (PsiMethod) resolvedMethod;

                    // containingClass is the client class that is being called. check if the class is an azure

                    // containingClass is the client class that is being called. check if the class is an azure client
                    PsiClass containingClass = method.getContainingClass();

                    // compare the package name of the containing class to the azure package name from the configuration file
                    if (containingClass != null && containingClass.getQualifiedName() != null
                            && containingClass.getQualifiedName().startsWith(RuleConfig.AZURE_PACKAGE_NAME)) {

                        PsiElement problemElement = methodExpression.getReferenceNameElement();

                        if (problemElement == null) {
                            return;
                        }
                        // give the suggestion of the discouraged method
                        holder.registerProblem(problemElement, METHODS_TO_CHECK.get(method.getName()));
                    }
                }
            }
        }
    }
}
