package com.microsoft.azure.toolkit.intellij.samples.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchResult(
    @JsonProperty(value = "total_count")
    int count,
    @JsonProperty(value = "incomplete_results")
    boolean incomplete,
    List<GithubRepository> items
) {
}
