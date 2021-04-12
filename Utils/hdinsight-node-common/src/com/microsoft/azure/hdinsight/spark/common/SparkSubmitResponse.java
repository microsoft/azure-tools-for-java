/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SparkSubmitResponse {
    private int id;
    private String state;

    private String appId;                   // The application ID
    private Map<String, Object> appInfo;    // The detailed application info
    private List<String> log;               // The log lines

    public String getAppId() {
        return appId;
    }

    public Map<String, Object> getAppInfo() {
        return appInfo;
    }

    public List<String> getLog() {
        return log == null ? Collections.emptyList() : log;
    }

    public int getId(){
        return id;
    }

    public String getState(){
        return state;
    }

    public boolean isAlive() {
        return !this.getState().equals("error") &&
                !this.getState().equals("success") &&
                !this.getState().equals("dead");
    }
}
