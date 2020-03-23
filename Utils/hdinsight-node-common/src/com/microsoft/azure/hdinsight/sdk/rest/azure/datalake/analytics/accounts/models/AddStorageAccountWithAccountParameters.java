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
 * The parameters used to add a new Azure Storage account while creating a new
 * Data Lake Analytics account.
 */
@JsonFlatten
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddStorageAccountWithAccountParameters {
    /**
     * The unique name of the Azure Storage account to add.
     */
    @JsonProperty(value = "name", required = true)
    private String name;

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Properties {
        /**
         * The access key associated with this Azure Storage account that will be
         * used to connect to it.
         */
        @JsonProperty(value = "accessKey", required = true)
        private String accessKey;

        /**
         * The optional suffix for the storage account.
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
     * @return the AddStorageAccountWithAccountParameters object itself.
     */
    public AddStorageAccountWithAccountParameters withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get the accessKey value.
     *
     * @return the accessKey value
     */
    public String accessKey() {
        return this.properties == null ? null : properties.accessKey;
    }

    /**
     * Set the accessKey value.
     *
     * @param accessKey the accessKey value to set
     * @return the AddStorageAccountWithAccountParameters object itself.
     */
    public AddStorageAccountWithAccountParameters withAccessKey(String accessKey) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.accessKey = accessKey;
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
     * @return the AddStorageAccountWithAccountParameters object itself.
     */
    public AddStorageAccountWithAccountParameters withSuffix(String suffix) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.suffix = suffix;
        return this;
    }

}
