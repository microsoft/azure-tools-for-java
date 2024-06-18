package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import org.json.*;
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
        return "Use Storage APIs With Length Parameter";
    }

    private static final List<String> METHODS_TO_CHECK = getMethodsToCheck();

    private static List<String> getMethodsToCheck() {
        try {
            InputStream inputStream = StorageUploadWithoutLengthCheck.class.getClassLoader().getResourceAsStream("META-INF/ruleConfigs.json");
            if (inputStream == null) {
                throw new FileNotFoundException();
            }
            JSONObject jsonObject = new JSONObject(new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining()));
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
                    boolean hasLengthArg = false;

                    PsiExpressionList argumentList = expression.getArgumentList();
                    PsiExpression[] arguments = argumentList.getExpressions();

                    for (PsiExpression arg : arguments) {
                        if (arg.getType().toString().equals("PsiType:long")) {
                            hasLengthArg = true;
                            break;
                        }

                        if (arg instanceof PsiMethodCallExpression) {
                            PsiMethodCallExpression argMethodCall = (PsiMethodCallExpression) arg;
                            PsiExpression qualifier = argMethodCall.getMethodExpression().getQualifierExpression();

                            while (qualifier instanceof PsiMethodCallExpression) {
                                PsiMethodCallExpression qualifierMethodCall = (PsiMethodCallExpression) qualifier;
                                PsiExpressionList qualifierArgumentList = qualifierMethodCall.getArgumentList();
                                PsiExpression[] qualifierArguments = qualifierArgumentList.getExpressions();

                                qualifier = qualifierMethodCall.getMethodExpression().getQualifierExpression();

                                if (qualifier instanceof PsiNewExpression) {
                                    PsiNewExpression newExpression = (PsiNewExpression) qualifier;
                                    PsiExpressionList newExpressionArgumentList = newExpression.getArgumentList();
                                    PsiExpression[] newExpressionArguments = newExpressionArgumentList.getExpressions();

                                    for (PsiExpression newExpressionArgument : newExpressionArguments) {
                                        if (newExpressionArgument.getType().toString().equals("PsiType:long")) {
                                            hasLengthArg = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!hasLengthArg) {
                     holder.registerProblem(expression, "Azure Storage upload API without length parameter detected");
                    }
                }
            }
        };
    };
}