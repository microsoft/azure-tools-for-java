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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class extends the LocalInspectionTool to check for the use of connection strings in the code.
 * If the method is called from an Azure client class, a problem is registered with the suggestion message.
 */
public class ConnectionStringCheck extends LocalInspectionTool {


    /**
     * This method builds the visitor for the inspection tool.
     * @param holder ProblemsHolder to register problems
     * @param isOnTheFly boolean to check if the inspection is on the fly. If true, the inspection is performed as you type.
     * @return
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new ConnectionStringVisitor(holder, isOnTheFly);
    }

    /**
     * This class extends the JavaElementVisitor to visit the elements in the code.
     * It checks if the method call is a connection string call and if the class is an Azure client.
     * If both conditions are met, a problem is registered with the suggestion message.
     */
    public static class ConnectionStringVisitor extends JavaElementVisitor {

        private final ProblemsHolder holder;
        private final boolean isOnTheFly;
        private static final Logger LOGGER = Logger.getLogger(ConnectionStringCheck.class.getName());
        private static String METHOD_TO_CHECK = "";
        private static String SUGGESTION = "";
        private static String AZURE_PACKAGE_NAME = "";

        // Load the JSON configuration file to get the method to check and the suggestion message
        static {
            String ruleName = "ConnectionStringCheck";

            try {
                LoadJsonConfigFile config = LoadJsonConfigFile.getInstance();
                METHOD_TO_CHECK = config.getMethodToCheck(ruleName);
                SUGGESTION = config.getSuggestionMessage(ruleName);
            } catch (IOException e) {
                // make a descriptive message
                LOGGER.log(Level.SEVERE, "Failed to load JSON configuration for rule '" + ruleName + "' from expected path: " + LoadJsonConfigFile.CONFIG_FILE_PATH, e);            }
        }

        /**
         * Constructor to initialize the visitor with the holder and isOnTheFly flag.
         * @param holder     ProblemsHolder
         * @param isOnTheFly boolean
         */
        public ConnectionStringVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
            this.isOnTheFly = isOnTheFly;
        }

        /**
         * This method visits the element in the code.
         * It checks if the method call is a connection string call and if the class is an Azure client.
         * If both conditions are met, a problem is registered with the suggestion message.
         * @param element PsiElement
         */
        @Override
        public void visitElement(@NotNull PsiElement element) {
            super.visitElement(element);

            // check if an element is an azure client
            if (element instanceof PsiMethodCallExpression) {

                PsiMethodCallExpression methodCallExpression = (PsiMethodCallExpression) element;
                PsiReferenceExpression methodExpression = methodCallExpression.getMethodExpression();

                // resolvedMethod is the method that is being called
                PsiElement resolvedMethod = methodExpression.resolve();

                // check if the method is a connectionString call
                if (resolvedMethod != null && resolvedMethod instanceof PsiElement && ((PsiMethod) resolvedMethod).getName().equals(METHOD_TO_CHECK)) {
                    PsiMethod method = (PsiMethod) resolvedMethod;

                    // containingClass is the client class that is being called. check if the class is an azure client
                    PsiClass containingClass = method.getContainingClass();

                    // compare the package name of the containing class to the azure package name from the configuration file
                    if (containingClass != null && containingClass.getQualifiedName() != null && containingClass.getQualifiedName().startsWith(LoadJsonConfigFile.PACKAGE_NAME)) {
                        holder.registerProblem(element, SUGGESTION);
                    }
                }
            }
        }
    }
}
