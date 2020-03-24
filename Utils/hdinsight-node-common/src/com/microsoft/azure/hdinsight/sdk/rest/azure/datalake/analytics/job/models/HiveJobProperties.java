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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Hive job properties used when retrieving Hive jobs.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonTypeName("Hive")
public class HiveJobProperties extends JobProperties {
    /**
     * The Hive logs location.
     */
    @JsonProperty(value = "logsLocation", access = JsonProperty.Access.WRITE_ONLY)
    private String logsLocation;

    /**
     * The location of Hive job output files (both execution output and results).
     */
    @JsonProperty(value = "outputLocation", access = JsonProperty.Access.WRITE_ONLY)
    private String outputLocation;

    /**
     * The number of statements that will be run based on the script.
     */
    @JsonProperty(value = "statementCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer statementCount;

    /**
     * The number of statements that have been run based on the script.
     */
    @JsonProperty(value = "executedStatementCount", access = JsonProperty.Access.WRITE_ONLY)
    private Integer executedStatementCount;

    /**
     * Get the Hive logs location.
     *
     * @return the logsLocation value
     */
    public String logsLocation() {
        return this.logsLocation;
    }

    /**
     * Get the location of Hive job output files (both execution output and results).
     *
     * @return the outputLocation value
     */
    public String outputLocation() {
        return this.outputLocation;
    }

    /**
     * Get the number of statements that will be run based on the script.
     *
     * @return the statementCount value
     */
    public Integer statementCount() {
        return this.statementCount;
    }

    /**
     * Get the number of statements that have been run based on the script.
     *
     * @return the executedStatementCount value
     */
    public Integer executedStatementCount() {
        return this.executedStatementCount;
    }

}
