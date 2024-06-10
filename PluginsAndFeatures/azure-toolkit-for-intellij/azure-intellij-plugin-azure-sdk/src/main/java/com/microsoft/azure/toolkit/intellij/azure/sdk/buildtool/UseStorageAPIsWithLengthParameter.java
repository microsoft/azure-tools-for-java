package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;


public class UseStorageAPIsWithLengthParameter extends LocalInspectionTool {
    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Use Storage APIs With Length Parameter";
    }

    private static final List<String> METHODS_TO_CHECK = Arrays.asList("uploadFromFile", "upload", "uploadWithResponse");

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaRecursiveElementWalkingVisitor() {
            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);
                String methodName = expression.getMethodExpression().getReferenceName();
                if (METHODS_TO_CHECK.contains(methodName)) {
                    PsiExpression[] arguments = expression.getArgumentList().getExpressions();
                    boolean hasInputStreamArg = Arrays.stream(arguments)
                            .anyMatch(arg -> arg.getType().equalsToText("java.io.InputStream"));
                    if (!hasInputStreamArg) {
                        holder.registerProblem(expression, "Use Storage APIs With Length Parameter");
                    }
                }
            }

            ;
        };
    }
}
