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
