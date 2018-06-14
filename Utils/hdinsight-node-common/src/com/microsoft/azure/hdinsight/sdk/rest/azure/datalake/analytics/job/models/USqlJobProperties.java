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

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models;

import java.util.List;
import org.joda.time.Period;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * U-SQL job properties used when retrieving U-SQL jobs.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonTypeName("USql")
public class USqlJobProperties extends JobProperties {
    /**
     * the list of resources that are required by the job.
     */
    @JsonProperty(value = "resources", access = JsonProperty.Access.WRITE_ONLY)
    private List<JobResource> resources;

    /**
     * the job specific statistics.
     */
    @JsonProperty(value = "statistics")
    private JobStatistics statistics;

    /**
     * the job specific debug data locations.
     */
    @JsonProperty(value = "debugData")
    private JobDataPath debugData;

    /**
     * the diagnostics for the job.
     */
    @JsonProperty(value = "diagnostics", access = JsonProperty.Access.WRITE_ONLY)
    private List<Diagnostics> diagnostics;

    /**
     * the algebra file path after the job has completed.
     */
    @JsonProperty(value = "algebraFilePath", access = JsonProperty.Access.WRITE_ONLY)
    private String algebraFilePath;

    /**
     * the total time this job spent compiling. This value should not be set by the user and will be ignored if it is.
     */
    @JsonProperty(value = "totalCompilationTime", access = JsonProperty.Access.WRITE_ONLY)
    private Period totalCompilationTime;

    /**
     * the total time this job spent paused. This value should not be set by the user and will be ignored if it is.
     */
    @JsonProperty(value = "totalPauseTime", access = JsonProperty.Access.WRITE_ONLY)
    private Period totalPauseTime;

    /**
     * the total time this job spent queued. This value should not be set by the user and will be ignored if it is.
     */
    @JsonProperty(value = "totalQueuedTime", access = JsonProperty.Access.WRITE_ONLY)
    private Period totalQueuedTime;

    /**
     * the total time this job spent executing. This value should not be set by the user and will be ignored if it is.
     */
    @JsonProperty(value = "totalRunningTime", access = JsonProperty.Access.WRITE_ONLY)
    private Period totalRunningTime;

    /**
     * the ID used to identify the job manager coordinating job execution. This value should not be set by the user and
     * will be ignored if it is.
     */
    @JsonProperty(value = "rootProcessNodeId", access = JsonProperty.Access.WRITE_ONLY)
    private String rootProcessNodeId;

    /**
     * the ID used to identify the yarn application executing the job. This value should not be set by the user and
     * will be ignored if it is.
     */
    @JsonProperty(value = "yarnApplicationId", access = JsonProperty.Access.WRITE_ONLY)
    private String yarnApplicationId;

    /**
     * the timestamp (in ticks) for the yarn application executing the job. This value should not be set by the user
     * and will be ignored if it is.
     */
    @JsonProperty(value = "yarnApplicationTimeStamp", access = JsonProperty.Access.WRITE_ONLY)
    private Long yarnApplicationTimeStamp;

    /**
     * the specific compilation mode for the job used during execution. If this is not specified during submission, the
     * server will determine the optimal compilation mode. Possible values include: 'Semantic', 'Full', 'SingleBox'.
     */
    @JsonProperty(value = "compileMode", access = JsonProperty.Access.WRITE_ONLY)
    private CompileMode compileMode;

    /**
     * Get the resources value.
     *
     * @return the resources value
     */
    public List<JobResource> resources() {
        return this.resources;
    }

    /**
     * Get the statistics value.
     *
     * @return the statistics value
     */
    public JobStatistics statistics() {
        return this.statistics;
    }

    /**
     * Set the statistics value.
     *
     * @param statistics the statistics value to set
     * @return the USqlJobProperties object itself.
     */
    public USqlJobProperties withStatistics(JobStatistics statistics) {
        this.statistics = statistics;
        return this;
    }

    /**
     * Get the debugData value.
     *
     * @return the debugData value
     */
    public JobDataPath debugData() {
        return this.debugData;
    }

    /**
     * Set the debugData value.
     *
     * @param debugData the debugData value to set
     * @return the USqlJobProperties object itself.
     */
    public USqlJobProperties withDebugData(JobDataPath debugData) {
        this.debugData = debugData;
        return this;
    }

    /**
     * Get the diagnostics value.
     *
     * @return the diagnostics value
     */
    public List<Diagnostics> diagnostics() {
        return this.diagnostics;
    }

    /**
     * Get the algebraFilePath value.
     *
     * @return the algebraFilePath value
     */
    public String algebraFilePath() {
        return this.algebraFilePath;
    }

    /**
     * Get the totalCompilationTime value.
     *
     * @return the totalCompilationTime value
     */
    public Period totalCompilationTime() {
        return this.totalCompilationTime;
    }

    /**
     * Get the totalPauseTime value.
     *
     * @return the totalPauseTime value
     */
    public Period totalPauseTime() {
        return this.totalPauseTime;
    }

    /**
     * Get the totalQueuedTime value.
     *
     * @return the totalQueuedTime value
     */
    public Period totalQueuedTime() {
        return this.totalQueuedTime;
    }

    /**
     * Get the totalRunningTime value.
     *
     * @return the totalRunningTime value
     */
    public Period totalRunningTime() {
        return this.totalRunningTime;
    }

    /**
     * Get the rootProcessNodeId value.
     *
     * @return the rootProcessNodeId value
     */
    public String rootProcessNodeId() {
        return this.rootProcessNodeId;
    }

    /**
     * Get the yarnApplicationId value.
     *
     * @return the yarnApplicationId value
     */
    public String yarnApplicationId() {
        return this.yarnApplicationId;
    }

    /**
     * Get the yarnApplicationTimeStamp value.
     *
     * @return the yarnApplicationTimeStamp value
     */
    public Long yarnApplicationTimeStamp() {
        return this.yarnApplicationTimeStamp;
    }

    /**
     * Get the compileMode value.
     *
     * @return the compileMode value
     */
    public CompileMode compileMode() {
        return this.compileMode;
    }

}
