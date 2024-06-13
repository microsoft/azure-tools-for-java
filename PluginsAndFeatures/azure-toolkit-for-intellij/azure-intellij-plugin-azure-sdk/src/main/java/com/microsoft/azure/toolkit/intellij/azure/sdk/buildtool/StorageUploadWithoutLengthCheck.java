package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import org.json.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * This class extends the LocalInspectionTool and is used to inspect the usage of Azure Storage upload APIs in the code.
 * It checks if the upload methods are being called without a 'length' parameter of type 'long'.
 * The methods to check are defined in a JSON configuration file.
 * If such a method call is detected, a problem is registered with the ProblemsHolder.
 */

public class StorageUploadWithoutLengthCheck extends LocalInspectionTool {
    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Use Storage APIs With Length Parameter";
    }

    private static final List<String> METHODS_TO_CHECK = getMethodsToCheck();

    private static List<String> getMethodsToCheck() {
        try {
            JSONObject jsonObject = new JSONObject(new String(Files.readAllBytes(Paths.get("C:/Users/t-njmwenjwa/Documents/azure-tools-for-java/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-azure-sdk/src/main/java/com/microsoft/azure/toolkit/intellij/azure/sdk/buildtool/ruleConfigs.json"))));
            JSONArray methods = jsonObject.getJSONObject("StorageUploadWithoutLengthCheck").getJSONArray("methodsToCheck");
            return methods.toList().stream().map(Object::toString).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }


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

                    // TODO: check if a parameter object instantiated from a class with a length parameter field will NOT be detected.
                    // eg line 150 stream parameter in sdk/storage/azure-storage-blob/src/test/java/com/azure/storage/blob/LargeBlobTests.java
                    boolean hasLengthArg = Arrays.stream(arguments)
                            .anyMatch(arg -> arg.getType().equalsToText("long"));
                    if (!hasLengthArg) {
                        holder.registerProblem(expression, "Azure Storage upload API without length parameter detected");
                    }
                }
            };
        };
    }
}
