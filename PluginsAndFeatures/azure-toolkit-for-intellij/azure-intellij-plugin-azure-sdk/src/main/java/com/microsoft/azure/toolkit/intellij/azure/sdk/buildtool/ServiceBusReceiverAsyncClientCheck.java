package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiTypeElement;
import org.jetbrains.annotations.NotNull;


/**
 * This class extends the LocalInspectionTool to check for the use of ServiceBusReceiverAsyncClient
 * in the code and suggests using ServiceBusProcessorClient instead.
 * The client data is loaded from the configuration file and the client name is checked against the
 * discouraged client name. If the client name matches, a problem is registered with the suggestion message.
 */
public class ServiceBusReceiverAsyncClientCheck extends LocalInspectionTool {

    // Define constants for string literals
    private static final String CLIENT_NAME;
    private static final String SUGGESTION;
    private static final boolean SKIP_WHOLE_RULE;

    // Static initializer block to load the client data once
    static {
        final String ruleName = "ServiceBusReceiverAsyncClientCheck";

        RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();
        final RuleConfig ruleConfig = centralRuleConfigLoader.getRuleConfig(ruleName);

        SKIP_WHOLE_RULE = ruleConfig == RuleConfig.EMPTY_RULE;
        CLIENT_NAME = ruleConfig.getClientsToCheck().get(0);
        SUGGESTION = ruleConfig.getAntiPatternMessageMap().get("antiPatternMessage");
    }

    /**
     * This method builds a visitor to check for the discouraged client name in the code.
     * If the client name matches the discouraged client, a problem is registered with the suggestion message.
     *
     * @param holder     ProblemsHolder object to register the problem
     * @param isOnTheFly boolean to check if the inspection is on the fly -- This is not in use
     * @return PsiElementVisitor object to visit the elements in the code
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitTypeElement(PsiTypeElement element) {
                super.visitTypeElement(element);

                if (SKIP_WHOLE_RULE) {
                    return;
                }

                // Check if the element is an instance of PsiTypeElement
                if (element instanceof PsiTypeElement && element.getType() != null) {

                    // Register a problem if the client used matches the discouraged client
                    if (element.getType().getPresentableText().equals(CLIENT_NAME)) {
                        holder.registerProblem(element, SUGGESTION);
                    }
                }
            }
        };
    }
}
