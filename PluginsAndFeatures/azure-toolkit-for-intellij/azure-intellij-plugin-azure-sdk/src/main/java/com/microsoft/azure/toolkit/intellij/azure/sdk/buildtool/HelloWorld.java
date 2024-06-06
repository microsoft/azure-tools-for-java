package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

// make a class similar to MavenProjectInspection to trigger a hello world statement

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.StdLanguages;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class HelloWorld extends LocalInspectionTool{

    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Hello World!!";
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {

            @Override
            public void visitFile(@NotNull PsiFile file) {
                if (file.getName().equals("pom.xml")) {
                    System.out.println("Hello World!!");
                    holder.registerProblem(file, "Hello World!!");


                    FileViewProvider viewProvider = file.getViewProvider();
                    XmlFile xmlFile = (XmlFile) viewProvider.getPsi(StdLanguages.XML);

                    XmlTag rootTag = xmlFile.getRootTag();
                    showError(rootTag, holder);
                }
            }
        };
    }

    private static void showError (XmlTag rootTag, @NotNull ProblemsHolder holder) {
        holder.registerProblem(rootTag, "Hello World!!");
    }
}
