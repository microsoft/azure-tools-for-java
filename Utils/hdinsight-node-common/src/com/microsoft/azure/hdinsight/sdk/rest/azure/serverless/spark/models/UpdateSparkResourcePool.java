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

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

/**
 * The parameters that can be used to update existing Data Lake Analytics spark resource pool. Only update of number of
 * spark workers is allowed.
 */
public class UpdateSparkResourcePool implements IConvertible {
    /**
     * Friendly name of the resource pool to submit.
     */
    @JsonProperty(value = "name", required = true)
    private String name;

    /**
     * The spark resource pool specific properties.
     */
    @JsonProperty(value = "properties")
    private UpdateSparkResourcePoolParameters properties;

    /**
     * Get the name value.
     *
     * @return the name value
     */
    public String name() {
        return this.name;
    }

    /**
     * Get the properties value.
     *
     * @return the properties value
     */
    public UpdateSparkResourcePoolParameters properties() {
        return this.properties;
    }

    /**
     * Set the name value.
     *
     * @param name the name value to set
     * @return the UpdateSparkResourcePool object itself.
     */
    public UpdateSparkResourcePool withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the properties value.
     *
     * @param properties the properties value to set
     * @return the UpdateSparkResourcePool object itself.
     */
    public UpdateSparkResourcePool withProperties(UpdateSparkResourcePoolParameters properties) {
        this.properties = properties;
        return this;
    }

}
