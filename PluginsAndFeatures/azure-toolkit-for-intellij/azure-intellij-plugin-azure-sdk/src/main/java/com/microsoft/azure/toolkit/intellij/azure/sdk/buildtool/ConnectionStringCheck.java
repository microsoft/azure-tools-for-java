package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

/**
 * This class extends the LocalInspectionTool and checks for the usage of connection strings in the code.
 * Connection strings that start with "DefaultEndpointsProtocol=http;AccountName=" are flagged as problems.
 * This is because the use of such connection strings is not recommended.
 */
public class ConnectionStringCheck extends LocalInspectionTool {
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitLiteralExpression(PsiLiteralExpression expression) {
                super.visitLiteralExpression(expression);

                String value = expression.getValue() instanceof String ? (String) expression.getValue() : null;

                if (value != null && value.startsWith("DefaultEndpointsProtocol=http;AccountName=")) {
                    holder.registerProblem(expression, "Use of connection strings is not recommended");
                }
            }
        };
    }
}
