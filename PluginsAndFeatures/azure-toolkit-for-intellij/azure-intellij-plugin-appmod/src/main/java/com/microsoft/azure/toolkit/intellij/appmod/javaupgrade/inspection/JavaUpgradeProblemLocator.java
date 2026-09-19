/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.inspection;

import com.intellij.psi.xml.XmlTag;
import com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.dao.JavaUpgradeIssue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesDetectionService.JDK_DISPLAY_NAME;
import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesDetectionService.JDK_LEARN_MORE_URL;
import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesDetectionService.MATURE_JAVA_LTS_VERSION;
import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.service.JavaUpgradeIssuesDetectionService.PACKAGE_ID_JDK;
import static com.microsoft.azure.toolkit.intellij.appmod.javaupgrade.utils.Constants.ISSUE_DISPLAY_NAME;

final class JavaUpgradeProblemLocator {

    private JavaUpgradeProblemLocator() {
    }

    static JavaUpgradeIssue createJavaVersionIssue(@NotNull XmlTag versionTag) {
        return createJavaVersionIssue(versionTag, new Properties());
    }

    static JavaUpgradeIssue createJavaVersionIssue(
        @NotNull XmlTag versionTag,
        @NotNull Properties effectiveProperties
    ) {
        return createJavaVersionIssue(
            resolveVersionText(versionTag, effectiveProperties, new HashSet<>()));
    }

    static JavaUpgradeIssue createJavaVersionIssue(@Nullable String versionText) {
        final Integer version = parseJavaVersion(versionText);
        if (version == null || version < 8 || version >= MATURE_JAVA_LTS_VERSION) {
            return null;
        }
        return JavaUpgradeIssue.builder()
            .packageId(PACKAGE_ID_JDK)
            .packageDisplayName(JDK_DISPLAY_NAME)
            .upgradeReason(JavaUpgradeIssue.UpgradeReason.JRE_TOO_OLD)
            .severity(JavaUpgradeIssue.Severity.WARNING)
            .currentVersion(String.valueOf(version))
            .supportedVersion(">=" + MATURE_JAVA_LTS_VERSION)
            .suggestedVersion(String.valueOf(MATURE_JAVA_LTS_VERSION))
            .message(String.format(
                ISSUE_DISPLAY_NAME,
                JDK_DISPLAY_NAME,
                version,
                JDK_DISPLAY_NAME,
                MATURE_JAVA_LTS_VERSION
            ))
            .learnMoreUrl(JDK_LEARN_MORE_URL)
            .build();
    }

    @Nullable
    private static String resolveVersionText(
        @NotNull XmlTag versionTag,
        @NotNull Properties effectiveProperties,
        @NotNull Set<String> resolvingProperties
    ) {
        final String versionText = versionTag.getValue().getText().trim();
        if (!versionText.startsWith("${") || !versionText.endsWith("}")) {
            return versionText;
        }

        final String propertyName = versionText.substring(2, versionText.length() - 1);
        if (!resolvingProperties.add(propertyName)) {
            return null;
        }

        XmlTag current = versionTag;
        while (current != null) {
            final XmlTag properties = "properties".equals(current.getName())
                ? current
                : current.findFirstSubTag("properties");
            if (properties != null) {
                final XmlTag property = properties.findFirstSubTag(propertyName);
                if (property != null) {
                    return resolveVersionText(property, effectiveProperties, resolvingProperties);
                }
            }
            current = current.getParentTag();
        }
        return resolvePropertyValue(
            effectiveProperties.getProperty(propertyName),
            effectiveProperties,
            resolvingProperties
        );
    }

    @Nullable
    private static String resolvePropertyValue(
        @Nullable String value,
        @NotNull Properties effectiveProperties,
        @NotNull Set<String> resolvingProperties
    ) {
        if (value == null) {
            return null;
        }
        final String normalized = value.trim();
        if (!normalized.startsWith("${") || !normalized.endsWith("}")) {
            return normalized;
        }
        final String propertyName = normalized.substring(2, normalized.length() - 1);
        if (!resolvingProperties.add(propertyName)) {
            return null;
        }
        return resolvePropertyValue(
            effectiveProperties.getProperty(propertyName),
            effectiveProperties,
            resolvingProperties
        );
    }

    @Nullable
    private static Integer parseJavaVersion(@Nullable String versionText) {
        if (versionText == null) {
            return null;
        }
        final String normalized = versionText.trim();
        if (normalized.isEmpty() || normalized.startsWith("${")) {
            return null;
        }
        final String majorVersion = normalized.startsWith("1.")
            ? normalized.substring(2)
            : normalized;
        final int separator = majorVersion.indexOf('.');
        final String major = separator < 0 ? majorVersion : majorVersion.substring(0, separator);
        try {
            return Integer.parseInt(major);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
