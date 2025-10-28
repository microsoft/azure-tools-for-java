package com.microsoft.azuretools.azuremcp;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GithubUser {
    @JsonProperty("login")
    public String login;
    @JsonProperty("id")
    public long id;
    @JsonProperty("node_id")
    public String nodeId;
    @JsonProperty("avatar_url")
    public String avatarUrl;
    @JsonProperty("gravatar_id")
    public String gravatarId;
    @JsonProperty("url")
    private String url;
    @JsonProperty("html_url")
    private String htmlUrl;
    @JsonProperty("followers_url")
    private String followersUrl;
    @JsonProperty("following_url")
    private String followingUrl;
    @JsonProperty("gists_url")
    private String gistsUrl;
    @JsonProperty("starred_url")
    private String starredUrl;
    @JsonProperty("subscriptions_url")
    private String subscriptionsUrl;
    @JsonProperty("organizations_url")
    private String organizationsUrl;
    @JsonProperty("repos_url")
    private String reposUrl;
    @JsonProperty("events_url")
    private String eventsUrl;
    @JsonProperty("received_events_url")
    private String receivedEventsUrl;
    @JsonProperty("type")
    private String type;
    @JsonProperty("site_admin")
    private boolean siteAdmin;

    public String getLogin() {
        return login;
    }
    public void setLogin(String login) {
        this.login = login;
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
    public String getAvatarUrl() {
        return avatarUrl;
    }
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    public String getGravatarId() {
        return gravatarId;
    }
    public void setGravatarId(String gravatarId) {
        this.gravatarId = gravatarId;
    }
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
    public String getFollowersUrl() {
        return followersUrl;
    }
    public void setFollowersUrl(String followersUrl) {
        this.followersUrl = followersUrl;
    }
    public String getFollowingUrl() {
        return followingUrl;
    }
    public void setFollowingUrl(String followingUrl) {
        this.followingUrl = followingUrl;
    }
    public String getGistsUrl() {
        return gistsUrl;
    }
    public void setGistsUrl(String gistsUrl) {
        this.gistsUrl = gistsUrl;
    }
    public String getStarredUrl() {
        return starredUrl;
    }
    public void setStarredUrl(String starredUrl) {
        this.starredUrl = starredUrl;
    }
    public String getSubscriptionsUrl() {
        return subscriptionsUrl;
    }
    public void setSubscriptionsUrl(String subscriptionsUrl) {
        this.subscriptionsUrl = subscriptionsUrl;
    }
    public String getOrganizationsUrl() {
        return organizationsUrl;
    }
    public void setOrganizationsUrl(String organizationsUrl) {
        this.organizationsUrl = organizationsUrl;
    }
    public String getReposUrl() {
        return reposUrl;
    }
    public void setReposUrl(String reposUrl) {
        this.reposUrl = reposUrl;
    }
    public String getEventsUrl() {
        return eventsUrl;
    }
    public void setEventsUrl(String eventsUrl) {
        this.eventsUrl = eventsUrl;
    }
    public String getReceivedEventsUrl() {
        return receivedEventsUrl;
    }
    public void setReceivedEventsUrl(String receivedEventsUrl) {
        this.receivedEventsUrl = receivedEventsUrl;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public boolean isSiteAdmin() {
        return siteAdmin;
    }
    public void setSiteAdmin(boolean siteAdmin) {
        this.siteAdmin = siteAdmin;
    }

}