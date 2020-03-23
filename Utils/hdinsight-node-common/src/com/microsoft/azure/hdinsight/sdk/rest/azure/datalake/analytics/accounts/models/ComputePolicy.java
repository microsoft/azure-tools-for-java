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

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.rest.serializer.JsonFlatten;
import com.microsoft.azure.SubResource;

/**
 * Data Lake Analytics compute policy information.
 */
@JsonFlatten
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComputePolicy extends SubResource {
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Properties {
        /**
         * The AAD object identifier for the entity to create a policy for.
         */
        @JsonProperty(value = "objectId", access = JsonProperty.Access.WRITE_ONLY)
        private UUID objectId;

        /**
         * The type of AAD object the object identifier refers to. Possible values
         * include: 'User', 'Group', 'ServicePrincipal'.
         */
        @JsonProperty(value = "objectType", access = JsonProperty.Access.WRITE_ONLY)
        private AADObjectType objectType;

        /**
         * The maximum degree of parallelism per job this user can use to submit
         * jobs.
         */
        @JsonProperty(value = "maxDegreeOfParallelismPerJob", access = JsonProperty.Access.WRITE_ONLY)
        private Integer maxDegreeOfParallelismPerJob;

        /**
         * The minimum priority per job this user can use to submit jobs.
         */
        @JsonProperty(value = "minPriorityPerJob", access = JsonProperty.Access.WRITE_ONLY)
        private Integer minPriorityPerJob;
    }

    /**
     * The properties
     */
    @JsonProperty(value = "properties")
    private Properties properties;

    /**
     * The resource name.
     */
    @JsonProperty(value = "name", access = JsonProperty.Access.WRITE_ONLY)
    private String name;

    /**
     * The resource type.
     */
    @JsonProperty(value = "type", access = JsonProperty.Access.WRITE_ONLY)
    private String type;

    /**
     * Get the objectId value.
     *
     * @return the objectId value
     */
    public UUID objectId() {
        return properties == null ? null : properties.objectId;
    }

    /**
     * Get the objectType value.
     *
     * @return the objectType value
     */
    public AADObjectType objectType() {
        return properties == null ? null : properties.objectType;
    }

    /**
     * Get the maxDegreeOfParallelismPerJob value.
     *
     * @return the maxDegreeOfParallelismPerJob value
     */
    public Integer maxDegreeOfParallelismPerJob() {
        return properties == null ? null : properties.maxDegreeOfParallelismPerJob;
    }

    /**
     * Get the minPriorityPerJob value.
     *
     * @return the minPriorityPerJob value
     */
    public Integer minPriorityPerJob() {
        return properties == null ? null : properties.minPriorityPerJob;
    }

    /**
     * Get the name value.
     *
     * @return the name value
     */
    public String name() {
        return this.name;
    }

    /**
     * Get the type value.
     *
     * @return the type value
     */
    public String type() {
        return this.type;
    }

}
