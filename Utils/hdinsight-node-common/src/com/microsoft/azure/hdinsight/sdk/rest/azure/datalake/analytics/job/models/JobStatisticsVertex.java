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

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The detailed information for a vertex.
 */
public class JobStatisticsVertex {
    /**
     * The name of the vertex.
     */
    @JsonProperty(value = "name", access = JsonProperty.Access.WRITE_ONLY)
    private String name;

    /**
     * The id of the vertex.
     */
    @JsonProperty(value = "vertexId", access = JsonProperty.Access.WRITE_ONLY)
    private UUID vertexId;

    /**
     * The amount of execution time of the vertex.
     */
    @JsonProperty(value = "executionTime", access = JsonProperty.Access.WRITE_ONLY)
    private String executionTime;

    /**
     * The amount of data read of the vertex, in bytes.
     */
    @JsonProperty(value = "dataRead", access = JsonProperty.Access.WRITE_ONLY)
    private Long dataRead;

    /**
     * The amount of peak memory usage of the vertex, in bytes.
     */
    @JsonProperty(value = "peakMemUsage", access = JsonProperty.Access.WRITE_ONLY)
    private Long peakMemUsage;

    /**
     * Get the name of the vertex.
     *
     * @return the name value
     */
    public String name() {
        return this.name;
    }

    /**
     * Get the id of the vertex.
     *
     * @return the vertexId value
     */
    public UUID vertexId() {
        return this.vertexId;
    }

    /**
     * Get the amount of execution time of the vertex.
     *
     * @return the executionTime value
     */
    public String executionTime() {
        return this.executionTime;
    }

    /**
     * Get the amount of data read of the vertex, in bytes.
     *
     * @return the dataRead value
     */
    public Long dataRead() {
        return this.dataRead;
    }

    /**
     * Get the amount of peak memory usage of the vertex, in bytes.
     *
     * @return the peakMemUsage value
     */
    public Long peakMemUsage() {
        return this.peakMemUsage;
    }

}
