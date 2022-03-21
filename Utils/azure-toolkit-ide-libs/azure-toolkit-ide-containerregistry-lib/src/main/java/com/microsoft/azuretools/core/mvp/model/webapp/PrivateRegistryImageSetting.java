/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.webapp;

public class PrivateRegistryImageSetting extends ImageSetting {
    private String serverUrl;
    private String username;
    private String password;

    public PrivateRegistryImageSetting() {
    }

    public PrivateRegistryImageSetting(String serverUrl, String username, String password, String imageNameWithTag,
                                       String startupFile) {
        super(imageNameWithTag, startupFile);
        this.setServerUrl(serverUrl);
        this.setUsername(username);
        this.setPassword(password);
    }

    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * remove all the tailing slash and set server URL.
     */
    public void setServerUrl(String serverUrl) {
        if (serverUrl != null) {
            serverUrl = serverUrl.replaceAll("/+$", "");
        }
        this.serverUrl = serverUrl;
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

    public String getImageTagWithServerUrl() {
        if (this.serverUrl != null && super.getImageNameWithTag() != null) {
            return this.serverUrl + "/" + super.getImageNameWithTag();
        }
        return "";
    }
}
