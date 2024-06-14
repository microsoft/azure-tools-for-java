package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;


/**
 * This class extends the LocalInspectionTool to check for the use of ServiceBusReceiverAsyncClient
 * in the code and suggests using ServiceBusProcessorClient instead.
 */
public class ServiceBusReceiverAsyncClientCheck extends LocalInspectionTool {


    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {

        return new JavaElementVisitor() {

            @Override
            public void visitLocalVariable(PsiLocalVariable variable) {
                super.visitLocalVariable(variable);

                final PsiType type = variable.getType();
                final String qualifiedName = type.getCanonicalText();

                if (qualifiedName.equals("com.azure.messaging.servicebus.ServiceBusReceiverAsyncClient")) {
                    holder.registerProblem(variable, "Use of ServiceBusReceiverAsyncClient detected. Use ServiceBusProcessorClient instead.");
                }
            }
        };
    }
}
