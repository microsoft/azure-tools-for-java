/**
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
 *
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import java.util.Date;
import java.util.UUID;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The common Data Lake Analytics activity information.
 */
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
     * State of the Activity. Possible values include: 'Any', 'Submitted', 'Preparing', 'Queued', 'Scheduled',
     * 'Finalizing', 'Ended'.
     */
    @JsonProperty(value = "state")
    private ActivityState state;

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
     * the specific identifier for the type of error encountered in the activity.
     */
    @JsonProperty(value = "errorId", access = JsonProperty.Access.WRITE_ONLY)
    private String errorId;

    /**
     * the key-value pairs used to add additional metadata to the activity.
     */
    @JsonProperty(value = "tags", access = JsonProperty.Access.WRITE_ONLY)
    private Map<String, String> tags;

    /**
     * Get the id value.
     *
     * @return the id value
     */
    public UUID id() {
        return this.id;
    }

    /**
     * Get the name value.
     *
     * @return the name value
     */
    public String name() {
        return this.name;
    }

    /**
     * Get the activityType value.
     *
     * @return the activityType value
     */
    public String activityType() {
        return this.activityType;
    }

    /**
     * Get the analyticsUnits value.
     *
     * @return the analyticsUnits value
     */
    public Integer analyticsUnits() {
        return this.analyticsUnits;
    }

    /**
     * Get the submitter value.
     *
     * @return the submitter value
     */
    public String submitter() {
        return this.submitter;
    }

    /**
     * Get the state value.
     *
     * @return the state value
     */
    public ActivityState state() {
        return this.state;
    }

    /**
     * Set the state value.
     *
     * @param state the state value to set
     * @return the AnalyticsActivity object itself.
     */
    public AnalyticsActivity withState(ActivityState state) {
        this.state = state;
        return this;
    }

    /**
     * Get the submitTime value.
     *
     * @return the submitTime value
     */
    public Date submitTime() {
        return this.submitTime;
    }

    /**
     * Get the startTime value.
     *
     * @return the startTime value
     */
    public Date startTime() {
        return this.startTime;
    }

    /**
     * Get the endTime value.
     *
     * @return the endTime value
     */
    public Date endTime() {
        return this.endTime;
    }

    /**
     * Get the errorId value.
     *
     * @return the errorId value
     */
    public String errorId() {
        return this.errorId;
    }

    /**
     * Get the tags value.
     *
     * @return the tags value
     */
    public Map<String, String> tags() {
        return this.tags;
    }

}
