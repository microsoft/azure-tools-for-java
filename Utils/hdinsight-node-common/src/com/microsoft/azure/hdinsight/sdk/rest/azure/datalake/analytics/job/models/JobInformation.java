/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
