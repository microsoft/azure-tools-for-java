package com.microsoft.azuretools.azuremcp;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

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
    
    
    // Getters and Setters
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getHtmlUrl() {
        return htmlUrl;
    }
    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }
    public String getAssetsUrl() {
        return assetsUrl;
    }
    public void setAssetsUrl(String assetsUrl) {
        this.assetsUrl = assetsUrl;
    }
    public String getUploadUrl() {
        return uploadUrl;
    }
    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }
    public String getTarballUrl() {
        return tarballUrl;
    }
    public void setTarballUrl(String tarballUrl) {
        this.tarballUrl = tarballUrl;
    }
    public String getZipballUrl() {
        return zipballUrl;
    }
    public void setZipballUrl(String zipballUrl) {
        this.zipballUrl = zipballUrl;
    }
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getNodeId() {
        return nodeId;
    }
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
    public String getTagName() {
        return tagName;
    }
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
    public String getTargetCommitish() {
        return targetCommitish;
    }
    public void setTargetCommitish(String targetCommitish) {
        this.targetCommitish = targetCommitish;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }
    public boolean isDraft() {
        return draft;
    }
    public void setDraft(boolean draft) {
        this.draft = draft;
    }
    public boolean isPrerelease() {
        return prerelease;
    }
    public void setPrerelease(boolean prerelease) {
        this.prerelease = prerelease;
    }
    public boolean isImmutable() {
        return immutable;
    }
    public void setImmutable(boolean immutable) {
        this.immutable = immutable;
    }
    public String getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    public String getPublishedAt() {
        return publishedAt;
    }
    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }
    public GithubUser getAuthor() {
        return author;
    }
    public void setAuthor(GithubUser author) {
        this.author = author;
    }
    public List<GithubAsset> getAssets() {
        return assets;
    }
    public void setAssets(List<GithubAsset> assets) {
        this.assets = assets;
    }

}