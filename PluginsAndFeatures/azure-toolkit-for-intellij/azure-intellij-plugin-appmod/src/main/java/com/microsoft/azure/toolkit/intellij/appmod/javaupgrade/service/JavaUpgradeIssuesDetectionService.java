/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.JavaUpgradeIssue;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils.GradleBuildFileUtils;
import com.microsoft.azure.toolkit.intellij.appmod.utils.AppModUtils;
import com.microsoft.azure.toolkit.intellij.common.utils.JdkUtils;
import com.microsoft.intellij.util.GradleUtils;
import com.microsoft.intellij.util.MavenUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.plugins.gradle.model.ExternalProject;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils.Constants.ISSUE_DISPLAY_NAME;

/**
 * Service to detect JDK version and framework dependency versions in Java projects.
 * This service analyzes the project to identify outdated versions that may need upgrading.
 * 
 * This implementation is aligned with the TypeScript version in vscode-java-dependency.
 * @see <a href="https://github.com/microsoft/vscode-java-dependency/blob/main/src/upgrade/assessmentManager.ts">assessmentManager.ts</a>
 */
@Slf4j
public class JavaUpgradeIssuesDetectionService {
    
    /**
     * The mature LTS version of Java that is recommended.
     * Aligned with MATURE_JAVA_LTS_VERSION from vscode-java-dependency.
     */
    public static final int MATURE_JAVA_LTS_VERSION = 21;
    
    // Group ID constants for Spring dependencies
    public static final String GROUP_ID_SPRING_BOOT = "org.springframework.boot";
    public static final String GROUP_ID_SPRING_FRAMEWORK = "org.springframework";
    public static final String GROUP_ID_SPRING_SECURITY = "org.springframework.security";
    
    // Artifact ID constants
    public static final String ARTIFACT_ID_SPRING_BOOT_STARTER_PARENT = "spring-boot-starter-parent";
    public static final String ARTIFACT_ID_MAVEN_COMPILER_PLUGIN = "maven-compiler-plugin";
    
    // Package ID constants (used for cache lookups)
    public static final String PACKAGE_ID_JDK = "jdk";
    public static final String JDK_DISPLAY_NAME = "JDK";
    
    /**
     * Metadata for dependencies to scan.
     * Aligned with DEPENDENCIES_TO_SCAN from vscode-java-dependency.
     */
    public static class DependencyCheckItem {
        @Nonnull public final String groupId;
        @Nonnull public final String artifactId;
        @Nonnull public final String displayName;
        @Nonnull public final String supportedVersion;
        @Nonnull public final String suggestedVersion;
        @Nonnull public final String learnMoreUrl;
        @Nonnull public final Map<String, String> eolDate;
        
        public DependencyCheckItem(@Nonnull String groupId, @Nonnull String artifactId, @Nonnull String displayName, 
                                   @Nonnull String supportedVersion, @Nonnull String suggestedVersion, @Nonnull String learnMoreUrl,
                                   @Nonnull Map<String, String> eolDate) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.displayName = displayName;
            this.supportedVersion = supportedVersion;
            this.suggestedVersion = suggestedVersion;
            this.learnMoreUrl = learnMoreUrl;
            this.eolDate = eolDate;
        }
        
        public String getPackageId() {
            return groupId + ":" + artifactId;
        }
        
        /**
         * Gets the EOL date for a specific version.
         * @param version The version to check (e.g., "2.7.x", "3.5.x")
         * @return The EOL date string (e.g., "2029-06") or null if not found
         */
        @Nullable
        public String getEolDateForVersion(@Nonnull String version) {
            // Try exact match first
            if (eolDate.containsKey(version)) {
                return eolDate.get(version);
            }
            // Try to match major.minor.x pattern
            String[] parts = version.split("\\.");
            if (parts.length >= 2) {
                String pattern = parts[0] + "." + parts[1] + ".x";
                return eolDate.get(pattern);
            }
            return null;
        }
    }
    
    /**
     * Dependencies to scan for upgrade issues.
     * Aligned with DEPENDENCIES_TO_SCAN from dependency.metadata.ts in vscode-java-dependency.
     */
    private static final List<DependencyCheckItem> DEPENDENCIES_TO_SCAN = List.of(
        // Spring Boot - supported versions: 2.7.x or >=3.2.x
        new DependencyCheckItem(
            GROUP_ID_SPRING_BOOT, 
            "*",
            "Spring Boot",
            "2.7.x || >=3.2.x",
            "3.5",
            "https://spring.io/projects/spring-boot#support",
            Map.ofEntries(
                Map.entry("4.0.x", "2027-12"),
                Map.entry("3.5.x", "2032-06"),
                Map.entry("3.4.x", "2026-12"),
                Map.entry("3.3.x", "2026-06"),
                Map.entry("3.2.x", "2025-12"),
                Map.entry("3.1.x", "2025-06"),
                Map.entry("3.0.x", "2024-12"),
                Map.entry("2.7.x", "2029-06"),
                Map.entry("2.6.x", "2024-02"),
                Map.entry("2.5.x", "2023-08"),
                Map.entry("2.4.x", "2023-02"),
                Map.entry("2.3.x", "2022-08"),
                Map.entry("2.2.x", "2022-01"),
                Map.entry("2.1.x", "2021-01"),
                Map.entry("2.0.x", "2020-06"),
                Map.entry("1.5.x", "2020-11")
            )
        ),
        // Spring Framework - supported versions: 5.3.x or >=6.2.x
        new DependencyCheckItem(
            GROUP_ID_SPRING_FRAMEWORK, 
            "*",
            "Spring Framework",
            "5.3.x || >=6.2.x",
            "6.2",
            "https://spring.io/projects/spring-framework#support",
            Map.ofEntries(
                Map.entry("7.0.x", "2028-06"),
                Map.entry("6.2.x", "2032-06"),
                Map.entry("6.1.x", "2026-06"),
                Map.entry("6.0.x", "2025-08"),
                Map.entry("5.3.x", "2029-06"),
                Map.entry("5.2.x", "2023-12"),
                Map.entry("5.1.x", "2022-12"),
                Map.entry("5.0.x", "2022-12"),
                Map.entry("4.3.x", "2020-12")
            )
        ),
        // Spring Security - supported versions: 5.7.x || 5.8.x || >=6.2.x
        new DependencyCheckItem(
            GROUP_ID_SPRING_SECURITY, 
            "*",
            "Spring Security",
            "5.7.x || 5.8.x || >=6.2.x",
            "6.5",
            "https://spring.io/projects/spring-security#support",
            Map.ofEntries(
                Map.entry("7.0.x", "2027-12"),
                Map.entry("6.5.x", "2032-06"),
                Map.entry("6.4.x", "2026-12"),
                Map.entry("6.3.x", "2026-06"),
                Map.entry("6.2.x", "2025-12"),
                Map.entry("6.1.x", "2025-06"),
                Map.entry("6.0.x", "2024-12"),
                Map.entry("5.8.x", "2029-06"),
                Map.entry("5.7.x", "2029-06"),
                Map.entry("5.6.x", "2024-02"),
                Map.entry("5.5.x", "2023-08"),
                Map.entry("5.4.x", "2023-02"),
                Map.entry("5.3.x", "2022-08"),
                Map.entry("5.2.x", "2022-01"),
                Map.entry("5.1.x", "2021-01"),
                Map.entry("5.0.x", "2020-06"),
                Map.entry("4.2.x", "2020-11")
            )
        )
    );
    
    private static final String JDK_LEARN_MORE_URL = 
        "https://learn.microsoft.com/azure/developer/java/fundamentals/java-support-on-azure";
    
    private static JavaUpgradeIssuesDetectionService instance;
    
    /** Formatter for parsing EOL dates in "yyyy-MM" format */
    private static final DateTimeFormatter EOL_DATE_PARSER = DateTimeFormatter.ofPattern("yyyy-MM");
    
    /** Formatter for displaying EOL dates in "MMMM yyyy" format (e.g., "June 2020") */
    private static final DateTimeFormatter EOL_DATE_DISPLAY = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    
    private JavaUpgradeIssuesDetectionService() {
    }
    
    public static synchronized JavaUpgradeIssuesDetectionService getInstance() {
        if (instance == null) {
            instance = new JavaUpgradeIssuesDetectionService();
        }
        return instance;
    }
    
    /**
     * Formats an EOL date from "yyyy-MM" format to "Month yyyy" format.
     * For example, "2020-06" becomes "June 2020".
     * 
     * @param eolDate The EOL date string in "yyyy-MM" format (e.g., "2020-06")
     * @return The formatted date string (e.g., "June 2020"), or the original string if parsing fails
     */
    @Nonnull
    public static String formatEolDate(@Nonnull String eolDate) {
        try {
            YearMonth yearMonth = YearMonth.parse(eolDate, EOL_DATE_PARSER);
            return yearMonth.format(EOL_DATE_DISPLAY);
        } catch (Exception e) {
            // If parsing fails, return the original string
            log.error("Error formatting EOL date '{}': {}", eolDate, e.getMessage());
            return eolDate;
        }
    }
    
    /**
     * Gets JDK/JRE version issues.
     * Aligned with getJavaIssues() from assessmentManager.ts.
     */
    @Nonnull
    public List<JavaUpgradeIssue> getJavaIssues(@Nonnull Project project) {
        final List<JavaUpgradeIssue> issues = new ArrayList<>();
        
        try {
            final Integer jdkVersion = JdkUtils.getJdkLanguageLevel(project);
            log.info("Got JDK version: {}", jdkVersion);
            AppModUtils.logTelemetryEvent("getJavaVersion", Map.of("jdkVersion", String.valueOf(jdkVersion)));
            if (jdkVersion == null) {
                return issues;
            }
            
            // Skip versions below 8 - out of scope
            if (jdkVersion < 8) {
                AppModUtils.logTelemetryEvent("getJavaVersionSkipped", Map.of("jdkVersion", String.valueOf(jdkVersion)));
                log.warn("JDK version below 8 detected ({}), skipping JDK upgrade check", jdkVersion);
                return issues;
            }
            
            // Check against MATURE_JAVA_LTS_VERSION (21)
            if (jdkVersion < MATURE_JAVA_LTS_VERSION) {
                issues.add(JavaUpgradeIssue.builder()
                    .packageId(PACKAGE_ID_JDK)
                    .packageDisplayName(JDK_DISPLAY_NAME)
                    .upgradeReason(JavaUpgradeIssue.UpgradeReason.JRE_TOO_OLD)
                    .severity(JavaUpgradeIssue.Severity.WARNING)
                    .currentVersion(String.valueOf(jdkVersion))
                    .supportedVersion(">=" + MATURE_JAVA_LTS_VERSION)
                    .suggestedVersion(String.valueOf(MATURE_JAVA_LTS_VERSION))
                    .message(String.format(ISSUE_DISPLAY_NAME, JDK_DISPLAY_NAME, jdkVersion, JDK_DISPLAY_NAME, MATURE_JAVA_LTS_VERSION))
                    .learnMoreUrl(JDK_LEARN_MORE_URL)
                    .build());
            }
        } catch (Exception e) {
            // Error checking JDK version
            log.error("Error checking JDK version: {}", e.getMessage(), e);
        }
        
        return issues;
    }
    
    /**
     * Gets dependency issues by checking against DEPENDENCIES_TO_SCAN metadata.
     * Aligned with getDependencyIssue() from assessmentManager.ts.
     */
    @Nonnull
    public List<JavaUpgradeIssue> getDependencyIssues(@Nonnull Project project) {
        final List<JavaUpgradeIssue> issues = new ArrayList<>();
        
        try {
            final Set<String> checkedPackages = new HashSet<>();

            if (MavenUtils.isMavenProject(project)) {
                log.info("Checking Maven project dependencies for upgrade issues");
                final MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstanceIfCreated(project);
                if (mavenProjectsManager != null && mavenProjectsManager.isMavenizedProject()) {
                    final List<MavenProject> mavenProjects = mavenProjectsManager.getProjects();
            
                    for (MavenProject mavenProject : mavenProjects) {
                        for (DependencyCheckItem checkItem : DEPENDENCIES_TO_SCAN) {
                            if (checkedPackages.contains(checkItem.getPackageId())) {
                                continue;
                            }
                    
                            final JavaUpgradeIssue issue = checkDependency(mavenProject, checkItem, checkedPackages);
                            if (issue != null) {
                                issues.add(issue);
                            }
                        }
                    }
                }
            } else if (GradleUtils.isGradleProject(project)) {
                log.info("Checking Gradle project dependencies for upgrade issues");
                final List<ExternalProject> gradleProjects = GradleUtils.listGradleProjects(project);
                for (ExternalProject gradleProject : gradleProjects) {
                    for (DependencyCheckItem checkItem : DEPENDENCIES_TO_SCAN) {
                        if (checkedPackages.contains(checkItem.getPackageId())) {
                            continue;
                        }
                        final JavaUpgradeIssue issue = checkGradleDependency(gradleProject, checkItem, checkedPackages);
                        if (issue != null) {
                            issues.add(issue);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            // Error checking dependencies
            log.error("Error checking dependency issues: {}", e.getMessage(), e);
        }
        
        return issues;
    }
    
    /**
     * Gets CVE (Common Vulnerabilities and Exposures) issues for project dependencies.
     * Aligned with getCVEIssues() from assessmentManager.ts.
     * 
     * @param project The IntelliJ project to analyze
     * @return List of CVE-related upgrade issues
     */
    @Nonnull
    public List<JavaUpgradeIssue> getCVEIssues(@Nonnull Project project) {
        try {
            final Set<String> coordinateSet = new HashSet<>();

            if (MavenUtils.isMavenProject(project)) {
                log.info("Checking Maven project dependencies for CVE issues");
                final MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstanceIfCreated(project);
                if (mavenProjectsManager != null && mavenProjectsManager.isMavenizedProject()) {
                    final List<MavenProject> mavenProjects = mavenProjectsManager.getProjects();
            
                    for (MavenProject mavenProject : mavenProjects) {
                        // Get direct dependencies only (root level of dependency tree)
                        mavenProject.getDependencyTree().stream()
                            .map(MavenArtifactNode::getArtifact)
                            .filter(dep -> StringUtils.isNotBlank(dep.getVersion()))
                            .forEach(dep -> coordinateSet.add(
                                dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion()
                            ));
                    }
                }
            } else if (GradleUtils.isGradleProject(project)) {
                log.info("Checking Gradle project dependencies for CVE issues");
                final List<ExternalProject> gradleProjects = GradleUtils.listGradleProjects(project);
                for (ExternalProject gradleProject : gradleProjects) {
                    collectDirectGradleDependencies(gradleProject, coordinateSet);
                }
            }

            if (coordinateSet.isEmpty()) {
                return Collections.emptyList();
            }
            
            // Check CVEs for all collected dependencies
            final List<String> coordinates = new ArrayList<>(coordinateSet);
            log.info("Checking CVE issues for {} dependencies", coordinates.size());
            return CVECheckService.getInstance().batchGetCVEIssues(coordinates);
            
        } catch (Exception e) {
            // Error checking CVE issues
            log.error("Error checking CVE issues: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Checks a single dependency against its metadata.
     * Aligned with the logic in getDependencyIssue() from assessmentManager.ts.
     */
    @Nullable
    private JavaUpgradeIssue checkDependency(@Nonnull MavenProject mavenProject,
                                              @Nonnull DependencyCheckItem checkItem,
                                              @Nonnull Set<String> checkedPackages) {
        String version = null;
        
        // Special handling for Spring Boot parent POM
        if (GROUP_ID_SPRING_BOOT.equals(checkItem.groupId)) {
            version = getParentVersion(mavenProject, checkItem.groupId, ARTIFACT_ID_SPRING_BOOT_STARTER_PARENT);
        }
        
        // If not found in parent, check direct dependencies
        if (version == null) {
            String targetArtifactId = "*".equals(checkItem.artifactId) ? null : checkItem.artifactId;
            final MavenArtifact dependency = findDirectDependency(mavenProject, checkItem.groupId, targetArtifactId);
            if (dependency != null) {
                version = dependency.getVersion();
            }
        }
        
        if (version == null || StringUtils.isBlank(version)) {
            return null;
        }
        
        checkedPackages.add(checkItem.getPackageId());
        
        // Check if version satisfies the supported version range
        if (!satisfiesVersionRange(version, checkItem.supportedVersion) && isVersionEndOfLife(version, checkItem)) {
            return JavaUpgradeIssue.builder()
                .packageId(checkItem.getPackageId())
                .packageDisplayName(checkItem.displayName)
                .upgradeReason(determineUpgradeReason(version, checkItem))
                .severity(determineSeverity(version, checkItem))
                .currentVersion(version)
                .supportedVersion(checkItem.supportedVersion)
                .suggestedVersion(checkItem.suggestedVersion)
                .message(buildUpgradeMessage(checkItem.displayName, version, checkItem))
                .learnMoreUrl(checkItem.learnMoreUrl)
                    .eofDate(checkItem.getEolDateForVersion(version))
                .build();
        }
        
        return null;
    }

    /**
     * Gets the version from parent POM.
     */
    @Nullable
    private String getParentVersion(@Nonnull MavenProject mavenProject, 
                                    @Nonnull String groupId, 
                                    @Nonnull String artifactId) {
        try {
            final var parentId = mavenProject.getParentId();
            if (parentId != null && 
                groupId.equals(parentId.getGroupId()) && 
                artifactId.equals(parentId.getArtifactId())) {
                return parentId.getVersion();
            }
        } catch (Exception e) {
            // Error getting parent version
            log.error("Error getting parent version for {}:{} - {}", groupId, artifactId, e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * Checks if a version satisfies a version range.
     * Supports ranges like: "2.7.x || >=3.2", ">=10", "5.3.x || >=6.1"
     * Aligned with semver logic from assessmentManager.ts.
     */
    private boolean satisfiesVersionRange(@Nonnull String version, @Nonnull String range) {
        // Split by "||" for OR conditions
        final String[] orConditions = range.split("\\|\\|");
        
        for (String condition : orConditions) {
            condition = condition.trim();
            
            if (satisfiesSingleCondition(version, condition)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a version satisfies a single version condition.
     */
    private boolean satisfiesSingleCondition(@Nonnull String version, @Nonnull String condition) {
        try {
            // Handle ">=" pattern (check before ".x" pattern to handle ">=3.2.x" correctly)
            if (condition.startsWith(">=")) {
                String minVersion = condition.substring(2).trim();
                // Handle version with wildcard, e.g. ">=3.2.x" -> "3.2"
                if (minVersion.endsWith(".x")) {
                    minVersion = minVersion.substring(0, minVersion.length() - 2);
                }
                final ComparableVersion current = new ComparableVersion(version);
                final ComparableVersion min = new ComparableVersion(minVersion);
                return current.compareTo(min) >= 0;
            }
            
            // Handle ">" pattern (check before ".x" pattern to handle ">3.2.x" correctly)
            if (condition.startsWith(">")) {
                String minVersion = condition.substring(1).trim();
                // Handle version with wildcard, e.g. ">3.2.x" -> "3.2"
                if (minVersion.endsWith(".x")) {
                    minVersion = minVersion.substring(0, minVersion.length() - 2);
                }
                final ComparableVersion current = new ComparableVersion(version);
                final ComparableVersion min = new ComparableVersion(minVersion);
                return current.compareTo(min) > 0;
            }
            
            // Handle "x.y.x" pattern (e.g., "2.7.x" means any 2.7.*)
            if (condition.endsWith(".x")) {
                final String prefix = condition.substring(0, condition.length() - 2);
                return version.startsWith(prefix + ".");
            }
            
            // Handle exact version match
            return version.equals(condition);
            
        } catch (Exception e) {
            // Error checking version range
            log.error("Error checking version '{}' against condition '{}': {}", version, condition, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Checks if a version has reached its end-of-life date based on the EOL map.
     * @param version The version to check (e.g., "2.0.1.RELEASE")
     * @param checkItem The dependency check item containing EOL dates
     * @return true if the version is past its EOL date
     */
    private boolean isVersionEndOfLife(@Nonnull String version, @Nonnull DependencyCheckItem checkItem) {
        String eolDateStr = checkItem.getEolDateForVersion(version);
        if (eolDateStr == null) {
            return false;
        }
        
        try {
            // Parse EOL date (format: "YYYY-MM")
            java.time.YearMonth eolDate = java.time.YearMonth.parse(eolDateStr);
            java.time.YearMonth currentDate = java.time.YearMonth.now();
            return currentDate.isAfter(eolDate);
        } catch (Exception e) {
            log.error("Error parsing EOL date '{}': {}", eolDateStr, e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets the EOL date string for a version if available.
     */
    @Nullable
    private String getEolDateString(@Nonnull String version, @Nonnull DependencyCheckItem checkItem) {
        return checkItem.getEolDateForVersion(version);
    }
    
    /**
     * Determines the upgrade reason based on version and EOL status.
     */
    @Nonnull
    private JavaUpgradeIssue.UpgradeReason determineUpgradeReason(@Nonnull String version, 
                                                                   @Nonnull DependencyCheckItem checkItem) {
        // Check if version has reached EOL based on the EOL date map
        if (isVersionEndOfLife(version, checkItem)) {
            return JavaUpgradeIssue.UpgradeReason.END_OF_LIFE;
        }
        
        // For deprecated but still maintained versions
        return JavaUpgradeIssue.UpgradeReason.DEPRECATED;
    }
    
    /**
     * Determines the severity based on version and EOL status.
     */
    @Nonnull
    private JavaUpgradeIssue.Severity determineSeverity(@Nonnull String version, 
                                                         @Nonnull DependencyCheckItem checkItem) {
        // If version has reached EOL, mark as critical
        if (isVersionEndOfLife(version, checkItem)) {
            return JavaUpgradeIssue.Severity.INFO;
        }
        
        // For other unsupported versions (not yet EOL but outside supported range)
        return JavaUpgradeIssue.Severity.INFO;
    }
    
    /**
     * Builds a human-readable upgrade message.
     */
    @Nonnull
    private String buildUpgradeMessage(@Nonnull String displayName, 
                                        @Nonnull String currentVersion,
                                        @Nonnull DependencyCheckItem checkItem) {
        return String.format(
                ISSUE_DISPLAY_NAME,
                displayName, currentVersion, displayName, checkItem.suggestedVersion
        );
    }
    
    /**
     * Finds a direct dependency in the Maven project (excludes transitive dependencies).
     * Uses getDependencyTree() to identify only dependencies explicitly declared in pom.xml.
     * This aligns with the TypeScript implementation which parses pom.xml directly.
     */
    @Nullable
    private MavenArtifact findDirectDependency(@Nonnull MavenProject mavenProject, 
                                                @Nonnull String groupId, 
                                                @Nullable String artifactId) {
        // getDependencyTree() returns the root-level nodes which are direct dependencies
        // (transitive dependencies are children of these nodes)
        List<MavenArtifactNode> dependencyTree = mavenProject.getDependencyTree();
        return dependencyTree.stream()
            .map(MavenArtifactNode::getArtifact)
            .filter(dep -> groupId.equals(dep.getGroupId()))
            .filter(dep -> artifactId == null || artifactId.equals(dep.getArtifactId()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Checks a single dependency against its metadata for Gradle projects.
     */
    @Nullable
    private JavaUpgradeIssue checkGradleDependency(@Nonnull ExternalProject gradleProject,
                                              @Nonnull DependencyCheckItem checkItem,
                                              @Nonnull Set<String> checkedPackages) {
        final String version = findDirectGradleDependencyVersion(gradleProject, checkItem);

        if (version == null || StringUtils.isBlank(version)) {
            return null;
        }
        
        checkedPackages.add(checkItem.getPackageId());
        
        // Check if version satisfies the supported version range
        if (!satisfiesVersionRange(version, checkItem.supportedVersion)) {
            return JavaUpgradeIssue.builder()
                .packageId(checkItem.getPackageId())
                .packageDisplayName(checkItem.displayName)
                .upgradeReason(determineUpgradeReason(version, checkItem))
                .severity(determineSeverity(version, checkItem))
                .currentVersion(version)
                .supportedVersion(checkItem.supportedVersion)
                .suggestedVersion(checkItem.suggestedVersion)
                .message(buildUpgradeMessage(checkItem.displayName, version, checkItem))
                .learnMoreUrl(checkItem.learnMoreUrl)
                    .eofDate(checkItem.getEolDateForVersion(version))
                .build();
        }
        
        return null;
    }

    /**
     * Gets all direct Gradle dependency locations from the build files of a Gradle project.
     * This is the shared logic used by both dependency issue checking and CVE checking.
     *
     * @param gradleProject The Gradle project to scan
     * @return List of dependency locations found in the build files
     */
    @Nonnull
    private List<GradleBuildFileUtils.GradleDependencyLocation> getDirectGradleDependencyLocations(
            @Nonnull ExternalProject gradleProject) {
        final List<GradleBuildFileUtils.GradleDependencyLocation> allLocations = new ArrayList<>();
        final Path projectDir = gradleProject.getProjectDir().toPath();
        final List<Path> buildFiles = List.of(
            projectDir.resolve("build.gradle"),
            projectDir.resolve("build.gradle.kts")
        );

        for (Path buildFile : buildFiles) {
            if (!Files.exists(buildFile)) {
                continue;
            }

            final String text;
            try {
                text = Files.readString(buildFile, StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.warn("Failed to read Gradle build file: {}", buildFile, e);
                continue;
            }

            allLocations.addAll(GradleBuildFileUtils.findDependencyLocations(text));
        }

        return allLocations;
    }

    /**
     * Gets the Spring Boot plugin version from a Gradle project's build files.
     * Checks multiple sources:
     * 1. Build files (build.gradle, build.gradle.kts)
     * 2. Version catalog (gradle/libs.versions.toml)
     *
     * @param gradleProject The Gradle project to scan
     * @return The Spring Boot plugin version, or null if not found
     */
    @Nullable
    private String getSpringBootPluginVersion(@Nonnull ExternalProject gradleProject) {
        final Path projectDir = gradleProject.getProjectDir().toPath();
        
        // Check build files first
        final List<Path> buildFiles = List.of(
            projectDir.resolve("build.gradle"),
            projectDir.resolve("build.gradle.kts")
        );

        for (Path buildFile : buildFiles) {
            if (!Files.exists(buildFile)) {
                continue;
            }

            final String text;
            try {
                text = Files.readString(buildFile, StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.warn("Failed to read Gradle build file: {}", buildFile, e);
                continue;
            }

            final List<GradleBuildFileUtils.GradleVersionLocation> pluginVersions =
                GradleBuildFileUtils.findSpringBootPluginVersions(text);
            if (!pluginVersions.isEmpty()) {
                return pluginVersions.get(0).version();
            }
        }
        
        // Check version catalog (gradle/libs.versions.toml)
        final Path versionCatalog = projectDir.resolve("gradle").resolve("libs.versions.toml");
        if (Files.exists(versionCatalog)) {
            try {
                final String tomlContent = Files.readString(versionCatalog, StandardCharsets.UTF_8);
                final List<GradleBuildFileUtils.GradleVersionLocation> catalogVersions =
                    GradleBuildFileUtils.findSpringBootVersionsInCatalog(tomlContent);
                if (!catalogVersions.isEmpty()) {
                    return catalogVersions.get(0).version();
                }
            } catch (IOException e) {
                log.warn("Failed to read version catalog: {}", versionCatalog, e);
            }
        }

        return null;
    }

    @Nullable
    private String findDirectGradleDependencyVersion(@Nonnull ExternalProject gradleProject,
                                                     @Nonnull DependencyCheckItem checkItem) {
        final List<GradleBuildFileUtils.GradleDependencyLocation> locations = 
            getDirectGradleDependencyLocations(gradleProject);

        for (GradleBuildFileUtils.GradleDependencyLocation location : locations) {
            final boolean groupMatches = StringUtils.equalsIgnoreCase(checkItem.groupId, location.groupId());
            final boolean artifactMatches = "*".equals(checkItem.artifactId) ||
                StringUtils.equalsIgnoreCase(checkItem.artifactId, location.artifactId());
            if (groupMatches && artifactMatches) {
                return location.version();
            }
        }

        // Check for Spring Boot plugin version
        if (GROUP_ID_SPRING_BOOT.equals(checkItem.groupId)) {
            return getSpringBootPluginVersion(gradleProject);
        }

        return null;
    }

    private void collectDirectGradleDependencies(@Nonnull ExternalProject gradleProject, @Nonnull Set<String> coordinateSet) {
        getDirectGradleDependencyLocations(gradleProject).stream()
            .filter(location -> StringUtils.isNotBlank(location.version()))
            .forEach(location -> coordinateSet.add(
                location.groupId() + ":" + location.artifactId() + ":" + location.version()
            ));
    }
}
