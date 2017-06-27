package com.microsoft.azuretools.container.utils;

public class PrivateRegistry {
    private String url;
    private String username;
    private String password;
    private String startupFile;
    public PrivateRegistry(String url, String username, String password) {
        super();
        this.url = url;
        this.username = username;
        this.password = password;
    }
    public PrivateRegistry(String url, String username, String password, String startupFile) {
        super();
        this.url = url;
        this.username = username;
        this.password = password;
        this.startupFile = startupFile;
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getStartupFile() {
        return startupFile;
    }
    public void setStartupFile(String startupFile) {
        this.startupFile = startupFile;
    }
}
