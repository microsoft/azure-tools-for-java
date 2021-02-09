/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Run info for a specific job pipeline.
 */
public class JobPipelineRunInformation {
    /**
     * The run identifier of an instance of pipeline executions (a GUID).
     */
    @JsonProperty(value = "runId", access = JsonProperty.Access.WRITE_ONLY)
    private UUID runId;

    /**
     * The time this instance was last submitted.
     */
    @JsonProperty(value = "lastSubmitTime", access = JsonProperty.Access.WRITE_ONLY)
    private String lastSubmitTime;

    /**
     * Get the run identifier of an instance of pipeline executions (a GUID).
     *
     * @return the runId value
     */
    public UUID runId() {
        return this.runId;
    }

    /**
     * Get the time this instance was last submitted.
     *
     * @return the lastSubmitTime value
     */
    public String lastSubmitTime() {
        return this.lastSubmitTime;
    }

}
