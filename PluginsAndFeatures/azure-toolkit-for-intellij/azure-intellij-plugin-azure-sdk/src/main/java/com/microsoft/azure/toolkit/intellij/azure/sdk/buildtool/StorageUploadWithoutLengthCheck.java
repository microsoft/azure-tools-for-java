package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
        return "Ensure Storage APIs use Length Parameter";
    }

    private static final List<String> METHODS_TO_CHECK = getMethodsToCheck();

    private static List<String> getMethodsToCheck() {
        try {
            String configFileName = "META-INF/ruleConfigs.json";
            InputStream inputStream = StorageUploadWithoutLengthCheck.class.getClassLoader().getResourceAsStream(configFileName);
            if (inputStream == null) {
                throw new FileNotFoundException("Configuration file not found");
            }
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                JSONObject jsonObject = new JSONObject(bufferedReader.lines().collect(Collectors.joining()));
                JSONArray methods = jsonObject.getJSONObject("StorageUploadWithoutLengthCheck").getJSONArray("methodsToCheck");
                return methods.toList().stream().map(Object::toString).collect(Collectors.toList());
            }
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
                    boolean hasLengthArg = false;

                    PsiExpressionList argumentList = expression.getArgumentList();
                    PsiExpression[] arguments = argumentList.getExpressions();

                    for (PsiExpression arg : arguments) {
                        // Check if the argument is of type 'long'
                        if (arg.getType().toString().equals("PsiType:long")) {
                            hasLengthArg = true;
                            break;
                        }
                        if (arg instanceof PsiMethodCallExpression) {
                            PsiMethodCallExpression argMethodCall = (PsiMethodCallExpression) arg;
                            PsiExpression qualifier = argMethodCall.getMethodExpression().getQualifierExpression();

                            // Analysing arguments that are chained method calls
                            hasLengthArg = checkMethodCallChain(argMethodCall);
                        }
                    }
                    if (!hasLengthArg) {
                     holder.registerProblem(expression, "Azure Storage upload API without length parameter detected");
                    }
                }
            }
        };
    };

    // Analysing arguments that are chained method calls
    private boolean checkMethodCallChain (PsiMethodCallExpression expression) {

        // Iterating up the chain of method calls
        PsiExpression qualifier = expression.getMethodExpression().getQualifierExpression();
        while (qualifier instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression qualifierMethodCall = (PsiMethodCallExpression) qualifier;
            PsiExpressionList qualifierArgumentList = qualifierMethodCall.getArgumentList();
            PsiExpression[] qualifierArguments = qualifierArgumentList.getExpressions();

            qualifier = qualifierMethodCall.getMethodExpression().getQualifierExpression();

            // Checking for constructor with 'long' type arguments
            if (qualifier instanceof PsiNewExpression) {
                PsiNewExpression newExpression = (PsiNewExpression) qualifier;
                PsiExpressionList newExpressionArgumentList = newExpression.getArgumentList();
                PsiExpression[] newExpressionArguments = newExpressionArgumentList.getExpressions();

                for (PsiExpression newExpressionArgument : newExpressionArguments) {
                    if (newExpressionArgument.getType().toString().equals("PsiType:long")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}