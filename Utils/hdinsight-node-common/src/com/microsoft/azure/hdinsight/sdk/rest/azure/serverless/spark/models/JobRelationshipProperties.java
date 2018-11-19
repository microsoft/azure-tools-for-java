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

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The job relationship information properties including pipeline information, correlation information, etc.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobRelationshipProperties {
    /**
     * the job relationship pipeline identifier (a GUID).
     */
    @JsonProperty(value = "pipelineId", access = JsonProperty.Access.WRITE_ONLY)
    private UUID pipelineId;

    /**
     * the friendly name of the job relationship pipeline, which does not need to be unique.
     */
    @JsonProperty(value = "pipelineName", access = JsonProperty.Access.WRITE_ONLY)
    private String pipelineName;

    /**
     * the pipeline uri, unique, links to the originating service for this pipeline.
     */
    @JsonProperty(value = "pipelineUri", access = JsonProperty.Access.WRITE_ONLY)
    private String pipelineUri;

    /**
     * the run identifier (a GUID), unique identifier of the iteration of this pipeline.
     */
    @JsonProperty(value = "runId", access = JsonProperty.Access.WRITE_ONLY)
    private UUID runId;

    /**
     * the recurrence identifier (a GUID), unique per activity/script, regardless of iterations. This is something to
     * link different occurrences of the same job together.
     */
    @JsonProperty(value = "recurrenceId", access = JsonProperty.Access.WRITE_ONLY)
    private UUID recurrenceId;

    /**
     * the recurrence name, user friendly name for the correlation between jobs.
     */
    @JsonProperty(value = "recurrenceName", access = JsonProperty.Access.WRITE_ONLY)
    private String recurrenceName;

    /**
     * Get the job relationship pipeline identifier (a GUID).
     *
     * @return the pipelineId value
     */
    public UUID pipelineId() {
        return this.pipelineId;
    }

    /**
     * Get the friendly name of the job relationship pipeline, which does not need to be unique.
     *
     * @return the pipelineName value
     */
    public String pipelineName() {
        return this.pipelineName;
    }

    /**
     * Get the pipeline uri, unique, links to the originating service for this pipeline.
     *
     * @return the pipelineUri value
     */
    public String pipelineUri() {
        return this.pipelineUri;
    }

    /**
     * Get the run identifier (a GUID), unique identifier of the iteration of this pipeline.
     *
     * @return the runId value
     */
    public UUID runId() {
        return this.runId;
    }

    /**
     * Get the recurrence identifier (a GUID), unique per activity/script, regardless of iterations. This is something to link different occurrences of the same job together.
     *
     * @return the recurrenceId value
     */
    public UUID recurrenceId() {
        return this.recurrenceId;
    }

    /**
     * Get the recurrence name, user friendly name for the correlation between jobs.
     *
     * @return the recurrenceName value
     */
    public String recurrenceName() {
        return this.recurrenceName;
    }

}
