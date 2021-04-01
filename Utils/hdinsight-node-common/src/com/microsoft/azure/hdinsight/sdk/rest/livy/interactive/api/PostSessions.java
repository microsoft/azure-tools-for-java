/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;
import com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.SessionKind;

import java.util.List;
import java.util.Map;

/**
 * The request body to Creates a new interactive Scala, Python, or R shell in the cluster.
 *
 * Based on Apache Livy, v0.4.0-incubating, refer to http://livy.incubator.apache.org./docs/0.4.0-incubating/rest-api.html
 *
 * Use the following URI:
 *   http://<livy base>/sessions
 *
 * HTTP Operations Supported
 *   POST
 *
 * Query Parameters Supported
 *   None
 *
 * Response Type
 *   @see com.microsoft.azure.hdinsight.sdk.rest.livy.interactive.Session
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PostSessions implements IConvertible {
    private SessionKind         kind;                       // The session kind (required)
    private String              proxyUser;                  // User to impersonate when starting the session
    private List<String>        jars;                       // jars to be used in this session
    private List<String>        pyFiles;                    // Python files to be used in this session
    private List<String>        files;                      // files to be used in this session
    private String              driverMemory;               // Amount of memory to use for the driver process
    private int                 driverCores;                // Number of cores to use for the driver process
    private String              executorMemory;             // Amount of memory to use per executor process
    private int                 executorCores;              // Number of cores to use for each executor
    private int                 numExecutors;               // Number of executors to launch for this session
    private List<String>        archives;                   // Archives to be used in this session
    private String              queue;                      // The name of the YARN queue to which submitted
    private String              name;                       // The name of this session
    private Map<String, String> conf;                       // Spark configuration properties
    private int                 heartbeatTimeoutInSecond;   // Timeout in second to which session be orphaned

    public SessionKind getKind() {
        return kind;
    }

    public void setKind(SessionKind kind) {
        this.kind = kind;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    public List<String> getJars() {
        return jars;
    }

    public void setJars(List<String> jars) {
        this.jars = jars;
    }

    public List<String> getPyFiles() {
        return pyFiles;
    }

    public void setPyFiles(List<String> pyFiles) {
        this.pyFiles = pyFiles;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }

    public String getDriverMemory() {
        return driverMemory;
    }

    public void setDriverMemory(String driverMemory) {
        this.driverMemory = driverMemory;
    }

    public int getDriverCores() {
        return driverCores;
    }

    public void setDriverCores(int driverCores) {
        this.driverCores = driverCores;
    }

    public String getExecutorMemory() {
        return executorMemory;
    }

    public void setExecutorMemory(String executorMemory) {
        this.executorMemory = executorMemory;
    }

    public int getExecutorCores() {
        return executorCores;
    }

    public void setExecutorCores(int executorCores) {
        this.executorCores = executorCores;
    }

    public int getNumExecutors() {
        return numExecutors;
    }

    public void setNumExecutors(int numExecutors) {
        this.numExecutors = numExecutors;
    }

    public List<String> getArchives() {
        return archives;
    }

    public void setArchives(List<String> archives) {
        this.archives = archives;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getConf() {
        return conf;
    }

    public void setConf(Map<String, String> conf) {
        this.conf = conf;
    }

    public int getHeartbeatTimeoutInSecond() {
        return heartbeatTimeoutInSecond;
    }

    public void setHeartbeatTimeoutInSecond(int heartbeatTimeoutInSecond) {
        this.heartbeatTimeoutInSecond = heartbeatTimeoutInSecond;
    }
}
