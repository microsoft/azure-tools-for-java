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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Data Lake Analytics job state audit records for tracking the lifecycle of a job.
 */
public class JobStateAuditRecord {
    /**
     * The new state the job is in.
     */
    @JsonProperty(value = "newState", access = JsonProperty.Access.WRITE_ONLY)
    private String newState;

    /**
     * The time stamp that the state change took place.
     */
    @JsonProperty(value = "timeStamp", access = JsonProperty.Access.WRITE_ONLY)
    private String timeStamp;

    /**
     * The user who requests the change.
     */
    @JsonProperty(value = "requestedByUser", access = JsonProperty.Access.WRITE_ONLY)
    private String requestedByUser;

    /**
     * The details of the audit log.
     */
    @JsonProperty(value = "details", access = JsonProperty.Access.WRITE_ONLY)
    private String details;

    /**
     * Get the new state the job is in.
     *
     * @return the newState value
     */
    public String newState() {
        return this.newState;
    }

    /**
     * Get the time stamp that the state change took place.
     *
     * @return the timeStamp value
     */
    public String timeStamp() {
        return this.timeStamp;
    }

    /**
     * Get the user who requests the change.
     *
     * @return the requestedByUser value
     */
    public String requestedByUser() {
        return this.requestedByUser;
    }

    /**
     * Get the details of the audit log.
     *
     * @return the details value
     */
    public String details() {
        return this.details;
    }

}
