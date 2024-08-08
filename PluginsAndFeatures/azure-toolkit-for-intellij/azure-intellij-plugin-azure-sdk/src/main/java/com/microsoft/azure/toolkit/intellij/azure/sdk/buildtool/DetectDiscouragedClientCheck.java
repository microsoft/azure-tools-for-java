package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiTypeElement;
import org.jetbrains.annotations.NotNull;

/**
 * This class extends the LocalInspectionTool to check for the use of discouraged clients
 * in the code and suggests using other clients instead.
 * The client data is loaded from the configuration file and the client name is checked against the
 * discouraged client name. If the client name matches, a problem is registered with the suggestion message.
 */

public class DetectDiscouragedClientCheck extends LocalInspectionTool {

    /**
     * This method builds a visitor to check for the discouraged client name in the code.
     * If the client name matches the discouraged client, a problem is registered with the suggestion message.
     *
     * @param holder     - the ProblemsHolder object to register the problem
     * @param isOnTheFly - whether the inspection is on the fly - not used in this implementation but required by the parent class
     */
    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new DetectDiscouragedClientVisitor(holder);
    }

    /**
     * This class is a visitor that checks for the use of discouraged clients in the code.
     * If the client name matches the discouraged client, a problem is registered with the suggestion message.
     */
    static class DetectDiscouragedClientVisitor extends JavaElementVisitor {

        // Define the fields for the visitor
        private final ProblemsHolder holder;

        /**
         * Constructor for the visitor
         *
         * @param holder - the ProblemsHolder object to register the problem
         */
        DetectDiscouragedClientVisitor(ProblemsHolder holder) {
            this.holder = holder;
        }

        // Define constants for string literals
        private static final RuleConfig RULE_CONFIG;
        private static final boolean SKIP_WHOLE_RULE;

        static {
            final String ruleName = "DetectDiscouragedClientCheck";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            RULE_CONFIG = centralRuleConfigLoader.getRuleConfig(ruleName);
            SKIP_WHOLE_RULE = RULE_CONFIG.skipRuleCheck() || RULE_CONFIG.getAntiPatternMessageMap().isEmpty();
        }

        /**
         * This method builds a visitor to check for the discouraged client name in the code.
         * If the client name matches the discouraged client, a problem is registered with the suggestion message.
         */
        @Override
        public void visitTypeElement(PsiTypeElement element) {
            super.visitTypeElement(element);

            // Skip the whole rule if the configuration is empty
            if (SKIP_WHOLE_RULE) {
                return;
            }
            // Check if the element is an instance of PsiTypeElement
            if (element instanceof PsiTypeElement && element.getType() != null) {

                String elementType = element.getType().getPresentableText();

                if (!RULE_CONFIG.getAntiPatternMessageMap().containsKey(elementType)) {
                    return;
                }

                // Register a problem if the client used matches a discouraged client
                holder.registerProblem(element, RULE_CONFIG.getAntiPatternMessageMap().get(elementType));
            }
        }
    }
}
