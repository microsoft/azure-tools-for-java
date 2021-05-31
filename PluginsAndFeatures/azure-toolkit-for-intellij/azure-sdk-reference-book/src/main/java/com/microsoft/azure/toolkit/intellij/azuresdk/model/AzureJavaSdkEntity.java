/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@NoArgsConstructor
public class AzureJavaSdkEntity {
    @JsonProperty("Package")
    private String artifactId;
    @JsonProperty("GroupId")
    private String groupId;
    @JsonProperty("VersionGA")
    private String versionGA;
    @JsonProperty("VersionPreview")
    private String versionPreview;
    @JsonProperty("DisplayName")
    private String displayName;
    @JsonProperty("ServiceName")
    private String serviceName;
    @JsonProperty("RepoPath")
    private String repoPath;
    @JsonProperty("MSDocs")
    private String msDocs;
    @JsonProperty("GHDocs")
    private String ghDocs;
    @JsonProperty("Type")
    private String type;
    @JsonProperty("New")
    private Boolean isNew; // New
    @JsonProperty("PlannedVersions")
    private String plannedVersions;
    @JsonProperty(value = "Hide")
    private Boolean isHide;
    @JsonProperty("Notes")
    private String notes;
    @JsonProperty("Support")
    private String support;
    @JsonProperty("Replace")
    private String replace;

    /**
     * {% assign package_url_template = "https://search.maven.org/artifact/item.GroupId/item.Package/item.Version/jar/" %}
     * repopath: https://search.maven.org/artifact/com.azure/azure-security-keyvault-jca
     */
    public String getMavenArtifactUrl() {
        return String.format("https://search.maven.org/artifact/%s/%s/", this.getGroupId(), this.getArtifactId());
    }

    /**
     * {% assign msdocs_url_template =  "https://docs.microsoft.com/java/api/overview/azure/item.TrimmedPackage-readme" %}
     * msdocs: https://docs.microsoft.com/java/api/overview/azure/security-keyvault-jca-readme?view=azure-java-preview
     */
    public String getMsdocsUrl() {
        final String url = this.getMsDocs();
        if ("NA".equals(url)) {
            return "";
        } else if (url.startsWith("http")) {
            return url;
        }
        final String trimmed = this.getArtifactId().replace("azure-", "");
        return String.format("https://docs.microsoft.com/java/api/overview/azure/%s-readme", trimmed);
    }

    /**
     * {% assign ghdocs_url_template = "https://azuresdkdocs.blob.core.windows.net/$web/java/item.Package/item.Version/index.html" %}
     * javadoc: https://azuresdkdocs.blob.core.windows.net/$web/java/azure-security-keyvault-jca/1.0.0-beta.4/index.html
     */
    public String getJavadocUrl() {
        final String url = this.getGhDocs();
        if ("NA".equals(url)) {
            return "";
        } else if (url.startsWith("http")) {
            return url;
        }
        return String.format("https://azuresdkdocs.blob.core.windows.net/$web/java/%s/${azure.version}/index.html", this.getArtifactId());
    }

    /**
     * {% assign source_url_template = "https://github.com/Azure/azure-sdk-for-java/tree/item.Package_item.Version/sdk/item.RepoPath/item.Package/" %}
     * github: https://github.com/Azure/azure-sdk-for-java/tree/master/sdk/keyvault/azure-security-keyvault-jca
     */
    public String getGitHubSourceUrl() {
        final String url = this.getRepoPath();
        if ("NA".equals(url)) {
            return "";
        } else if (url.startsWith("http")) {
            return url;
        }
        return String.format("https://github.com/Azure/azure-sdk-for-java/tree/%s_${azure.version}/sdk/%s/%s/", this.getArtifactId(), url,
                this.getArtifactId());
    }

    public String getPackageName() {
        return String.format("%s/%s", groupId, artifactId);
    }
}