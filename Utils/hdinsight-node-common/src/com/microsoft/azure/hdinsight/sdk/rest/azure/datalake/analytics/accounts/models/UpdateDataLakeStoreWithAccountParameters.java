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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.rest.serializer.JsonFlatten;

/**
 * The parameters used to update a Data Lake Store account while updating a
 * Data Lake Analytics account.
 */
@JsonFlatten
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateDataLakeStoreWithAccountParameters {
    /**
     * The unique name of the Data Lake Store account to update.
     */
    @JsonProperty(value = "name", required = true)
    private String name;

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Properties {
        /**
         * The optional suffix for the Data Lake Store account.
         */
        @JsonProperty(value = "suffix")
        private String suffix;
    }

    /**
     * The properties
     */
    @JsonProperty(value = "properties")
    private Properties properties;

    /**
     * Get the name value.
     *
     * @return the name value
     */
    public String name() {
        return this.name;
    }

    /**
     * Set the name value.
     *
     * @param name the name value to set
     * @return the UpdateDataLakeStoreWithAccountParameters object itself.
     */
    public UpdateDataLakeStoreWithAccountParameters withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get the suffix value.
     *
     * @return the suffix value
     */
    public String suffix() {
        return this.properties == null ? null : properties.suffix;
    }

    /**
     * Set the suffix value.
     *
     * @param suffix the suffix value to set
     * @return the UpdateDataLakeStoreWithAccountParameters object itself.
     */
    public UpdateDataLakeStoreWithAccountParameters withSuffix(String suffix) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.suffix = suffix;
        return this;
    }

}
