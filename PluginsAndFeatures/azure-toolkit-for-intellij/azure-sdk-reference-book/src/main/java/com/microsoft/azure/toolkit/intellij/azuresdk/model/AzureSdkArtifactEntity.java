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

    public String generateMavenDependencySnippet(String version) {
        return String.join("",
                "<dependency>\n",
                "    <groupId>", this.groupId, "</groupId>\n",
                "    <artifactId>", this.artifactId, "</artifactId>\n",
                "    <version>", version, "</version>\n",
                "</dependency>"
        );
    }

    public String getLink(String type) {
        return this.links.get(type);
    }

    public static class Type {
        public static final String SPRING = "spring";
        public static final String CLIENT = "client";
        public static final String MANAGEMENT = "mgmt";
    }
}
