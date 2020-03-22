/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
