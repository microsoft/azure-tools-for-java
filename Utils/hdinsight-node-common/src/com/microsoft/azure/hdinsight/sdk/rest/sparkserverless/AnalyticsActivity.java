/*
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

package com.microsoft.azure.hdinsight.sdk.rest.sparkserverless;


import java.util.UUID;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.joda.time.DateTime;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The common Data Lake Analytics activity information.
 */
public class AnalyticsActivity {
    /**
     * the activity's unique identifier (a GUID).
     */
    @NotNull
    @JsonProperty(value = "id", access = JsonProperty.Access.WRITE_ONLY)
    private UUID id;

    /**
     * the friendly name of the activity.
     */
    @NotNull
    @JsonProperty(value = "name", access = JsonProperty.Access.WRITE_ONLY)
    private String name;

    /**
     * the activity type.
     */
    @NotNull
    @JsonProperty(value = "activityType", access = JsonProperty.Access.WRITE_ONLY)
    private String activityType;

    /**
     * the number of Analytics Units (AUs) used for this activity.
     */
    @NotNull
    @JsonProperty(value = "analyticsUnits", access = JsonProperty.Access.WRITE_ONLY)
    private Integer analyticsUnits;

    /**
     * the user or account that submitted the activity.
     */
    @NotNull
    @JsonProperty(value = "submitter", access = JsonProperty.Access.WRITE_ONLY)
    private String submitter;

    /**
     * the activity state.
     */
    @NotNull
    @JsonProperty(value = "state", access = JsonProperty.Access.WRITE_ONLY)
    private ActivityState state;

    /**
     * the time the activity was submitted to the service.
     */
    @NotNull
    @JsonProperty(value = "submitTime", access = JsonProperty.Access.WRITE_ONLY)
    private DateTime submitTime;

    /**
     * the start time of the activity.
     */
    @NotNull
    @JsonProperty(value = "startTime", access = JsonProperty.Access.WRITE_ONLY)
    private DateTime startTime;

    /**
     * the completion time of the activity.
     */
    @NotNull
    @JsonProperty(value = "endTime", access = JsonProperty.Access.WRITE_ONLY)
    private DateTime endTime;

    /**
     * the specific identifier for the type of error encountered in the
     * activity.
     */
    @NotNull
    @JsonProperty(value = "errorId", access = JsonProperty.Access.WRITE_ONLY)
    private String errorId;

    /**
     * the key-value pairs used to add additional metadata to the activity.
     */
    @NotNull
    @JsonProperty(value = "tags", access = JsonProperty.Access.WRITE_ONLY)
    private Map<String, String> tags;

    /**
     * Get the id value.
     *
     * @return the id value
     */
    @NotNull
    public UUID getId() {
        return this.id;
    }

    /**
     * Get the name value.
     *
     * @return the name value
     */
    @NotNull
    public String getName() {
        return this.name;
    }

    /**
     * Get the activityType value.
     *
     * @return the activityType value
     */
    @NotNull
    public String getActivityType() {
        return this.activityType;
    }

    /**
     * Get the analyticsUnits value.
     *
     * @return the analyticsUnits value
     */
    @NotNull
    public Integer getAnalyticsUnits() {
        return this.analyticsUnits;
    }

    /**
     * Get the submitter value.
     *
     * @return the submitter value
     */
    @NotNull
    public String getSubmitter() {
        return this.submitter;
    }

    /**
     * Get the state value.
     *
     * @return the state value
     */
    @NotNull
    public ActivityState getState() {
        return this.state;
    }

    /**
     * Get the submitTime value.
     *
     * @return the submitTime value
     */
    @NotNull
    public DateTime getSubmitTime() {
        return this.submitTime;
    }

    /**
     * Get the startTime value.
     *
     * @return the startTime value
     */
    @NotNull
    public DateTime getStartTime() {
        return this.startTime;
    }

    /**
     * Get the endTime value.
     *
     * @return the endTime value
     */
    @NotNull
    public DateTime getEndTime() {
        return this.endTime;
    }

    /**
     * Get the errorId value.
     *
     * @return the errorId value
     */
    @NotNull
    public String getErrorId() {
        return this.errorId;
    }

    /**
     * Get the tags value.
     *
     * @return the tags value
     */
    @NotNull
    public Map<String, String> getTags() {
        return this.tags;
    }

}
