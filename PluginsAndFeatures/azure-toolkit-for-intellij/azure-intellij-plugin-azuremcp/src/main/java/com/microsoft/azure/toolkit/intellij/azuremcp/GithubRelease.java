package com.microsoft.azure.toolkit.intellij.azuremcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class GithubRelease {
    @JsonProperty("url")
    private String url;
    @JsonProperty("html_url")
    private String htmlUrl;
    @JsonProperty("assets_url")
    private String assetsUrl;
    @JsonProperty("upload_url")
    private String uploadUrl;
    @JsonProperty("tarball_url")
    private String tarballUrl;
    @JsonProperty("zipball_url")
    private String zipballUrl;
    @JsonProperty("id")
    private long id;
    @JsonProperty("node_id")
    private String nodeId;
    @JsonProperty("tag_name")
    private String tagName;
    @JsonProperty("target_commitish")
    private String targetCommitish;
    @JsonProperty("name")
    private String name;
    @JsonProperty("body")
    private String body;
    @JsonProperty("draft")
    private boolean draft;
    @JsonProperty("prerelease")
    private boolean prerelease;
    @JsonProperty("immutable")
    private boolean immutable;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("published_at")
    private String publishedAt;
    @JsonProperty("author")
    private GithubUser author;
    @JsonProperty("assets")
    private List<GithubAsset> assets;

}
