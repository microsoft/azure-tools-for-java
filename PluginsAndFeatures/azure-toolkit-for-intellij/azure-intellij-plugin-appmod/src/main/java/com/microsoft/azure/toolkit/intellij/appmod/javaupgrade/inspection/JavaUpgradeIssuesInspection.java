/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.action.JavaUpgradeQuickFix;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.JavaUpgradeIssue;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesCache;

import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesDetectionService.*;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.util.List;
import java.util.Properties;

/**
 * Inspection that displays Java upgrade issues detected by JavaUpgradeDetectionService.
 * Shows JDK version and framework version issues in pom.xml files with wavy underlines.
 * 
 * Note: Issues are cached at project startup via JavaUpgradeIssueCache to avoid
 * repeated expensive scans during inspection runs.
 */
@Slf4j
public class JavaUpgradeIssuesInspection extends LocalInspectionTool {

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        final PsiFile file = holder.getFile();

        // Only process pom.xml files
        if (!(file instanceof XmlFile) || !file.getName().equals("pom.xml")) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        final Project project = holder.getProject();
        final JavaUpgradeIssuesCache cache = JavaUpgradeIssuesCache.getInstance(project);

        // Skip if cache is not yet initialized (will show issues after startup completes)
        if (!cache.isInitialized()) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        final List<JavaUpgradeIssue> dependencyIssues = cache.getDependencyIssues();
        final Properties mavenProperties = getMavenProperties(project, file);

        return new XmlElementVisitor() {
            @Override
            public void visitXmlTag(@NotNull XmlTag tag) {
                super.visitXmlTag(tag);

                if (isJavaVersionProperty(tag) || isCompilerPluginVersionTag(tag)) {
                    final JavaUpgradeIssue javaIssue =
                        JavaUpgradeProblemLocator.createJavaVersionIssue(tag, mavenProperties);
                    if (javaIssue != null) {
                        registerProblem(holder, tag, javaIssue);
                    }
                }

                // Check for dependency/parent version tags and register all matching issues
                if ("version".equals(tag.getName())) {
                    XmlTag parentElement = tag.getParentTag();
                    if (parentElement != null) {
                        String parentTagName = parentElement.getName();
                        if ("dependency".equals(parentTagName) || "parent".equals(parentTagName)) {
                            XmlTag groupIdTag = parentElement.findFirstSubTag("groupId");
                            XmlTag artifactIdTag = parentElement.findFirstSubTag("artifactId");
                            if (groupIdTag != null && artifactIdTag != null) {
                                String packageId = groupIdTag.getValue().getText() + ":" + artifactIdTag.getValue().getText();
                                // Register all issues that match this package
                                for (JavaUpgradeIssue issue : dependencyIssues) {
                                    if (matchesPackageId(packageId, issue.getPackageId())) {
                                        registerProblem(holder, tag, issue);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    @NotNull
    private Properties getMavenProperties(@NotNull Project project, @NotNull PsiFile file) {
        if (file.getVirtualFile() == null) {
            return new Properties();
        }
        final MavenProjectsManager manager = MavenProjectsManager.getInstanceIfCreated(project);
        final MavenProject mavenProject =
            manager == null ? null : manager.findProject(file.getVirtualFile());
        return mavenProject == null ? new Properties() : mavenProject.getProperties();
    }
    private void registerProblem(@NotNull ProblemsHolder holder, @NotNull XmlTag tag, @NotNull JavaUpgradeIssue issue) {
        log.info("Registering Java upgrade issue in inspection: {}", issue);
        holder.registerProblem(
            tag,
            issue.getMessage(),
            ProblemHighlightType.WEAK_WARNING,
            new JavaUpgradeQuickFix(issue)
        );
    }

    /**
     * Checks if the packageId matches the issue's packageId pattern.
     * Supports wildcard patterns like "org.springframework.boot:*" to match any artifact in a group.
     */
    private boolean matchesPackageId(@NotNull String packageId, @NotNull String issuePackageId) {
        if (issuePackageId.endsWith(":*")) {
            // Wildcard match: check if packageId starts with the group prefix
            String groupPrefix = issuePackageId.substring(0, issuePackageId.length() - 1); // "org.springframework.boot:"
            return packageId.startsWith(groupPrefix);
        }
        return packageId.equals(issuePackageId);
    }

    /**
     * Checks if the tag is a Java version property (java.version, maven.compiler.source, maven.compiler.target).
     */
    private boolean isJavaVersionProperty(@NotNull XmlTag tag) {
        String tagName = tag.getName();
        XmlTag parent = tag.getParentTag();
        
        if (parent == null || !"properties".equals(parent.getName())) {
            return false;
        }

        return "java.version".equals(tagName) ||
               "maven.compiler.source".equals(tagName) ||
               "maven.compiler.target".equals(tagName) ||
               "maven.compiler.release".equals(tagName);
    }

    /**
     * Checks if the tag is a version tag inside maven-compiler-plugin configuration.
     */
    private boolean isCompilerPluginVersionTag(@NotNull XmlTag tag) {
        String tagName = tag.getName();
        if (!"source".equals(tagName) && !"target".equals(tagName) && !"release".equals(tagName)) {
            return false;
        }

        // Check if we're inside maven-compiler-plugin
        XmlTag current = tag.getParentTag();
        while (current != null) {
            if ("plugin".equals(current.getName())) {
                XmlTag artifactIdTag = current.findFirstSubTag("artifactId");
                if (artifactIdTag != null && ARTIFACT_ID_MAVEN_COMPILER_PLUGIN.equals(artifactIdTag.getValue().getText())) {
                    return true;
                }
            }
            current = current.getParentTag();
        }
        return false;
    }
}
