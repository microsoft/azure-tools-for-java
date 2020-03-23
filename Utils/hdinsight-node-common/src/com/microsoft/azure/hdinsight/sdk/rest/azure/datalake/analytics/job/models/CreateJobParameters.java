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

/**
 * The parameters used to submit a new Data Lake Analytics job.
 */
public class CreateJobParameters extends BaseJobParameters {
    /**
     * The friendly name of the job to submit.
     */
    @JsonProperty(value = "name", required = true)
    private String name;

    /**
     * The degree of parallelism to use for this job. This must be greater than 0, if set to less than 0 it will
     * default to 1.
     */
    @JsonProperty(value = "degreeOfParallelism")
    private Integer degreeOfParallelism;

    /**
     * The priority value to use for the current job. Lower numbers have a higher priority. By default, a job has a
     * priority of 1000. This must be greater than 0.
     */
    @JsonProperty(value = "priority")
    private Integer priority;

    /**
     * The list of log file name patterns to find in the logFolder. '*' is the only matching character allowed. Example
     * format: jobExecution*.log or *mylog*.txt.
     */
    @JsonProperty(value = "logFilePatterns")
    private List<String> logFilePatterns;

    /**
     * The recurring job relationship information properties.
     */
    @JsonProperty(value = "related")
    private JobRelationshipProperties related;

    /**
     * Get the friendly name of the job to submit.
     *
     * @return the name value
     */
    public String name() {
        return this.name;
    }

    /**
     * Set the friendly name of the job to submit.
     *
     * @param name the name value to set
     * @return the CreateJobParameters object itself.
     */
    public CreateJobParameters withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get the degree of parallelism to use for this job. This must be greater than 0, if set to less than 0 it will default to 1.
     *
     * @return the degreeOfParallelism value
     */
    public Integer degreeOfParallelism() {
        return this.degreeOfParallelism;
    }

    /**
     * Set the degree of parallelism to use for this job. This must be greater than 0, if set to less than 0 it will default to 1.
     *
     * @param degreeOfParallelism the degreeOfParallelism value to set
     * @return the CreateJobParameters object itself.
     */
    public CreateJobParameters withDegreeOfParallelism(Integer degreeOfParallelism) {
        this.degreeOfParallelism = degreeOfParallelism;
        return this;
    }

    /**
     * Get the priority value to use for the current job. Lower numbers have a higher priority. By default, a job has a priority of 1000. This must be greater than 0.
     *
     * @return the priority value
     */
    public Integer priority() {
        return this.priority;
    }

    /**
     * Set the priority value to use for the current job. Lower numbers have a higher priority. By default, a job has a priority of 1000. This must be greater than 0.
     *
     * @param priority the priority value to set
     * @return the CreateJobParameters object itself.
     */
    public CreateJobParameters withPriority(Integer priority) {
        this.priority = priority;
        return this;
    }

    /**
     * Get the list of log file name patterns to find in the logFolder. '*' is the only matching character allowed. Example format: jobExecution*.log or *mylog*.txt.
     *
     * @return the logFilePatterns value
     */
    public List<String> logFilePatterns() {
        return this.logFilePatterns;
    }

    /**
     * Set the list of log file name patterns to find in the logFolder. '*' is the only matching character allowed. Example format: jobExecution*.log or *mylog*.txt.
     *
     * @param logFilePatterns the logFilePatterns value to set
     * @return the CreateJobParameters object itself.
     */
    public CreateJobParameters withLogFilePatterns(List<String> logFilePatterns) {
        this.logFilePatterns = logFilePatterns;
        return this;
    }

    /**
     * Get the recurring job relationship information properties.
     *
     * @return the related value
     */
    public JobRelationshipProperties related() {
        return this.related;
    }

    /**
     * Set the recurring job relationship information properties.
     *
     * @param related the related value to set
     * @return the CreateJobParameters object itself.
     */
    public CreateJobParameters withRelated(JobRelationshipProperties related) {
        this.related = related;
        return this;
    }

}
