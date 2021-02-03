/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.yarn.rm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

import java.util.List;

/**
 * An application resource contains information about a particular application that was submitted to a cluster.
 *
 * Based on Hadoop 3.0.0, refer to https://hadoop.apache.org/docs/current/hadoop-yarn/hadoop-yarn-site/ResourceManagerRest.html#Cluster_Application_API
 *
 * Use the following URI to obtain an apps list,
 *   http://<rm http address:port>/ws/v1/cluster/apps
 *
 * HTTP Operations Supported
 *   GET
 *
 * Query Parameters Supported
 *   None
 */
public class YarnApplications implements IConvertible {
    @JsonProperty(value = "app")
    private List<App> apps;

    public List<App> getApps() {
        return apps;
    }

    public void setApps(List<App> apps) {
        this.apps = apps;
    }
}
