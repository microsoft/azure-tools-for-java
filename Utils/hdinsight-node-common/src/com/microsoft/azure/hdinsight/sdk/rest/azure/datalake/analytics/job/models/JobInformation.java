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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The extended Data Lake Analytics job information properties returned when retrieving a specific job.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobInformation extends JobInformationBasic {
    /**
     * The error message details for the job, if the job failed.
     */
    @JsonProperty(value = "errorMessage", access = JsonProperty.Access.WRITE_ONLY)
    private List<JobErrorDetails> errorMessage;

    /**
     * The job state audit records, indicating when various operations have been performed on this job.
     */
    @JsonProperty(value = "stateAuditRecords", access = JsonProperty.Access.WRITE_ONLY)
    private List<JobStateAuditRecord> stateAuditRecords;

    /**
     * The job specific properties.
     */
    @JsonProperty(value = "properties", required = true)
    private JobProperties properties;

    /**
     * Get the error message details for the job, if the job failed.
     *
     * @return the errorMessage value
     */
    public List<JobErrorDetails> errorMessage() {
        return this.errorMessage;
    }

    /**
     * Get the job state audit records, indicating when various operations have been performed on this job.
     *
     * @return the stateAuditRecords value
     */
    public List<JobStateAuditRecord> stateAuditRecords() {
        return this.stateAuditRecords;
    }

    /**
     * Get the job specific properties.
     *
     * @return the properties value
     */
    public JobProperties properties() {
        return this.properties;
    }

    /**
     * Set the job specific properties.
     *
     * @param properties the properties value to set
     * @return the JobInformation object itself.
     */
    public JobInformation withProperties(JobProperties properties) {
        this.properties = properties;
        return this;
    }

}
