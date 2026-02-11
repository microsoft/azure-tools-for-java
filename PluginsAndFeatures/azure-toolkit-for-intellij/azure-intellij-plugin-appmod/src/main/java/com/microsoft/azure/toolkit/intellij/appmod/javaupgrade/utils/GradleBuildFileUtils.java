/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils;

import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing and extracting information from Gradle build files.
 */
@SuppressWarnings({"unused", "MismatchedQueryAndUpdateOfCollection", "UnnecessaryTemporaryOnConversionFromString"})
public final class GradleBuildFileUtils {

    // Pattern for dependencies with explicit version: 'groupId:artifactId:version'
    private static final Pattern DEPENDENCY_COORDINATE_PATTERN = Pattern.compile(
        "(['\"])([^'\"\\s:]+):([^'\"\\s:]+):([^'\"]+)\\1"
    );

    private static final Pattern MAP_GROUP_PATTERN = Pattern.compile("group\\s*[:=]\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern MAP_NAME_PATTERN = Pattern.compile("name\\s*[:=]\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern MAP_VERSION_PATTERN = Pattern.compile("version\\s*[:=]\\s*['\"]?([^'\"\\s)]+)['\"]?");

    private static final Pattern EXT_BLOCK_ASSIGNMENT_PATTERN = Pattern.compile("\\b(\\w+)\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern EXT_DOT_ASSIGNMENT_PATTERN = Pattern.compile("\\bext\\.(\\w+)\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern EXTRA_MAP_ASSIGNMENT_PATTERN = Pattern.compile("extra\\s*\\[\\s*['\"]([^'\"]+)['\"]\\s*]\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern SIMPLE_VAR_ASSIGNMENT_PATTERN = Pattern.compile("\\b(?:def|val|var)\\s+(\\w+)\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern VARIABLE_REFERENCE_PATTERN = Pattern.compile("\\$\\{(\\w+)}|\\$(\\w+)");

    // === Spring Boot Plugin Patterns ===
    
    // Pattern 1: Modern plugins DSL (Groovy): id 'org.springframework.boot' version '2.7.3'
    private static final Pattern SPRING_BOOT_PLUGIN_VERSION_PATTERN = Pattern.compile(
        "id\\s*\\(?\\s*['\"]org\\.springframework\\.boot['\"]\\s*\\)?\\s*version\\s*['\"]([^'\"]+)['\"]"
    );

    // Pattern 2: Kotlin DSL plugins: id("org.springframework.boot") version "2.7.3"
    private static final Pattern SPRING_BOOT_PLUGIN_KOTLIN_PATTERN = Pattern.compile(
        "id\\s*\\(\\s*['\"]org\\.springframework\\.boot['\"]\\s*\\)\\s*version\\s*['\"]([^'\"]+)['\"]"
    );

    // Pattern 3: Buildscript classpath: classpath "org.springframework.boot:spring-boot-gradle-plugin:VERSION"
    private static final Pattern SPRING_BOOT_CLASSPATH_PATTERN = Pattern.compile(
        "classpath\\s*\\(?\\s*['\"]org\\.springframework\\.boot:spring-boot-gradle-plugin:([^'\"]+)['\"]\\s*\\)?"
    );

    // Pattern 4: Platform/BOM: platform('org.springframework.boot:spring-boot-dependencies:2.7.3')
    private static final Pattern SPRING_BOOT_PLATFORM_PATTERN = Pattern.compile(
        "platform\\s*\\(\\s*['\"]org\\.springframework\\.boot:spring-boot-dependencies:([^'\"]+)['\"]\\s*\\)"
    );

    // Pattern 5: dependencyManagement mavenBom: mavenBom "org.springframework.boot:spring-boot-dependencies:2.7.3"
    private static final Pattern SPRING_BOOT_MAVEN_BOM_PATTERN = Pattern.compile(
        "mavenBom\\s*\\(?\\s*['\"]org\\.springframework\\.boot:spring-boot-dependencies:([^'\"]+)['\"]\\s*\\)?"
    );

    private static final Pattern JAVA_VERSION_TOKEN_PATTERN = Pattern.compile(
        "JavaVersion\\.VERSION_(\\d+)(?:_(\\d+))?"
    );

    private static final Pattern JAVA_SOURCE_TARGET_PATTERN = Pattern.compile(
        "(?:sourceCompatibility|targetCompatibility)\\s*=\\s*['\"]?(\\d+(?:\\.\\d+)?)['\"]?"
    );

    private static final Pattern JAVA_TOOLCHAIN_PATTERN = Pattern.compile(
        "JavaLanguageVersion\\.of\\((\\d+)\\)"
    );

    private GradleBuildFileUtils() {
        // Utility class, no instantiation
    }

    public static boolean isGradleBuildFile(@NotNull PsiFile file) {
        final String name = file.getName();
        return name.endsWith(".gradle") || name.endsWith(".gradle.kts");
    }

    /**
     * Checks if the file is a Gradle Version Catalog file (libs.versions.toml).
     */
    public static boolean isVersionCatalogFile(@NotNull PsiFile file) {
        final String name = file.getName();
        return name.equals("libs.versions.toml");
    }

    @NotNull
    public static List<GradleDependencyLocation> findDependencyLocations(@NotNull String text) {
        if (text.isEmpty()) {
            return Collections.emptyList();
        }

        final List<GradleDependencyLocation> locations = new ArrayList<>();
        final Map<String, String> variables = extractVariables(text);
        
        // Find dependencies with explicit version: 'groupId:artifactId:version'
        final Matcher matcher = DEPENDENCY_COORDINATE_PATTERN.matcher(text);
        while (matcher.find()) {
            // Skip if this match is inside a comment
            if (isInsideComment(text, matcher.start())) {
                continue;
            }
            final String groupId = matcher.group(2);
            final String artifactId = matcher.group(3);
            final String version = resolveVersion(matcher.group(4), variables);
            locations.add(new GradleDependencyLocation(groupId, artifactId, version, matcher.start(2)));
        }

        // Also check for map-style dependencies: group: 'x', name: 'y', version: 'z'
        final String[] lines = text.split("\\r?\\n");
        int offset = 0;
        for (String line : lines) {
            // Skip commented lines
            final String trimmedLine = line.trim();
            if (trimmedLine.startsWith("//") || trimmedLine.startsWith("*") || trimmedLine.startsWith("/*")) {
                offset += line.length() + 1;
                continue;
            }
            
            final Matcher groupMatcher = MAP_GROUP_PATTERN.matcher(line);
            final Matcher nameMatcher = MAP_NAME_PATTERN.matcher(line);
            final Matcher versionMatcher = MAP_VERSION_PATTERN.matcher(line);

            if (groupMatcher.find() && nameMatcher.find() && versionMatcher.find()) {
                final String groupId = groupMatcher.group(1);
                final String artifactId = nameMatcher.group(1);
                final String rawVersion = versionMatcher.group(1);
                final String version = resolveVersion(rawVersion, variables);
                locations.add(new GradleDependencyLocation(groupId, artifactId, version, offset + groupMatcher.start(1)));
            }

            offset += line.length() + 1;
        }
        return locations;
    }

    @NotNull
    public static List<GradleVersionLocation> findSpringBootPluginVersions(@NotNull String text) {
        if (text.isEmpty()) {
            return Collections.emptyList();
        }

        final List<GradleVersionLocation> locations = new ArrayList<>();
        final Map<String, String> variables = extractVariables(text);
        
        // Pattern 1: Modern plugins DSL (Groovy): id 'org.springframework.boot' version 'x.y.z'
        final Matcher pluginMatcher = SPRING_BOOT_PLUGIN_VERSION_PATTERN.matcher(text);
        while (pluginMatcher.find()) {
            if (isInsideComment(text, pluginMatcher.start())) {
                continue;
            }
            final String version = resolveVersion(pluginMatcher.group(1), variables);
            locations.add(new GradleVersionLocation(version, pluginMatcher.start(1), pluginMatcher.end(1)));
        }
        
        // Pattern 2: Kotlin DSL plugins: id("org.springframework.boot") version "x.y.z"
        final Matcher kotlinPluginMatcher = SPRING_BOOT_PLUGIN_KOTLIN_PATTERN.matcher(text);
        while (kotlinPluginMatcher.find()) {
            if (isInsideComment(text, kotlinPluginMatcher.start())) {
                continue;
            }
            final String version = resolveVersion(kotlinPluginMatcher.group(1), variables);
            locations.add(new GradleVersionLocation(version, kotlinPluginMatcher.start(1), kotlinPluginMatcher.end(1)));
        }
        
        // Pattern 3: Buildscript classpath: classpath "org.springframework.boot:spring-boot-gradle-plugin:x.y.z"
        final Matcher classpathMatcher = SPRING_BOOT_CLASSPATH_PATTERN.matcher(text);
        while (classpathMatcher.find()) {
            if (isInsideComment(text, classpathMatcher.start())) {
                continue;
            }
            final String version = resolveVersion(classpathMatcher.group(1), variables);
            locations.add(new GradleVersionLocation(version, classpathMatcher.start(1), classpathMatcher.end(1)));
        }
        
        // Pattern 4: Platform/BOM: platform('org.springframework.boot:spring-boot-dependencies:x.y.z')
        final Matcher platformMatcher = SPRING_BOOT_PLATFORM_PATTERN.matcher(text);
        while (platformMatcher.find()) {
            if (isInsideComment(text, platformMatcher.start())) {
                continue;
            }
            final String version = resolveVersion(platformMatcher.group(1), variables);
            locations.add(new GradleVersionLocation(version, platformMatcher.start(1), platformMatcher.end(1)));
        }
        
        // Pattern 5: dependencyManagement mavenBom: mavenBom "org.springframework.boot:spring-boot-dependencies:x.y.z"
        final Matcher bomMatcher = SPRING_BOOT_MAVEN_BOM_PATTERN.matcher(text);
        while (bomMatcher.find()) {
            if (isInsideComment(text, bomMatcher.start())) {
                continue;
            }
            final String version = resolveVersion(bomMatcher.group(1), variables);
            locations.add(new GradleVersionLocation(version, bomMatcher.start(1), bomMatcher.end(1)));
        }
        
        return locations;
    }

    @NotNull
    public static List<GradleVersionLocation> findJavaVersionLocations(@NotNull String text) {
        if (text.isEmpty()) {
            return Collections.emptyList();
        }

        final List<GradleVersionLocation> locations = new ArrayList<>();

        final Matcher javaVersionMatcher = JAVA_VERSION_TOKEN_PATTERN.matcher(text);
        while (javaVersionMatcher.find()) {
            if (isInsideComment(text, javaVersionMatcher.start())) {
                continue;
            }
            final String major = javaVersionMatcher.group(1);
            final String minor = javaVersionMatcher.group(2);
            final Integer version = parseJavaVersionToken(major, minor);
            if (version != null) {
                final int start = javaVersionMatcher.start(1);
                final int end = minor != null ? javaVersionMatcher.end(2) : javaVersionMatcher.end(1);
                locations.add(new GradleVersionLocation(String.valueOf(version), start, end));
            }
        }

        final Matcher sourceTargetMatcher = JAVA_SOURCE_TARGET_PATTERN.matcher(text);
        while (sourceTargetMatcher.find()) {
            if (isInsideComment(text, sourceTargetMatcher.start())) {
                continue;
            }
            final String rawVersion = sourceTargetMatcher.group(1);
            final Integer version = parseSimpleVersion(rawVersion);
            if (version != null) {
                locations.add(new GradleVersionLocation(String.valueOf(version), sourceTargetMatcher.start(1), sourceTargetMatcher.end(1)));
            }
        }

        final Matcher toolchainMatcher = JAVA_TOOLCHAIN_PATTERN.matcher(text);
        while (toolchainMatcher.find()) {
            if (isInsideComment(text, toolchainMatcher.start())) {
                continue;
            }
            locations.add(new GradleVersionLocation(toolchainMatcher.group(1), toolchainMatcher.start(1), toolchainMatcher.end(1)));
        }

        return locations;
    }

    @NotNull
    public static DependencyCoordinate findDependencyAtOffset(@NotNull String text, int offset) {
        final int start = Math.max(0, text.lastIndexOf('\n', offset - 1));
        final int end = Math.min(text.length(), text.indexOf('\n', offset));
        final int lineStart = start == -1 ? 0 : start + 1;
        final int lineEnd = end == -1 ? text.length() : end;
        final String line = text.substring(lineStart, lineEnd);

        final Matcher matcher = DEPENDENCY_COORDINATE_PATTERN.matcher(line);
        while (matcher.find()) {
            final int absoluteStart = lineStart + matcher.start(2);
            final int absoluteEnd = lineStart + matcher.end(4);
            if (offset >= absoluteStart && offset <= absoluteEnd) {
                return new DependencyCoordinate(matcher.group(2), matcher.group(3), matcher.group(4));
            }
        }
        return DependencyCoordinate.EMPTY;
    }

    private static Integer parseJavaVersionToken(@NotNull String major, String minor) {
        try {
            if ("1".equals(major) && minor != null) {
                return Integer.parseInt(minor);
            }
            return Integer.parseInt(major);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Integer parseSimpleVersion(@NotNull String version) {
        try {
            if (version.startsWith("1.")) {
                return Integer.parseInt(version.substring(2));
            }
            return Integer.parseInt(version);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * Checks if a position in the text is inside a comment (single-line // or multi-line \/* *\/).
     * 
     * @param text The full text content
     * @param position The character position to check
     * @return true if the position is inside a comment
     */
    private static boolean isInsideComment(@NotNull String text, int position) {
        // Find the start of the line containing this position
        int lineStart = text.lastIndexOf('\n', position - 1) + 1;
        String linePrefix = text.substring(lineStart, position);
        
        // Check for single-line comment (//)
        int singleLineComment = linePrefix.indexOf("//");
        if (singleLineComment >= 0) {
            return true;
        }
        
        // Check for multi-line comment (/* ... */)
        // Count /* and */ before the position
        int blockCommentDepth = 0;
        for (int i = 0; i < position - 1; i++) {
            if (text.charAt(i) == '/' && text.charAt(i + 1) == '*') {
                blockCommentDepth++;
                i++; // Skip the next character
            } else if (text.charAt(i) == '*' && text.charAt(i + 1) == '/') {
                blockCommentDepth--;
                i++; // Skip the next character
            }
        }
        
        return blockCommentDepth > 0;
    }

    @NotNull
    private static Map<String, String> extractVariables(@NotNull String text) {
        final Map<String, String> variables = new HashMap<>();

        final Matcher extBlockMatcher = Pattern.compile("ext\\s*\\{([\\s\\S]*?)}").matcher(text);
        while (extBlockMatcher.find()) {
            final String block = extBlockMatcher.group(1);
            final Matcher assignmentMatcher = EXT_BLOCK_ASSIGNMENT_PATTERN.matcher(block);
            while (assignmentMatcher.find()) {
                variables.putIfAbsent(assignmentMatcher.group(1), assignmentMatcher.group(2));
            }
        }

        final Matcher extDotMatcher = EXT_DOT_ASSIGNMENT_PATTERN.matcher(text);
        while (extDotMatcher.find()) {
            variables.putIfAbsent(extDotMatcher.group(1), extDotMatcher.group(2));
        }

        final Matcher extraMapMatcher = EXTRA_MAP_ASSIGNMENT_PATTERN.matcher(text);
        while (extraMapMatcher.find()) {
            variables.putIfAbsent(extraMapMatcher.group(1), extraMapMatcher.group(2));
        }

        final Matcher simpleVarMatcher = SIMPLE_VAR_ASSIGNMENT_PATTERN.matcher(text);
        while (simpleVarMatcher.find()) {
            variables.putIfAbsent(simpleVarMatcher.group(1), simpleVarMatcher.group(2));
        }

        return variables;
    }

    @NotNull
    private static String resolveVersion(@NotNull String rawVersion, @NotNull Map<String, String> variables) {
        final String trimmed = rawVersion.trim();
        if (variables.containsKey(trimmed)) {
            return variables.get(trimmed);
        }

        final Matcher matcher = VARIABLE_REFERENCE_PATTERN.matcher(trimmed);
        final StringBuffer resolved = new StringBuffer();
        boolean replaced = false;
        while (matcher.find()) {
            final String key = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            final String value = variables.get(key);
            if (value != null) {
                matcher.appendReplacement(resolved, Matcher.quoteReplacement(value));
                replaced = true;
            }
        }
        matcher.appendTail(resolved);

        return replaced ? resolved.toString() : trimmed;
    }

    /**
     * Parses a Gradle Version Catalog file (libs.versions.toml) to find Spring Boot version.
     * The version catalog format is:
     * <pre>
     * [versions]
     * spring-boot = "2.7.3"
     * 
     * [plugins]
     * spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
     * </pre>
     * 
     * @param tomlContent The content of the libs.versions.toml file
     * @return List of Spring Boot version locations found
     */
    @NotNull
    public static List<GradleVersionLocation> findSpringBootVersionsInCatalog(@NotNull String tomlContent) {
        if (tomlContent.isEmpty()) {
            return Collections.emptyList();
        }

        final List<GradleVersionLocation> locations = new ArrayList<>();
        final Map<String, VersionLocation> versionRefs = new HashMap<>();
        
        // First pass: extract all version definitions from [versions] section
        // Pattern: spring-boot = "2.7.3" or springBoot = "2.7.3"
        final Pattern versionDefPattern = Pattern.compile(
            "^\\s*([\\w-]+)\\s*=\\s*['\"]([^'\"]+)['\"]\\s*$",
            Pattern.MULTILINE
        );
        final Matcher versionMatcher = versionDefPattern.matcher(tomlContent);
        while (versionMatcher.find()) {
            final String key = versionMatcher.group(1);
            final String version = versionMatcher.group(2);
            versionRefs.put(key, new VersionLocation(version, versionMatcher.start(2), versionMatcher.end(2)));
        }
        
        // Second pass: find Spring Boot plugin references
        // Pattern: spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
        // or: spring-boot = { id = "org.springframework.boot", version = "2.7.3" }
        final Pattern pluginPattern = Pattern.compile(
            "([\\w-]+)\\s*=\\s*\\{[^}]*id\\s*=\\s*['\"]org\\.springframework\\.boot['\"][^}]*\\}",
            Pattern.MULTILINE
        );
        final Matcher pluginMatcher = pluginPattern.matcher(tomlContent);
        while (pluginMatcher.find()) {
            final String pluginBlock = pluginMatcher.group(0);
            
            // Check for version.ref = "xxx"
            final Pattern versionRefPattern = Pattern.compile("version\\.ref\\s*=\\s*['\"]([^'\"]+)['\"]");
            final Matcher refMatcher = versionRefPattern.matcher(pluginBlock);
            if (refMatcher.find()) {
                final String ref = refMatcher.group(1);
                final VersionLocation versionLoc = versionRefs.get(ref);
                if (versionLoc != null) {
                    locations.add(new GradleVersionLocation(versionLoc.version, versionLoc.startOffset, versionLoc.endOffset));
                }
            }
            
            // Check for inline version = "xxx"
            final Pattern inlineVersionPattern = Pattern.compile("(?<!\\.)version\\s*=\\s*['\"]([^'\"]+)['\"]");
            final Matcher inlineMatcher = inlineVersionPattern.matcher(pluginBlock);
            if (inlineMatcher.find()) {
                final int absoluteStart = pluginMatcher.start() + inlineMatcher.start(1);
                final int absoluteEnd = pluginMatcher.start() + inlineMatcher.end(1);
                locations.add(new GradleVersionLocation(inlineMatcher.group(1), absoluteStart, absoluteEnd));
            }
        }
        
        // Also check for spring-boot-dependencies in [libraries] section
        // Pattern: spring-boot-bom = { group = "org.springframework.boot", name = "spring-boot-dependencies", version.ref = "spring-boot" }
        final Pattern libraryPattern = Pattern.compile(
            "([\\w-]+)\\s*=\\s*\\{[^}]*['\"]org\\.springframework\\.boot['\"][^}]*['\"]spring-boot-dependencies['\"][^}]*\\}",
            Pattern.MULTILINE
        );
        final Matcher libraryMatcher = libraryPattern.matcher(tomlContent);
        while (libraryMatcher.find()) {
            final String libraryBlock = libraryMatcher.group(0);
            
            final Pattern versionRefPattern = Pattern.compile("version\\.ref\\s*=\\s*['\"]([^'\"]+)['\"]");
            final Matcher refMatcher = versionRefPattern.matcher(libraryBlock);
            if (refMatcher.find()) {
                final String ref = refMatcher.group(1);
                final VersionLocation versionLoc = versionRefs.get(ref);
                if (versionLoc != null && !containsVersion(locations, versionLoc.version)) {
                    locations.add(new GradleVersionLocation(versionLoc.version, versionLoc.startOffset, versionLoc.endOffset));
                }
            }
        }
        
        return locations;
    }
    
    private static boolean containsVersion(List<GradleVersionLocation> locations, String version) {
        return locations.stream().anyMatch(loc -> loc.version().equals(version));
    }
    
    private record VersionLocation(String version, int startOffset, int endOffset) {}

    public record GradleDependencyLocation(@NotNull String groupId, @NotNull String artifactId, @NotNull String version, int startOffset) {
    }

    public record GradleVersionLocation(@NotNull String version, int startOffset, int endOffset) {
    }

    public record DependencyCoordinate(@NotNull String groupId, @NotNull String artifactId, @NotNull String version) {
        public static final DependencyCoordinate EMPTY = new DependencyCoordinate("", "", "");

        public boolean isValid() {
            return !groupId.isBlank() && !artifactId.isBlank();
        }

        public String getPackageId() {
            return groupId + ":" + artifactId;
        }
    }
}
