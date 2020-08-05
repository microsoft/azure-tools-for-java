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
import com.fasterxml.jackson.annotation.JsonSubTypes;

/**
 * The common Data Lake Analytics job properties for job submission.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonTypeName("CreateJobProperties")
@JsonSubTypes({
    @JsonSubTypes.Type(name = "USql", value = CreateUSqlJobProperties.class),
    @JsonSubTypes.Type(name = "Scope", value = CreateScopeJobProperties.class)
})
public class CreateJobProperties {
    /**
     * The runtime version of the Data Lake Analytics engine to use for the specific type of job being run.
     */
    @JsonProperty(value = "runtimeVersion")
    private String runtimeVersion;

    /**
     * The script to run. Please note that the maximum script size is 3 MB.
     */
    @JsonProperty(value = "script", required = true)
    private String script;

    /**
     * Get the runtime version of the Data Lake Analytics engine to use for the specific type of job being run.
     *
     * @return the runtimeVersion value
     */
    public String runtimeVersion() {
        return this.runtimeVersion;
    }

    /**
     * Set the runtime version of the Data Lake Analytics engine to use for the specific type of job being run.
     *
     * @param runtimeVersion the runtimeVersion value to set
     * @return the CreateJobProperties object itself.
     */
    public CreateJobProperties withRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
        return this;
    }

    /**
     * Get the script to run. Please note that the maximum script size is 3 MB.
     *
     * @return the script value
     */
    public String script() {
        return this.script;
    }

    /**
     * Set the script to run. Please note that the maximum script size is 3 MB.
     *
     * @param script the script value to set
     * @return the CreateJobProperties object itself.
     */
    public CreateJobProperties withScript(String script) {
        this.script = script;
        return this;
    }

}
