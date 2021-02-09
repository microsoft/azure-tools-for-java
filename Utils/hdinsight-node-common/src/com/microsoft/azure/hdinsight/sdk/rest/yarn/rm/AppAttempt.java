/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.yarn.rm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AppAttempt {
    private int id;                  // The app attempt id
    private String nodeId;           // The node id of the node the attempt ran on
    private String nodeHttpAddress;  // The node http address of the node the attempt ran on
    private String logsLink;         // The http link to the app attempt logs
    private String containerId;      // The id of the container for the app attempt
    private long startTime;          // The start time of the attempt (in ms since epoch)
    private long finishedTime;       // The end time of the attempt (in ms since epoch), 0 for not end
    private String blacklistedNodes; // Nodes blacklist
    private String appAttemptId;     // App Attempt Id

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeHttpAddress() {
        return nodeHttpAddress;
    }

    public void setNodeHttpAddress(String nodeHttpAddress) {
        this.nodeHttpAddress = nodeHttpAddress;
    }

    public String getLogsLink() {
        return logsLink;
    }

    public void setLogsLink(String logsLink) {
        this.logsLink = logsLink;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getFinishedTime() {
        return finishedTime;
    }

    public void setFinishedTime(long finishedTime) {
        this.finishedTime = finishedTime;
    }

    public String getBlacklistedNodes() {
        return blacklistedNodes;
    }

    public void setBlacklistedNodes(String blacklistedNodes) {
        this.blacklistedNodes = blacklistedNodes;
    }

    public String getAppAttemptId() {
        return appAttemptId;
    }

    public void setAppAttemptId(String appAttemptId) {
        this.appAttemptId = appAttemptId;
    }
}
