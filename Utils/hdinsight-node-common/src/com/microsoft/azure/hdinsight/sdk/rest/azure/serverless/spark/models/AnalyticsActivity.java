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

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import java.util.Date;
import java.util.UUID;
import java.util.Map;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The common Data Lake Analytics activity information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalyticsActivity {
    /**
     * the activity's unique identifier (a GUID).
     */
    @JsonProperty(value = "id", access = JsonProperty.Access.WRITE_ONLY)
    private UUID id;

    /**
     * the friendly name of the activity.
     */
    @JsonProperty(value = "name", access = JsonProperty.Access.WRITE_ONLY)
    private String name;

    /**
     * the activity type.
     */
    @JsonProperty(value = "activityType", access = JsonProperty.Access.WRITE_ONLY)
    private String activityType;

    /**
     * Category of the job. Possible values include: 'ResourcePools', 'BatchJobs', 'StreamingJobs'.
     */
    @JsonProperty(value = "entityType", access = JsonProperty.Access.WRITE_ONLY)
    private EntityType entityType;

    /**
     * the number of Analytics Units (AUs) used for this activity.
     */
    @JsonProperty(value = "analyticsUnits", access = JsonProperty.Access.WRITE_ONLY)
    private Integer analyticsUnits;

    /**
     * the user or account that submitted the activity.
     */
    @JsonProperty(value = "submitter", access = JsonProperty.Access.WRITE_ONLY)
    private String submitter;

    /**
     * State in which the activity is in from the perspective of the scheduler. These states are common across
     * different activity types. Kept here for backward compatibility. Soon to be deprecated. Possible values include:
     * 'Any', 'Submitted', 'Preparing', 'Queued', 'Scheduled', 'Finalizing', 'Ended'.
     */
    @JsonProperty(value = "state")
    private SchedulerState state;

    /**
     * State in which the activity is in from the perspective of the scheduler. These states are common across
     * different activity types. Possible values include: 'Any', 'Submitted', 'Preparing', 'Queued', 'Scheduled',
     * 'Finalizing', 'Ended'.
     */
    @JsonProperty(value = "schedulerState")
    private SchedulerState schedulerState;

    /**
     * State in which the activity is in from the perspective of the activity plugin. The value set for this state
     * differs for each activity type.
     */
    @JsonProperty(value = "activityState", access = JsonProperty.Access.WRITE_ONLY)
    private String activityState;

    /**
     * Result of the activity. Possible values include: 'None', 'Succeeded', 'Cancelled', 'Failed'.
     */
    @JsonProperty(value = "result", access = JsonProperty.Access.WRITE_ONLY)
    private ActivityResult result;

    /**
     * the time the activity was submitted to the service.
     */
    @JsonProperty(value = "submitTime", access = JsonProperty.Access.WRITE_ONLY)
    private Date submitTime;

    /**
     * the start time of the activity.
     */
    @JsonProperty(value = "startTime", access = JsonProperty.Access.WRITE_ONLY)
    private Date startTime;

    /**
     * the completion time of the activity.
     */
    @JsonProperty(value = "endTime", access = JsonProperty.Access.WRITE_ONLY)
    private Date endTime;

    /**
     * the key-value pairs used to add additional metadata to the activity.
     */
    @JsonProperty(value = "tags", access = JsonProperty.Access.WRITE_ONLY)
    private Map<String, String> tags;

    /**
     * The priority value to use for the current job. Lower numbers have a higher priority. By default, a job has a
     * priority of 1000. This must be greater than 0.
     */
    @JsonProperty(value = "priority", access = JsonProperty.Access.WRITE_ONLY)
    private Integer priority;

    /**
     * the name of hierarchy queue node this job is assigned to, Null if job has not been assigned yet or the account
     * doesn't have hierarchy queue.
     */
    @JsonProperty(value = "hierarchyQueueNode", access = JsonProperty.Access.WRITE_ONLY)
    private String hierarchyQueueNode;

    /**
     * The error message details for the activity, if the activity failed.
     */
//    @JsonProperty(value = "errorMessage", access = JsonProperty.Access.WRITE_ONLY)
//    private List<ErrorDetails> errorMessage;

    /**
     * Get the activity's unique identifier (a GUID).
     *
     * @return the id value
     */
    public UUID id() {
        return this.id;
    }

    /**
     * Get the friendly name of the activity.
     *
     * @return the name value
     */
    public String name() {
        return this.name;
    }

    /**
     * Get the activity type.
     *
     * @return the activityType value
     */
    public String activityType() {
        return this.activityType;
    }

    /**
     * Get category of the job. Possible values include: 'ResourcePools', 'BatchJobs', 'StreamingJobs'.
     *
     * @return the entityType value
     */
    public EntityType entityType() {
        return this.entityType;
    }

    /**
     * Get the number of Analytics Units (AUs) used for this activity.
     *
     * @return the analyticsUnits value
     */
    public Integer analyticsUnits() {
        return this.analyticsUnits;
    }

    /**
     * Get the user or account that submitted the activity.
     *
     * @return the submitter value
     */
    public String submitter() {
        return this.submitter;
    }

    /**
     * Get state in which the activity is in from the perspective of the scheduler. These states are common across different activity types. Kept here for backward compatibility. Soon to be deprecated. Possible values include: 'Any', 'Submitted', 'Preparing', 'Queued', 'Scheduled', 'Finalizing', 'Ended'.
     *
     * @return the state value
     */
    public SchedulerState state() {
        return this.state;
    }

    /**
     * Set state in which the activity is in from the perspective of the scheduler. These states are common across different activity types. Kept here for backward compatibility. Soon to be deprecated. Possible values include: 'Any', 'Submitted', 'Preparing', 'Queued', 'Scheduled', 'Finalizing', 'Ended'.
     *
     * @param state the state value to set
     * @return the AnalyticsActivity object itself.
     */
    public AnalyticsActivity withState(SchedulerState state) {
        this.state = state;
        return this;
    }

    /**
     * Get state in which the activity is in from the perspective of the scheduler. These states are common across different activity types. Possible values include: 'Any', 'Submitted', 'Preparing', 'Queued', 'Scheduled', 'Finalizing', 'Ended'.
     *
     * @return the schedulerState value
     */
    public SchedulerState schedulerState() {
        return this.schedulerState;
    }

    /**
     * Set state in which the activity is in from the perspective of the scheduler. These states are common across different activity types. Possible values include: 'Any', 'Submitted', 'Preparing', 'Queued', 'Scheduled', 'Finalizing', 'Ended'.
     *
     * @param schedulerState the schedulerState value to set
     * @return the AnalyticsActivity object itself.
     */
    public AnalyticsActivity withSchedulerState(SchedulerState schedulerState) {
        this.schedulerState = schedulerState;
        return this;
    }

    /**
     * Get state in which the activity is in from the perspective of the activity plugin. The value set for this state differs for each activity type.
     *
     * @return the activityState value
     */
    public String activityState() {
        return this.activityState;
    }

    /**
     * Get result of the activity. Possible values include: 'None', 'Succeeded', 'Cancelled', 'Failed'.
     *
     * @return the result value
     */
    public ActivityResult result() {
        return this.result;
    }

    /**
     * Get the time the activity was submitted to the service.
     *
     * @return the submitTime value
     */
    public Date submitTime() {
        return this.submitTime;
    }

    /**
     * Get the start time of the activity.
     *
     * @return the startTime value
     */
    public Date startTime() {
        return this.startTime;
    }

    /**
     * Get the completion time of the activity.
     *
     * @return the endTime value
     */
    public Date endTime() {
        return this.endTime;
    }

    /**
     * Get the key-value pairs used to add additional metadata to the activity.
     *
     * @return the tags value
     */
    public Map<String, String> tags() {
        return this.tags;
    }

    /**
     * Get the priority value to use for the current job. Lower numbers have a higher priority. By default, a job has a priority of 1000. This must be greater than 0.
     *
     * @return the priority value
     */
    public Integer priority() {
        return this.priority;
    }

    /**
     * Get the name of hierarchy queue node this job is assigned to, Null if job has not been assigned yet or the account doesn't have hierarchy queue.
     *
     * @return the hierarchyQueueNode value
     */
    public String hierarchyQueueNode() {
        return this.hierarchyQueueNode;
    }

    /**
     * Get the error message details for the activity, if the activity failed.
     *
     * @return the errorMessage value
     */
//    public List<ErrorDetails> errorMessage() {
//        return this.errorMessage;
//    }

}
