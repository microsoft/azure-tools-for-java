/*
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.sdk.rest.sparkserverless;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

/**
 * The parameters used to submit a new Data Lake Analytics resource pool
 * creation request.
 */
public class CreateSparkResourcePool {
    /**
     * the friendly name of the resource pool to submit.
     */
    @NotNull
    @JsonProperty(value = "name", required = true)
    private String name;

    /**
     * The spark resource pool specific properties.
     */
    @NotNull
    @JsonProperty(value = "properties", required = true)
    private CreateSparkResourcePoolParameters properties;

    /**
     * Get the name value.
     *
     * @return the name value
     */
    @NotNull
    public String getName() {
        return this.name;
    }

    /**
     * Set the name value.
     *
     * @param name the name value to set
     */
    public void setName(@NotNull String name) {
        this.name = name;
    }

    /**
     * Get the properties value.
     *
     * @return the properties value
     */
    @NotNull
    public CreateSparkResourcePoolParameters getProperties() {
        return this.properties;
    }

    /**
     * Set the properties value.
     *
     * @param properties the properties value to set
     */
    public void setProperties(@NotNull CreateSparkResourcePoolParameters properties) {
        this.properties = properties;
    }

}
