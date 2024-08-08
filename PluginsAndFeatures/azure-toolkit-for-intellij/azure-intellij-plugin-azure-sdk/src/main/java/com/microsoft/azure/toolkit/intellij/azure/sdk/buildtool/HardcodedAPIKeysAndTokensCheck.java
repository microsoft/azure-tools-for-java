package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiNewExpression;
import org.jetbrains.annotations.NotNull;

/**
 * This class is a custom inspection tool that checks for hardcoded API keys and tokens in the code.
 * It extends the LocalInspectionTool class and overrides the buildVisitor method to create a visitor for the inspection.
 * The visitor checks for the use of specific methods that are known to be used for API keys and tokens.
 * These are AzureKeyCredential and AccessToken.
 * <p>
 * These are some instances that a flag would be raised.
 * 1. TextAnalyticsClient client = new TextAnalyticsClientBuilder()
 * .endpoint(endpoint)
 * .credential(new AzureKeyCredential(apiKey))
 * .buildClient();
 * <p>
 * 2. TokenCredential credential = request -> {
 * AccessToken token = new AccessToken("<your-hardcoded-token>", OffsetDateTime.now().plusHours(1));
 * }
 */
public class HardcodedAPIKeysAndTokensCheck extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new APIKeysAndTokensVisitor(holder, isOnTheFly);
    }

    // This class is a visitor that checks for the use of specific methods that authenticate API keys and tokens.
    static class APIKeysAndTokensVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;

        // // Define constants for string literals
        private static final RuleConfig RULE_CONFIG;
        private static final boolean SKIP_WHOLE_RULE;

        static {
            final String ruleName = "HardcodedAPIKeysAndTokensCheck";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            RULE_CONFIG = centralRuleConfigLoader.getRuleConfig(ruleName);
            SKIP_WHOLE_RULE = RULE_CONFIG == RuleConfig.EMPTY_RULE || RULE_CONFIG.getServicesToCheck().isEmpty();
        }


        // This constructor is used to create a visitor for the inspection
        public APIKeysAndTokensVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
        }

        @Override
        public void visitElement(@NotNull PsiElement element) {
            super.visitElement(element);

            if (SKIP_WHOLE_RULE) {
                return;
            }

            // Check if the element is a new expression -- i.e., a constructor call
            if (element instanceof PsiNewExpression) {

                // Cast the element to a new expression
                PsiNewExpression newExpression = (PsiNewExpression) element;
                if (newExpression.getClassReference() == null) {
                    return;
                }

                // Get the class reference name from the new expression
                String classReference = newExpression.getClassReference().getReferenceName();

                // Check if the class reference is not null, the qualifier name starts with "com.azure" and
                // the class reference is in the list of clients to check
                if (newExpression.getClassReference() != null && newExpression.getClassReference().getQualifiedName().startsWith(RuleConfig.AZURE_PACKAGE_NAME) && RULE_CONFIG.getServicesToCheck().contains(classReference)) {
                    this.holder.registerProblem(newExpression, RULE_CONFIG.getAntiPatternMessageMap().get("antiPatternMessage"));
                }
            }
        }
    }
}
