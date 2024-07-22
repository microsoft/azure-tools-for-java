package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.annotations.NotNull;

/**
 * This class extends the AbstractUpdateCheckpointChecker class to check for the usage of the updateCheckpointAsync() method call in the code.
 * The visitor inspects the method call expressions and checks if the method call is updateCheckpointAsync().
 * If the method call is updateCheckpointAsync() and the following method is subscribe, a problem is registered.
 */
public class UpdateCheckpointAsyncSubscribeChecker extends AbstractUpdateCheckpointAsyncChecker {

    /**
     * This method creates the visitor for the inspection tool.
     *
     * @param holder     ProblemsHolder to register problems
     * @param isOnTheFly boolean to check if the inspection is on the fly. If true, the inspection is performed as you type. - This parameter is not used in the method but is required by the method signature.
     * @return PsiElementVisitor visitor to inspect elements in the code
     */
    @Override
    protected JavaElementVisitor createVisitor(ProblemsHolder holder, boolean isOnTheFly) {
        return new SubscribeVisitor(holder, isOnTheFly);
    }

    /**
     * This class extends the JavaElementVisitor to visit the elements in the code.
     * It checks if the method call is updateCheckpointAsync() and if the following method is `subscribe`.
     * If both conditions are met, a problem is registered with the suggestion message.
     */
    static class SubscribeVisitor extends JavaElementVisitor {

        // Define the holder to register problems
        private final ProblemsHolder holder;

        /**
         * Constructor to initialize the visitor with the holder and isOnTheFly flag.
         *
         * @param holder     ProblemsHolder to register problems
         * @param isOnTheFly boolean to check if the inspection is on the fly. If true, the inspection is performed as you type.
         */
        SubscribeVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
        }

        // Define constants for string literals
        protected static final RuleConfig RULE_CONFIG;
        protected static final boolean SKIP_WHOLE_RULE;

        static {
            final String ruleName = "UpdateCheckpointAsyncSubscribeChecker";
            RuleConfigLoader centralRuleConfigLoader = RuleConfigLoader.getInstance();

            // Get the RuleConfig object for the rule
            RULE_CONFIG = centralRuleConfigLoader.getRuleConfig(ruleName);
            SKIP_WHOLE_RULE = RULE_CONFIG.skipRuleCheck() || RULE_CONFIG.getAntiPatternMessageMap().isEmpty();
        }

        /**
         * This method visits the method call expressions in the code.
         * It checks if the method call is updateCheckpointAsync() and if the following method is `subscribe`.
         * If both conditions are met, a problem is registered with the suggestion message.
         *
         * @param expression The method call expression to inspect
         */
        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);

            // Check if the rule should be skipped
            if (SKIP_WHOLE_RULE) {
                return;
            }

            if (expression.getMethodExpression() == null || expression.getMethodExpression().getReferenceName() == null) {
                return;
            }

            // Check if the method call is updateCheckpointAsync()
            if ("updateCheckpointAsync".equals(expression.getMethodExpression().getReferenceName())) {

                // Get the method name following the updateCheckpointAsync() method call
                String followingMethod = getFollowingMethodName(expression);

                // Check if the following method is `subscribe` and
                // Check if the updateCheckpointAsync() method call is called on an EventBatchContext object
                if (RULE_CONFIG.getMethodsToCheck().get(0).equals(followingMethod) && isCalledOnEventBatchContext(expression)) {
                    holder.registerProblem(expression, RULE_CONFIG.getAntiPatternMessageMap().get("antiPatternMessage"));
                }
            }
        }
    }
}
