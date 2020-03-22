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
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Data Lake Analytics job data path item.
 */
public class JobDataPath {
    /**
     * The ID of the job this data is for.
     */
    @JsonProperty(value = "jobId", access = JsonProperty.Access.WRITE_ONLY)
    private UUID jobId;

    /**
     * The command that this job data relates to.
     */
    @JsonProperty(value = "command", access = JsonProperty.Access.WRITE_ONLY)
    private String command;

    /**
     * The list of paths to all of the job data.
     */
    @JsonProperty(value = "paths", access = JsonProperty.Access.WRITE_ONLY)
    private List<String> paths;

    /**
     * Get the ID of the job this data is for.
     *
     * @return the jobId value
     */
    public UUID jobId() {
        return this.jobId;
    }

    /**
     * Get the command that this job data relates to.
     *
     * @return the command value
     */
    public String command() {
        return this.command;
    }

    /**
     * Get the list of paths to all of the job data.
     *
     * @return the paths value
     */
    public List<String> paths() {
        return this.paths;
    }

}
