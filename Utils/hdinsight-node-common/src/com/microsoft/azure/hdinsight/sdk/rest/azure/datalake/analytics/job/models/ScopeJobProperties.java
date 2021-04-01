/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Scope job properties used when submitting and retrieving Scope jobs. (Only for use internally with Scope job type.).
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonTypeName("Scope")
public class ScopeJobProperties extends JobProperties {
    /**
     * The list of resources that are required by the job.
     */
    @JsonProperty(value = "resources", access = JsonProperty.Access.WRITE_ONLY)
    private List<ScopeJobResource> resources;

    /**
     * The algebra file path after the job has completed.
     */
    @JsonProperty(value = "userAlgebraPath", access = JsonProperty.Access.WRITE_ONLY)
    private String userAlgebraPath;

    /**
     * The list of email addresses, separated by semi-colons, to notify when the job reaches a terminal state.
     */
    @JsonProperty(value = "notifier")
    private String notifier;

    /**
     * The total time this job spent compiling. This value should not be set by the user and will be ignored if it is.
     */
    @JsonProperty(value = "totalCompilationTime", access = JsonProperty.Access.WRITE_ONLY)
    private String totalCompilationTime;

    /**
     * The total time this job spent queued. This value should not be set by the user and will be ignored if it is.
     */
    @JsonProperty(value = "totalQueuedTime", access = JsonProperty.Access.WRITE_ONLY)
    private String totalQueuedTime;

    /**
     * The total time this job spent executing. This value should not be set by the user and will be ignored if it is.
     */
    @JsonProperty(value = "totalRunningTime", access = JsonProperty.Access.WRITE_ONLY)
    private String totalRunningTime;

    /**
     * The total time this job spent paused. This value should not be set by the user and will be ignored if it is.
     */
    @JsonProperty(value = "totalPausedTime", access = JsonProperty.Access.WRITE_ONLY)
    private String totalPausedTime;

    /**
     * The ID used to identify the job manager coordinating job execution. This value should not be set by the user and
     * will be ignored if it is.
     */
    @JsonProperty(value = "rootProcessNodeId", access = JsonProperty.Access.WRITE_ONLY)
    private String rootProcessNodeId;

    /**
     * The ID used to identify the yarn application executing the job. This value should not be set by the user and
     * will be ignored if it is.
     */
    @JsonProperty(value = "yarnApplicationId", access = JsonProperty.Access.WRITE_ONLY)
    private String yarnApplicationId;

    /**
     * Get the list of resources that are required by the job.
     *
     * @return the resources value
     */
    public List<ScopeJobResource> resources() {
        return this.resources;
    }

    /**
     * Get the algebra file path after the job has completed.
     *
     * @return the userAlgebraPath value
     */
    public String userAlgebraPath() {
        return this.userAlgebraPath;
    }

    /**
     * Get the list of email addresses, separated by semi-colons, to notify when the job reaches a terminal state.
     *
     * @return the notifier value
     */
    public String notifier() {
        return this.notifier;
    }

    /**
     * Set the list of email addresses, separated by semi-colons, to notify when the job reaches a terminal state.
     *
     * @param notifier the notifier value to set
     * @return the ScopeJobProperties object itself.
     */
    public ScopeJobProperties withNotifier(String notifier) {
        this.notifier = notifier;
        return this;
    }

    /**
     * Get the total time this job spent compiling. This value should not be set by the user and will be ignored if it is.
     *
     * @return the totalCompilationTime value
     */
    public String totalCompilationTime() {
        return this.totalCompilationTime;
    }

    /**
     * Get the total time this job spent queued. This value should not be set by the user and will be ignored if it is.
     *
     * @return the totalQueuedTime value
     */
    public String totalQueuedTime() {
        return this.totalQueuedTime;
    }

    /**
     * Get the total time this job spent executing. This value should not be set by the user and will be ignored if it is.
     *
     * @return the totalRunningTime value
     */
    public String totalRunningTime() {
        return this.totalRunningTime;
    }

    /**
     * Get the total time this job spent paused. This value should not be set by the user and will be ignored if it is.
     *
     * @return the totalPausedTime value
     */
    public String totalPausedTime() {
        return this.totalPausedTime;
    }

    /**
     * Get the ID used to identify the job manager coordinating job execution. This value should not be set by the user and will be ignored if it is.
     *
     * @return the rootProcessNodeId value
     */
    public String rootProcessNodeId() {
        return this.rootProcessNodeId;
    }

    /**
     * Get the ID used to identify the yarn application executing the job. This value should not be set by the user and will be ignored if it is.
     *
     * @return the yarnApplicationId value
     */
    public String yarnApplicationId() {
        return this.yarnApplicationId;
    }

}
