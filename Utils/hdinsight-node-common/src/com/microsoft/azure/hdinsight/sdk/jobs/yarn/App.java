/**
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
package com.microsoft.azure.hdinsight.sdk.jobs.yarn;
import com.fasterxml.jackson.annotation.JsonClassDescription;

import java.util.List;

/**
 * Created by ltian on 5/6/2017.
 */
public class App {
    private String progress;

    private String queue;

    private String clusterUsagePercentage;

    private String trackingUI;

    private String state;

    private String amContainerLogs;

    private String applicationType;

    private String preemptedResourceVCores;

    private String runningContainers;

    private String allocatedMB;

    private String preemptedResourceMB;

    private String id;

    private String unmanagedApplication;

    private String priority;

    private String finishedTime;

    private String allocatedVCores;

    private String name;

    private String logAggregationStatus;

    private String vcoreSeconds;

    private String numNonAMContainerPreempted;

    private String memorySeconds;

    private String elapsedTime;

    private String amNodeLabelExpression;

    private String amHostHttpAddress;

    private String finalStatus;

    private String trackingUrl;

    private String numAMContainerPreempted;

    private List<ResourceRequest> resourceRequests;

    private String applicationTags;

    private String clusterId;

    private String user;

    private String diagnostics;

    private String startedTime;

    private String queueUsagePercentage;

    public List<ResourceRequest> getResourceRequests() {
        return resourceRequests;
    }

    public void setResourceRequests(List<ResourceRequest> resourceRequests) {
        this.resourceRequests = resourceRequests;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public String getClusterUsagePercentage() {
        return clusterUsagePercentage;
    }

    public void setClusterUsagePercentage(String clusterUsagePercentage) {
        this.clusterUsagePercentage = clusterUsagePercentage;
    }

    public String getTrackingUI() {
        return trackingUI;
    }

    public void setTrackingUI(String trackingUI) {
        this.trackingUI = trackingUI;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getAmContainerLogs() {
        return amContainerLogs;
    }

    public void setAmContainerLogs(String amContainerLogs) {
        this.amContainerLogs = amContainerLogs;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(String applicationType) {
        this.applicationType = applicationType;
    }

    public String getPreemptedResourceVCores() {
        return preemptedResourceVCores;
    }

    public void setPreemptedResourceVCores(String preemptedResourceVCores) {
        this.preemptedResourceVCores = preemptedResourceVCores;
    }

    public String getRunningContainers() {
        return runningContainers;
    }

    public void setRunningContainers(String runningContainers) {
        this.runningContainers = runningContainers;
    }

    public String getAllocatedMB() {
        return allocatedMB;
    }

    public void setAllocatedMB(String allocatedMB) {
        this.allocatedMB = allocatedMB;
    }

    public String getPreemptedResourceMB() {
        return preemptedResourceMB;
    }

    public void setPreemptedResourceMB(String preemptedResourceMB) {
        this.preemptedResourceMB = preemptedResourceMB;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUnmanagedApplication() {
        return unmanagedApplication;
    }

    public void setUnmanagedApplication(String unmanagedApplication) {
        this.unmanagedApplication = unmanagedApplication;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getFinishedTime() {
        return finishedTime;
    }

    public void setFinishedTime(String finishedTime) {
        this.finishedTime = finishedTime;
    }

    public String getAllocatedVCores() {
        return allocatedVCores;
    }

    public void setAllocatedVCores(String allocatedVCores) {
        this.allocatedVCores = allocatedVCores;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogAggregationStatus() {
        return logAggregationStatus;
    }

    public void setLogAggregationStatus(String logAggregationStatus) {
        this.logAggregationStatus = logAggregationStatus;
    }

    public String getVcoreSeconds() {
        return vcoreSeconds;
    }

    public void setVcoreSeconds(String vcoreSeconds) {
        this.vcoreSeconds = vcoreSeconds;
    }

    public String getNumNonAMContainerPreempted() {
        return numNonAMContainerPreempted;
    }

    public void setNumNonAMContainerPreempted(String numNonAMContainerPreempted) {
        this.numNonAMContainerPreempted = numNonAMContainerPreempted;
    }

    public String getMemorySeconds() {
        return memorySeconds;
    }

    public void setMemorySeconds(String memorySeconds) {
        this.memorySeconds = memorySeconds;
    }

    public String getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(String elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public String getAmNodeLabelExpression() {
        return amNodeLabelExpression;
    }

    public void setAmNodeLabelExpression(String amNodeLabelExpression) {
        this.amNodeLabelExpression = amNodeLabelExpression;
    }

    public String getAmHostHttpAddress() {
        return amHostHttpAddress;
    }

    public void setAmHostHttpAddress(String amHostHttpAddress) {
        this.amHostHttpAddress = amHostHttpAddress;
    }

    public String getFinalStatus() {
        return finalStatus;
    }

    public void setFinalStatus(String finalStatus) {
        this.finalStatus = finalStatus;
    }

    public String getTrackingUrl() {
        return trackingUrl;
    }

    public void setTrackingUrl(String trackingUrl) {
        this.trackingUrl = trackingUrl;
    }

    public String getNumAMContainerPreempted() {
        return numAMContainerPreempted;
    }

    public void setNumAMContainerPreempted(String numAMContainerPreempted) {
        this.numAMContainerPreempted = numAMContainerPreempted;
    }

    public String getApplicationTags() {
        return applicationTags;
    }

    public void setApplicationTags(String applicationTags) {
        this.applicationTags = applicationTags;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getDiagnostics() {
        return diagnostics;
    }

    public void setDiagnostics(String diagnostics) {
        this.diagnostics = diagnostics;
    }

    public String getStartedTime() {
        return startedTime;
    }

    public void setStartedTime(String startedTime) {
        this.startedTime = startedTime;
    }

    public String getQueueUsagePercentage() {
        return queueUsagePercentage;
    }

    public void setQueueUsagePercentage(String queueUsagePercentage) {
        this.queueUsagePercentage = queueUsagePercentage;
    }
}

