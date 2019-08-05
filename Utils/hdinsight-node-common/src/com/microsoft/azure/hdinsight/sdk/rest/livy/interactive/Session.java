/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
