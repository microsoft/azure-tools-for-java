/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

import com.google.gson.annotations.SerializedName;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

public class Gateway {

    @SerializedName("restAuthCredential.isEnabled")
    private String isEnabled;

    @SerializedName("restAuthCredential.password")
    private String password;

    @SerializedName("restAuthCredential.username")
    private String username;

    public String getIsEnabled(){
        return isEnabled;
    }

    public String getPassword(){
        return password;
    }

    public String getUsername(){
        return username;
    }

    public void setIsEnabled(@Nullable String isEnabled) {
        this.isEnabled = isEnabled;
    }

    public void setPassword(@Nullable String password) {
        this.password = password;
    }

    public void setUsername(@Nullable String username) {
        this.username = username;
    }
}
