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
package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

/**
 * Parameters used to submit a new Data Lake Analytics resource pool creation request.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateSparkResourcePool implements IConvertible {
    /**
     * Friendly name of the resource pool to submit.
     */
    @JsonProperty(value = "name", required = true)
    private String name;

    /**
     * The spark resource pool specific properties.
     */
    @JsonProperty(value = "properties", required = true)
    private CreateSparkResourcePoolParameters properties;

    /**
     * Get friendly name of the resource pool to submit.
     *
     * @return the name value
     */
    public String name() {
        return this.name;
    }

    /**
     * Set friendly name of the resource pool to submit.
     *
     * @param name the name value to set
     * @return the CreateSparkResourcePool object itself.
     */
    public CreateSparkResourcePool withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get the spark resource pool specific properties.
     *
     * @return the properties value
     */
    public CreateSparkResourcePoolParameters properties() {
        return this.properties;
    }

    /**
     * Set the spark resource pool specific properties.
     *
     * @param properties the properties value to set
     * @return the CreateSparkResourcePool object itself.
     */
    public CreateSparkResourcePool withProperties(CreateSparkResourcePoolParameters properties) {
        this.properties = properties;
        return this;
    }

}
