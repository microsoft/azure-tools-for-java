/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.livy.interactive;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Session implements IConvertible {
    private int                 id;         // The session id
    private String              appId;      // The application id of this session
    private String              owner;      // Remote user who submitted this session
    private String              proxyUser;  // User to impersonate when running
    private SessionKind         kind;       // Session kind (spark, pyspark, or sparkr)
    private List<String>        log;        // The log lines
    private SessionState        state;      // The session state
    private Map<String, String> appInfo;    // The detailed application info

    public int getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public String getOwner() {
        return owner;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public SessionKind getKind() {
        return kind;
    }

    public List<String> getLog() {
        return log;
    }

    public SessionState getState() {
        return state;
    }

    public Map<String, String> getAppInfo() {
        return appInfo;
    }
}
