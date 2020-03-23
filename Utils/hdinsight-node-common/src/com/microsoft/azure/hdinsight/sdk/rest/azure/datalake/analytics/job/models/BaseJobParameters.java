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
 * Data Lake Analytics Job Parameters base class for build and submit.
 */
public class BaseJobParameters {
    /**
     * The job type of the current job (Hive, USql, or Scope (for internal use only)). Possible values include: 'USql',
     * 'Hive', 'Scope'.
     */
    @JsonProperty(value = "type", required = true)
    private JobType type;

    /**
     * The job specific properties.
     */
    @JsonProperty(value = "properties", required = true)
    private CreateJobProperties properties;

    /**
     * Get the job type of the current job (Hive, USql, or Scope (for internal use only)). Possible values include: 'USql', 'Hive', 'Scope'.
     *
     * @return the type value
     */
    public JobType type() {
        return this.type;
    }

    /**
     * Set the job type of the current job (Hive, USql, or Scope (for internal use only)). Possible values include: 'USql', 'Hive', 'Scope'.
     *
     * @param type the type value to set
     * @return the BaseJobParameters object itself.
     */
    public BaseJobParameters withType(JobType type) {
        this.type = type;
        return this;
    }

    /**
     * Get the job specific properties.
     *
     * @return the properties value
     */
    public CreateJobProperties properties() {
        return this.properties;
    }

    /**
     * Set the job specific properties.
     *
     * @param properties the properties value to set
     * @return the BaseJobParameters object itself.
     */
    public BaseJobParameters withProperties(CreateJobProperties properties) {
        this.properties = properties;
        return this;
    }

}
