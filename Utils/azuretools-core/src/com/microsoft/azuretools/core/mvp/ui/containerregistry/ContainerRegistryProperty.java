/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.ui.containerregistry;

import com.microsoft.azuretools.core.mvp.ui.base.ResourceProperty;

public class ContainerRegistryProperty extends ResourceProperty {

    private final String id;
    private String loginServerUrl;
    private boolean isAdminEnabled;
    private String userName;
    private String password;
    private String password2;

    /**
     * Model class for ACR property.
     */
    public ContainerRegistryProperty(String id, String name, String type, String groupName, String regionName,
                                     String subscriptionId, String loginServerUrl, boolean isAdminEnabled,
                                     String userName, String password, String password2) {
        super(name, type, groupName, regionName, subscriptionId);
        this.id = id;
        this.loginServerUrl = loginServerUrl;
        this.isAdminEnabled = isAdminEnabled;
        this.userName = userName;
        this.password = password;
        this.password2 = password2;
    }

    public String getId() {
        return id;
    }

    public String getLoginServerUrl() {
        return loginServerUrl;
    }

    public void setLoginServerUrl(String loginServerUrl) {
        this.loginServerUrl = loginServerUrl;
    }

    public boolean isAdminEnabled() {
        return isAdminEnabled;
    }

    public void setAdminEnabled(boolean adminEnabled) {
        isAdminEnabled = adminEnabled;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword2() {
        return password2;
    }

    public void setPassword2(String password2) {
        this.password2 = password2;
    }
}
