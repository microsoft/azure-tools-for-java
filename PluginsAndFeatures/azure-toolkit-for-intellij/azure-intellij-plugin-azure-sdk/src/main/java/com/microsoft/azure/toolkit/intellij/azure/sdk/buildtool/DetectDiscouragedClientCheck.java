package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiTypeElement;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * This class extends the LocalInspectionTool to check for the use of discouraged clients
 * in the code and suggests using other clients instead.
 * The client data is loaded from the configuration file and the client name is checked against the
 * discouraged client name. If the client name matches, a problem is registered with the suggestion message.
 */

public class DetectDiscouragedClientCheck extends LocalInspectionTool {

    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new DetectDiscouragedClientVisitor(holder, isOnTheFly);
    }

    static class DetectDiscouragedClientVisitor extends JavaElementVisitor {

        // Define the fields for the visitor
        private final boolean isOnTheFly;
        private final ProblemsHolder holder;

        // Constructor for the visitor
        public DetectDiscouragedClientVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
            this.isOnTheFly = isOnTheFly;
        }

        private static Map<String, String> CLIENT_DATA;

        // Define constants for string literals
        private static final RuleConfig RULE_CONFIG;
        private static final Map<String, String> CLIENTS_TO_CHECK;
        private static final boolean SKIP_WHOLE_RULE;

        static {
            final String ruleName = "DetectDiscouragedClientCheck";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            RULE_CONFIG = centralRuleConfigLoader.getRuleConfig(ruleName);
            CLIENTS_TO_CHECK = RULE_CONFIG.getDiscouragedIdentifiersMap();
            SKIP_WHOLE_RULE = RULE_CONFIG == RuleConfig.EMPTY_RULE || CLIENTS_TO_CHECK.isEmpty();
        }

        /**
         * This method builds a visitor to check for the discouraged client name in the code.
         * If the client name matches the discouraged client, a problem is registered with the suggestion message.
         */
        @Override
        public void visitTypeElement(PsiTypeElement element) {
            super.visitTypeElement(element);

            // Check if the element is an instance of PsiTypeElement
            if (element instanceof PsiTypeElement && element.getType() != null) {

                String elementType = element.getType().getPresentableText();

                // Register a problem if the client used matches a discouraged client
                holder.registerProblem(element, CLIENTS_TO_CHECK.get(elementType));
            }
        }
    }
}
