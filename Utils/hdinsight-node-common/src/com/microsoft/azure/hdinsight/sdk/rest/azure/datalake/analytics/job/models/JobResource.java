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
 * The Data Lake Analytics job resources.
 */
public class JobResource {
    /**
     * The name of the resource.
     */
    @JsonProperty(value = "name")
    private String name;

    /**
     * The path to the resource.
     */
    @JsonProperty(value = "resourcePath")
    private String resourcePath;

    /**
     * The job resource type. Possible values include: 'VertexResource', 'JobManagerResource', 'StatisticsResource',
     * 'VertexResourceInUserFolder', 'JobManagerResourceInUserFolder', 'StatisticsResourceInUserFolder'.
     */
    @JsonProperty(value = "type")
    private JobResourceType type;

    /**
     * Get the name of the resource.
     *
     * @return the name value
     */
    public String name() {
        return this.name;
    }

    /**
     * Set the name of the resource.
     *
     * @param name the name value to set
     * @return the JobResource object itself.
     */
    public JobResource withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get the path to the resource.
     *
     * @return the resourcePath value
     */
    public String resourcePath() {
        return this.resourcePath;
    }

    /**
     * Set the path to the resource.
     *
     * @param resourcePath the resourcePath value to set
     * @return the JobResource object itself.
     */
    public JobResource withResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
        return this;
    }

    /**
     * Get the job resource type. Possible values include: 'VertexResource', 'JobManagerResource', 'StatisticsResource', 'VertexResourceInUserFolder', 'JobManagerResourceInUserFolder', 'StatisticsResourceInUserFolder'.
     *
     * @return the type value
     */
    public JobResourceType type() {
        return this.type;
    }

    /**
     * Set the job resource type. Possible values include: 'VertexResource', 'JobManagerResource', 'StatisticsResource', 'VertexResourceInUserFolder', 'JobManagerResourceInUserFolder', 'StatisticsResourceInUserFolder'.
     *
     * @param type the type value to set
     * @return the JobResource object itself.
     */
    public JobResource withType(JobResourceType type) {
        this.type = type;
        return this;
    }

}
