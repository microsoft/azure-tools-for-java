/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureSdkArtifactEntity {
    private String artifactId;
    private String groupId;
    private String versionGA;
    private String versionPreview;
    private String type;
    private Map<String, String> links;
    private Map<String, String> dependencyPattern;

    public String getDependencySnippet(DependencyType type, String version) {
        final String strType = type.getName().toLowerCase();
        if (Objects.nonNull(dependencyPattern) && dependencyPattern.containsKey(strType)) {
            return dependencyPattern.get(strType).replace("${azure.version}", version);
        }
        return getDefaultDependencySnippet(type, version);
    }

    private String getDefaultDependencySnippet(final DependencyType type, final String version) {
        if (DependencyType.GRADLE == type) {
            return String.format("implementation '%s:%s:%s'", this.groupId, this.artifactId, version);
        }
        return String.join("",
                "<dependency>\n",
                "    <groupId>", this.groupId, "</groupId>\n",
                "    <artifactId>", this.artifactId, "</artifactId>\n",
                "    <version>", version, "</version>\n",
                "</dependency>"
        );
    }

    public static class Type {
        public static final String SPRING = "spring";
        public static final String CLIENT = "client";
        public static final String MANAGEMENT = "mgmt";
    }

    @Getter
    public enum DependencyType {
        MAVEN("Maven", "xml"), GRADLE("Gradle", "gradle");

        private final String name;
        private final String fileExt;

        DependencyType(final String name, final String fileExt) {
            this.name = name;
            this.fileExt = fileExt;
        }
    }
}
