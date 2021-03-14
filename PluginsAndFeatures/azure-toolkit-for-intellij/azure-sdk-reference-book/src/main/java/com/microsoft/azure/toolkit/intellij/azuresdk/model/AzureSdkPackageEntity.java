/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@Getter
@Builder
@ToString(of = "artifact", includeFieldNames = false)
public class AzureSdkPackageEntity {
    private final String service;
    private final String feature;
    private final String group;
    private final String artifact;
    private final String type;
    private final String versionGA;
    private final String versionPreview;
    private final String mavenPath;
    private final Map<String, String> links;

    public String generateMavenDependencySnippet(String version) {
        return String.join("", "",
            "<dependency>\n",
            "    <groupId>", this.group, "</groupId>\n",
            "    <artifactId>", this.artifact, "</artifactId>\n",
            "    <version>", version, "</version>\n",
            "</dependency>"
        );
    }
}
