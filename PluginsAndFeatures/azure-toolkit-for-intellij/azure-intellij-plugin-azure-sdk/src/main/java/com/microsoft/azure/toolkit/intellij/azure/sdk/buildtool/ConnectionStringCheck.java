package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.*;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class extends the LocalInspectionTool and checks for the usage of connection strings API to create clients
 * in the Azure SDK for Java.
 * If a connection string is found, a warning is displayed.
 */
public class ConnectionStringCheck extends LocalInspectionTool {

    private static final List <String> ruleConfigurations = loadRuleConfigurations();

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitElement(@NotNull PsiElement element) {
                super.visitElement(element);

                String METHOD_TO_CHECK = ruleConfigurations.get(0);
                String SUGGESTION = ruleConfigurations.get(1);

                // check if an element is an azure client
                if (element instanceof PsiMethodCallExpression) {

                    // check if the method is a connectionString call
                    PsiMethodCallExpression methodCallExpression = (PsiMethodCallExpression) element;
                    PsiReferenceExpression methodExpression = methodCallExpression.getMethodExpression();

                    // resolvedMethod is the method that is being called
                    PsiElement resolvedMethod = methodExpression.resolve();

                    if (resolvedMethod != null && resolvedMethod instanceof PsiElement && ((PsiMethod) resolvedMethod).getName().equals(METHOD_TO_CHECK)) {
                        PsiMethod method = (PsiMethod) resolvedMethod;

                        // containingClass is the client class that is being called
                        PsiClass containingClass = method.getContainingClass();

                        // check if the class is an azure client
                        if (containingClass != null && containingClass.getQualifiedName() != null && containingClass.getQualifiedName().startsWith("com.azure")) {
                            holder.registerProblem(element, SUGGESTION);
                        }
                    }
                }
            }
        };
    };

    // Load the rule configurations from the configuration file
    private static List<String> loadRuleConfigurations() {

        final String RULE_CONFIGURATION = "META-INF/ruleConfigs.json";
        final String RULE_NAME = "ConnectionStringCheck";
        final String METHOD_TO_CHECK_KEY = "method_to_check";
        final String SUGGESTION_KEY = "antipattern_message";

        try {
            InputStream inputStream = ConnectionStringCheck.class.getClassLoader().getResourceAsStream(RULE_CONFIGURATION);
            if (inputStream == null) {
                throw new FileNotFoundException("Configuration file not found");
            }
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                final JSONObject jsonObject = new JSONObject(bufferedReader.lines().collect(Collectors.joining()));
                JSONObject ruleConfig = jsonObject.getJSONObject(RULE_NAME);
                String methodToCheck = ruleConfig.getString(METHOD_TO_CHECK_KEY);
                String antipatternMessage = ruleConfig.getString(SUGGESTION_KEY);
                return Arrays.asList(methodToCheck, antipatternMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}