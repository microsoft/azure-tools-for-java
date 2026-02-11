/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.inspection;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.action.JavaUpgradeQuickFix;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.JavaUpgradeIssue;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesCache;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesDetectionService;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils.GradleBuildFileUtils;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils.Constants;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Inspection that displays Java upgrade issues detected by JavaUpgradeDetectionService.
 * Shows JDK version and framework version issues in pom.xml and Gradle build files with wavy underlines.
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

        final boolean isPomFile = file instanceof XmlFile && file.getName().equals("pom.xml");
        final boolean isGradleBuildFile = GradleBuildFileUtils.isGradleBuildFile(file);
        final boolean isVersionCatalogFile = GradleBuildFileUtils.isVersionCatalogFile(file);

        // Only process pom.xml, Gradle build files, or version catalog files
        if (!isPomFile && !isGradleBuildFile && !isVersionCatalogFile) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        final Project project = holder.getProject();
        final JavaUpgradeIssuesCache cache = JavaUpgradeIssuesCache.getInstance(project);

        // Skip if cache is not yet initialized (will show issues after startup completes)
        if (!cache.isInitialized()) {
            return PsiElementVisitor.EMPTY_VISITOR;
        }

        // Get cached issues (computed once at project startup)
        // Note: CVE issues are handled separately by CveFixDependencyIntentionAction
        final JavaUpgradeIssue jdkIssue = cache.getJdkIssue();
        final List<JavaUpgradeIssue> dependencyIssues = cache.getDependencyIssues();

        if (isPomFile) {
            return new XmlElementVisitor() {
                @Override
                public void visitXmlTag(@NotNull XmlTag tag) {
                    super.visitXmlTag(tag);

                    // Check for JDK version tags
                    if (jdkIssue != null) {
                        if (isJavaVersionProperty(tag) || isCompilerPluginVersionTag(tag)) {
                            registerProblem(holder, tag, jdkIssue);
                        }
                    }

                    // Check for dependency/parent version tags and register all matching issues
                    // Note: CVE issues are handled separately by CveFixDependencyIntentionAction
                    if ("version".equals(tag.getName())) {
                        XmlTag parentElement = tag.getParentTag();
                        if (parentElement != null) {
                            String parentTagName = parentElement.getName();
                            if ("dependency".equals(parentTagName) || "parent".equals(parentTagName)) {
                                XmlTag groupIdTag = parentElement.findFirstSubTag("groupId");
                                XmlTag artifactIdTag = parentElement.findFirstSubTag("artifactId");
                                if (groupIdTag != null && artifactIdTag != null) {
                                    String packageId = groupIdTag.getValue().getText() + ":" + artifactIdTag.getValue().getText();
                                    // Register dependency upgrade issues
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
        
        // Handle version catalog files (libs.versions.toml)
        if (isVersionCatalogFile) {
            return new PsiElementVisitor() {
                @Override
                public void visitFile(@NotNull PsiFile file) {
                    super.visitFile(file);

                    final String text = file.getText();

                    for (GradleBuildFileUtils.GradleVersionLocation location : GradleBuildFileUtils.findSpringBootVersionsInCatalog(text)) {
                        final String packageId = JavaUpgradeIssuesDetectionService.GROUP_ID_SPRING_BOOT + ":catalog";
                        for (JavaUpgradeIssue issue : dependencyIssues) {
                            if (matchesPackageId(packageId, issue.getPackageId())) {
                                registerProblem(holder, file, location.startOffset(), issue);
                            }
                        }
                    }
                }
            };
        }

        return new PsiElementVisitor() {
            @Override
            public void visitFile(@NotNull PsiFile file) {
                super.visitFile(file);

                final String text = file.getText();
                
                // Track registered offsets to avoid duplicate problems
                final java.util.Set<Integer> registeredOffsets = new java.util.HashSet<>();

                GradleBuildFileUtils.findJavaVersionLocations(text)
                    .forEach(location -> {
                        final JavaUpgradeIssue gradleJdkIssue = buildJdkIssueForVersion(location.version());
                        if (gradleJdkIssue != null) {
                            if (registeredOffsets.add(location.startOffset())) {
                                registerProblem(holder, file, location.startOffset(), gradleJdkIssue);
                            }
                        } else if (jdkIssue != null) {
                            if (registeredOffsets.add(location.startOffset())) {
                                registerProblem(holder, file, location.startOffset(), jdkIssue);
                            }
                        }
                    });

                // Note: CVE issues are handled separately by CveFixDependencyIntentionAction
                for (GradleBuildFileUtils.GradleDependencyLocation location : GradleBuildFileUtils.findDependencyLocations(text)) {
                    final String packageId = location.groupId() + ":" + location.artifactId();
                    // Register dependency upgrade issues
                    for (JavaUpgradeIssue issue : dependencyIssues) {
                        if (matchesPackageId(packageId, issue.getPackageId())) {
                            if (registeredOffsets.add(location.startOffset())) {
                                registerProblem(holder, file, location.startOffset(), issue);
                            }
                        }
                    }
                }

                for (GradleBuildFileUtils.GradleVersionLocation location : GradleBuildFileUtils.findSpringBootPluginVersions(text)) {
                    final String packageId = JavaUpgradeIssuesDetectionService.GROUP_ID_SPRING_BOOT + ":plugin";
                    for (JavaUpgradeIssue issue : dependencyIssues) {
                        if (matchesPackageId(packageId, issue.getPackageId())) {
                            if (registeredOffsets.add(location.startOffset())) {
                                registerProblem(holder, file, location.startOffset(), issue);
                            }
                        }
                    }
                }
            }
        };
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

    private void registerProblem(@NotNull ProblemsHolder holder, @NotNull PsiFile file, int offset, @NotNull JavaUpgradeIssue issue) {
        final PsiElement element = file.findElementAt(Math.max(0, offset));
        final PsiElement target = element != null ? element : file;
        log.info("Registering Java upgrade issue in inspection: {}", issue);
        holder.registerProblem(
            target,
            issue.getMessage(),
            ProblemHighlightType.WEAK_WARNING,
            new JavaUpgradeQuickFix(issue)
        );
    }

    @Nullable
    private JavaUpgradeIssue buildJdkIssueForVersion(@NotNull String versionText) {
        final Integer version = parseJavaVersion(versionText);
        if (version == null) {
            return null;
        }
        if (version < 8) {
            return null;
        }
        if (version >= JavaUpgradeIssuesDetectionService.MATURE_JAVA_LTS_VERSION) {
            return null;
        }

        return JavaUpgradeIssue.builder()
            .packageId(JavaUpgradeIssuesDetectionService.PACKAGE_ID_JDK)
            .packageDisplayName(JavaUpgradeIssuesDetectionService.JDK_DISPLAY_NAME)
            .upgradeReason(JavaUpgradeIssue.UpgradeReason.JRE_TOO_OLD)
            .severity(JavaUpgradeIssue.Severity.WARNING)
            .currentVersion(String.valueOf(version))
            .supportedVersion(">=" + JavaUpgradeIssuesDetectionService.MATURE_JAVA_LTS_VERSION)
            .suggestedVersion(String.valueOf(JavaUpgradeIssuesDetectionService.MATURE_JAVA_LTS_VERSION))
            .message(String.format(Constants.ISSUE_DISPLAY_NAME,
                JavaUpgradeIssuesDetectionService.JDK_DISPLAY_NAME,
                version,
                JavaUpgradeIssuesDetectionService.JDK_DISPLAY_NAME,
                JavaUpgradeIssuesDetectionService.MATURE_JAVA_LTS_VERSION))
            .build();
    }

    @Nullable
    @SuppressWarnings("UnnecessaryTemporaryOnConversionFromString")
    private Integer parseJavaVersion(@NotNull String versionText) {
        try {
            if (versionText.startsWith("1.")) {
                return Integer.parseInt(versionText.substring(2));
            }
            return Integer.parseInt(versionText);
        } catch (NumberFormatException ignored) {
            return null;
        }
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
                if (artifactIdTag != null && JavaUpgradeIssuesDetectionService.ARTIFACT_ID_MAVEN_COMPILER_PLUGIN
                    .equals(artifactIdTag.getValue().getText())) {
                    return true;
                }
            }
            current = current.getParentTag();
        }
        return false;
    }
}
