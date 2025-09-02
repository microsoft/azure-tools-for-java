package com.microsoft.azure.toolkit.intellij.azuremcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GithubAsset {
    @JsonProperty("url")
    private String url;
    @JsonProperty("browser_download_url")
    private String browserDownloadUrl;
    @JsonProperty("id")
    private long id;
    @JsonProperty("node_id")
    private String nodeId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("label")
    private String label;
    @JsonProperty("state")
    private String state;
    @JsonProperty("content_type")
    private String contentType;
    @JsonProperty("size")
    private long size;
    @JsonProperty("digest")
    private String digest;
    @JsonProperty("download_count")
    private int downloadCount;
    @JsonProperty("created_at")
    private String createdAt;
    @JsonProperty("updated_at")
    private String updatedAt;
    @JsonProperty("uploader")
    private GithubUser uploader;

}
