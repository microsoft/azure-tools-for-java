package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class extends the LocalInspectionTool to check for the use of connection strings in the code.
 * If the method is called from an Azure client class, a problem is registered with the suggestion message.
 */
public class ConnectionStringCheck extends LocalInspectionTool {

    private static List <String> ruleConfigurations;
    private static final Logger LOGGER = Logger.getLogger(ServiceBusReceiverAsyncClientCheck.class.getName());


    // Load the rule configurations from the configuration file
    static {
        try {
            ruleConfigurations = loadRuleConfigurations();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading client data", e);
        }
    }

    private static final String RULE_NAME = "ConnectionStringCheck";
    private static final String METHOD_TO_CHECK_KEY = "method_to_check";
    private static final String SUGGESTION_KEY = "antipattern_message";

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

                    PsiMethodCallExpression methodCallExpression = (PsiMethodCallExpression) element;
                    PsiReferenceExpression methodExpression = methodCallExpression.getMethodExpression();

                    // resolvedMethod is the method that is being called
                    PsiElement resolvedMethod = methodExpression.resolve();

                    // check if the method is a connectionString call
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
    private static List<String> loadRuleConfigurations() throws IOException {

        final JSONObject jsonObject = LoadJsonConfigFile.getInstance().getJsonObject();
        String methodToCheck = jsonObject.getJSONObject(RULE_NAME).getString(METHOD_TO_CHECK_KEY);
        String antipatternMessage = jsonObject.getJSONObject(RULE_NAME).getString(SUGGESTION_KEY);
        return Arrays.asList(methodToCheck, antipatternMessage);
    }
}