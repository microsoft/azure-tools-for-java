package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.XmlSuppressableInspectionTool;
import com.intellij.lang.StdLanguages;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

public class MavenProjectInspection extends XmlSuppressableInspectionTool {
    @Nls
    @NotNull
    @Override
    public String getDisplayName() {
        return "Check Maven Project";
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {

            @Override
            public void visitFile(@NotNull PsiFile file) {
                if (!file.getName().equals("pom.xml")) {
                    return;
                }
                MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(file.getProject());
                if (!mavenProjectsManager.isMavenizedProject()) {
                    return;
                }
                FileViewProvider viewProvider = file.getViewProvider();
                XmlFile xmlFile = (XmlFile) viewProvider.getPsi(StdLanguages.XML);

                XmlTag rootTag = xmlFile.getRootTag();
                if (rootTag != null && "project".equals(rootTag.getName())) {
                    checkDependencyManagement(rootTag, holder);
                    checkDependencies(rootTag, holder);
                }
            }
        };
    }

    private static void checkDependencyManagement(XmlTag rootTag, @NotNull ProblemsHolder holder) {
        XmlTag dependencyManagement = rootTag.findFirstSubTag("dependencyManagement");
        if (dependencyManagement != null) {
            XmlTag dependenciesTag = dependencyManagement.findFirstSubTag("dependencies");
            if (dependenciesTag != null) {
                XmlTag[] dependencyTags = dependenciesTag.findSubTags("dependency");
                for (XmlTag dependencyTag : dependencyTags) {
                    String groupId = dependencyTag.findFirstSubTag("groupId").getValue().getText();
                    String artifactId = dependencyTag.findFirstSubTag("artifactId").getValue().getText();
                    String versionId = null;

                    if (dependencyTag.findFirstSubTag("version") != null) {
                        versionId = dependencyTag.findFirstSubTag("version").getValue().getText();
                    }

                    if ("com.azure".equals(groupId) && artifactId.equals("azure-sdk-bom")) {
                        if (!versionId.equals("1.2.24")) {
                            holder.registerProblem(dependencyTag, "Newer version of azure-sdk-bom is available. Update to version 1.2.24");
                        }
                    }
                }
            }
        }
    }

    private static void checkDependencies(XmlTag rootTag, @NotNull ProblemsHolder holder) {
        XmlTag dependenciesTag = rootTag.findFirstSubTag("dependencies");
        if (dependenciesTag != null) {
            XmlTag[] dependencyTags = dependenciesTag.findSubTags("dependency");
            for (XmlTag dependencyTag : dependencyTags) {
                String groupId = dependencyTag.findFirstSubTag("groupId").getValue().getText();
                String artifactId = dependencyTag.findFirstSubTag("artifactId").getValue().getText();
                String versionId = null;
                if (dependencyTag.findFirstSubTag("version") != null) {
                    versionId = dependencyTag.findFirstSubTag("version").getValue().getText();
                }
                if ("com.azure".equals(groupId) && versionId != null && !versionId.contains("beta")) {
                    holder.registerProblem(dependencyTag, "Use Azure SDK Bom (azure-sdk-bom) instead of specifying version in dependency tag");
                }

                if ("com.azure".equals(groupId) && versionId != null && versionId.contains("beta")) {
                    holder.registerProblem(dependencyTag, "Using a beta version in production is not recommended. Please use a stable version.");
                }
            }
        }
    }


}
