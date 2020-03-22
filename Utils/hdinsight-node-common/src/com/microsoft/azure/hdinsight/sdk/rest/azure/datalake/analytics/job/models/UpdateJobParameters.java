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

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The parameters that can be used to update existing Data Lake Analytics job information properties. (Only for use
 * internally with Scope job type.).
 */
public class UpdateJobParameters {
    /**
     * The degree of parallelism used for this job. This must be greater than 0, if set to less than 0 it will default
     * to 1.
     */
    @JsonProperty(value = "degreeOfParallelism")
    private Integer degreeOfParallelism;

    /**
     * The priority value for the current job. Lower numbers have a higher priority. By default, a job has a priority
     * of 1000. This must be greater than 0.
     */
    @JsonProperty(value = "priority")
    private Integer priority;

    /**
     * The key-value pairs used to add additional metadata to the job information.
     */
    @JsonProperty(value = "tags")
    private Map<String, String> tags;

    /**
     * Get the degree of parallelism used for this job. This must be greater than 0, if set to less than 0 it will default to 1.
     *
     * @return the degreeOfParallelism value
     */
    public Integer degreeOfParallelism() {
        return this.degreeOfParallelism;
    }

    /**
     * Set the degree of parallelism used for this job. This must be greater than 0, if set to less than 0 it will default to 1.
     *
     * @param degreeOfParallelism the degreeOfParallelism value to set
     * @return the UpdateJobParameters object itself.
     */
    public UpdateJobParameters withDegreeOfParallelism(Integer degreeOfParallelism) {
        this.degreeOfParallelism = degreeOfParallelism;
        return this;
    }

    /**
     * Get the priority value for the current job. Lower numbers have a higher priority. By default, a job has a priority of 1000. This must be greater than 0.
     *
     * @return the priority value
     */
    public Integer priority() {
        return this.priority;
    }

    /**
     * Set the priority value for the current job. Lower numbers have a higher priority. By default, a job has a priority of 1000. This must be greater than 0.
     *
     * @param priority the priority value to set
     * @return the UpdateJobParameters object itself.
     */
    public UpdateJobParameters withPriority(Integer priority) {
        this.priority = priority;
        return this;
    }

    /**
     * Get the key-value pairs used to add additional metadata to the job information.
     *
     * @return the tags value
     */
    public Map<String, String> tags() {
        return this.tags;
    }

    /**
     * Set the key-value pairs used to add additional metadata to the job information.
     *
     * @param tags the tags value to set
     * @return the UpdateJobParameters object itself.
     */
    public UpdateJobParameters withTags(Map<String, String> tags) {
        this.tags = tags;
        return this;
    }

}
