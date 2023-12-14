/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.samples.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubRepository {
    String name;
    @JsonProperty(value = "full_name")
    String fullName;
    String description;
    @JsonProperty(value = "html_url")
    String htmlUrl;
    @JsonProperty(value = "git_url")
    String gitUrl;
    @JsonProperty(value = "ssh_url")
    String sshUrl;
    @JsonProperty(value = "clone_url")
    String cloneUrl;
    @JsonProperty(value = "stargazers_count")
    int stars;
    License licence;
    List<String> topics;
    String visibility;
    boolean archived;
    boolean disabled;

    public void cloneRepo() {
    }

    public void openInGithub() {

    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record License(
    String key,
    String name,
    String url
) {
}
