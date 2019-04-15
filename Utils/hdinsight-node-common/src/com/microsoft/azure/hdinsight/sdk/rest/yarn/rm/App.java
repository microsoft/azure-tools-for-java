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
package com.microsoft.azure.hdinsight.sdk.rest.yarn.rm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An application resource contains information about a particular application that was submitted to a cluster.
 *
 * Based on Hadoop 3.0.0, refer to https://hadoop.apache.org/docs/current/hadoop-yarn/hadoop-yarn-site/ResourceManagerRest.html#Cluster_Application_API
 *
 * Use the following URI to obtain an app object, from a application identified by the appid value.
 *   http://<rm http address:port>/ws/v1/cluster/apps/{appid}
 *
 * HTTP Operations Supported
 *   GET
 *
 * Query Parameters Supported
 *   None
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class App implements IConvertible {
    private String id;                  // The application id
    private String user;                // The user who started the application
    private String name;                // The application name
    private String applicationType;    // The application type
    private String queue;               // The queue the application was submitted to
    private String state;               // The application state according to the ResourceManager -
                                        // valid values are members of the YarnApplicationState enum:
                                        // NEW, NEW_SAVING, SUBMITTED, ACCEPTED, RUNNING, FINISHED, FAILED, KILLED
    private String finalStatus;         // The final status of the application if finished -
                                        // reported by the application itself - valid values are:
                                        // UNDEFINED, SUCCEEDED, FAILED, KILLED
    private float progress;             // The progress of the application as a percent
    private String trackingUI;          // Where the tracking url is currently pointing -
                                        // History (for history server) or ApplicationMaster
    private String trackingUrl;         // The web URL that can be used to track the application
    private String diagnostics;         // Detailed diagnostics information
    private long clusterId;             // The cluster id
    private long startedTime;           // The time in which application started (in ms since epoch)
    private long finishedTime;          // The time in which the application finished (in ms since epoch)
    private long elapsedTime;           // The elapsed time since the application started (in ms)
    private String amContainerLogs;     // The URL of the application master container logs
    private String amHostHttpAddress;   // The nodes http address of the application master
    private int allocatedMB;            // The sum of memory in MB allocated to the application’s running containers
    private int allocatedVCores;        // The sum of virtual cores allocated to the application’s running containers
    private int runningContainers;      // The number of containers currently running for the application
    private long memorySeconds;         // The amount of memory the application has allocated (megabyte-seconds)
    private long vcoreSeconds;          // The amount of CPU resources the application has allocated (virtual core-seconds)
    private String applicationTags;     // Comma separated tags of an application
    private float clusterUsagePercentage; // The percentage of resources of the cluster that the app is using.
    private long preemptedResourceMB; // Memory used by preempted container
    private int preemptedResourceVCores; // Number of virtual cores used by preempted container
    private boolean unmanagedApplication; // Is the application unmanaged.
    private String priority;              //Priority of the submitted application
    private String logAggregationStatus; // Status of log aggregation - valid values are the members of the LogAggregationStatus enum:
                                         // DISABLED, NOT_START, RUNNING, RUNNING_WITH_FAILURE, SUCCEEDED, FAILED, TIME_OUT
    private int numNonAMContainerPreempted; // Number of standard containers preempted;
    private String amNodeLabelExpression; // Node Label expression which is used to identify the node on which application’s AM container is expected to run.
    private int numAMContainerPreempted; //	Number of application master containers preempted
    private float queueUsagePercentage; // The percentage of resources of the queue that the app is using
    private List<ResourceRequest> resourceRequests; // additional for HDInsight cluster


    public List<ResourceRequest> getResourceRequests() {
        return resourceRequests;
    }

    public void setResourceRequests(List<ResourceRequest> resourceRequests) {
        this.resourceRequests = resourceRequests;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public float getClusterUsagePercentage() {
        return clusterUsagePercentage;
    }

    public void setClusterUsagePercentage(float clusterUsagePercentage) {
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

    public int getPreemptedResourceVCores() {
        return preemptedResourceVCores;
    }

    public void setPreemptedResourceVCores(int preemptedResourceVCores) {
        this.preemptedResourceVCores = preemptedResourceVCores;
    }

    public int getRunningContainers() {
        return runningContainers;
    }

    public void setRunningContainers(int runningContainers) {
        this.runningContainers = runningContainers;
    }

    public int getAllocatedMB() {
        return allocatedMB;
    }

    public void setAllocatedMB(int allocatedMB) {
        this.allocatedMB = allocatedMB;
    }

    public long getPreemptedResourceMB() {
        return preemptedResourceMB;
    }

    public void setPreemptedResourceMB(long preemptedResourceMB) {
        this.preemptedResourceMB = preemptedResourceMB;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isUnmanagedApplication() {
        return unmanagedApplication;
    }

    public void setUnmanagedApplication(boolean unmanagedApplication) {
        this.unmanagedApplication = unmanagedApplication;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public long getFinishedTime() {
        return finishedTime;
    }

    public void setFinishedTime(long finishedTime) {
        this.finishedTime = finishedTime;
    }

    public int getAllocatedVCores() {
        return allocatedVCores;
    }

    public void setAllocatedVCores(int allocatedVCores) {
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

    public long getVcoreSeconds() {
        return vcoreSeconds;
    }

    public void setVcoreSeconds(long vcoreSeconds) {
        this.vcoreSeconds = vcoreSeconds;
    }

    public int getNumNonAMContainerPreempted() {
        return numNonAMContainerPreempted;
    }

    public void setNumNonAMContainerPreempted(int numNonAMContainerPreempted) {
        this.numNonAMContainerPreempted = numNonAMContainerPreempted;
    }

    public long getMemorySeconds() {
        return memorySeconds;
    }

    public void setMemorySeconds(long memorySeconds) {
        this.memorySeconds = memorySeconds;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(long elapsedTime) {
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

    public int getNumAMContainerPreempted() {
        return numAMContainerPreempted;
    }

    public void setNumAMContainerPreempted(int numAMContainerPreempted) {
        this.numAMContainerPreempted = numAMContainerPreempted;
    }

    public String getApplicationTags() {
        return applicationTags;
    }

    public void setApplicationTags(String applicationTags) {
        this.applicationTags = applicationTags;
    }

    public long getClusterId() {
        return clusterId;
    }

    public void setClusterId(long clusterId) {
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

    public long getStartedTime() {
        return startedTime;
    }

    public void setStartedTime(long startedTime) {
        this.startedTime = startedTime;
    }

    public float getQueueUsagePercentage() {
        return queueUsagePercentage;
    }

    public void setQueueUsagePercentage(float queueUsagePercentage) {
        this.queueUsagePercentage = queueUsagePercentage;
    }

    /**
     * Check if the Yarn job finish or not.
     *
     * @return true for finished.
     */
    public boolean isFinished() {
        String state = this.getState().toUpperCase();

        return state.equals("FINISHED") || state.equals("FAILED") || state.equals("KILLED");
    }

    /**
     * Check if it is job submit from livy.
     *
     * @return true for livy job.
     */
    public boolean isLivyJob() {
        return getUser().equalsIgnoreCase("livy");
    }

    public static final List<App> EMPTY_LIST = Collections.unmodifiableList(new ArrayList<>(0));
}

