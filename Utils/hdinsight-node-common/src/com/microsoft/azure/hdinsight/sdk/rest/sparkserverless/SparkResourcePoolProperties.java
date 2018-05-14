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

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

/**
 * Spark specific resource pool information.
 */
public class SparkResourcePoolProperties {
    /**
     * Name for the spark resource pool.
     */
    @NotNull
    @JsonProperty(value = "name", access = JsonProperty.Access.WRITE_ONLY)
    private String name;

    /**
     * Version of the template used while deploying the resource pool.
     */
    @NotNull
    @JsonProperty(value = "resourcePoolVersion", access = JsonProperty.Access.WRITE_ONLY)
    private String resourcePoolVersion;

    /**
     * Spark version to be deployed on the instances of the resource pool.
     */
    @NotNull
    @JsonProperty(value = "sparkVersion", access = JsonProperty.Access.WRITE_ONLY)
    private String sparkVersion;

    /**
     * Account name of the storage account (ADLS) where logs and history will
     * be stored.
     */
    @NotNull
    @JsonProperty(value = "userStorageAccount", access = JsonProperty.Access.WRITE_ONLY)
    private String userStorageAccount;

    /**
     * Definition of spark master and spark workers.
     */
    @NotNull
    @JsonProperty(value = "sparkResourceCollection", access = JsonProperty.Access.WRITE_ONLY)
    private List<SparkResourcePoolItemProperties> sparkResourceCollection;

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
     * Get the resourcePoolVersion value.
     *
     * @return the resourcePoolVersion value
     */
    @NotNull
    public String getResourcePoolVersion() {
        return this.resourcePoolVersion;
    }

    /**
     * Get the sparkVersion value.
     *
     * @return the sparkVersion value
     */
    @NotNull
    public String getSparkVersion() {
        return this.sparkVersion;
    }

    /**
     * Get the userStorageAccount value.
     *
     * @return the userStorageAccount value
     */
    @NotNull
    public String getUserStorageAccount() {
        return this.userStorageAccount;
    }

    /**
     * Get the sparkResourceCollection value.
     *
     * @return the sparkResourceCollection value
     */
    @NotNull
    public List<SparkResourcePoolItemProperties> getSparkResourceCollection() {
        return this.sparkResourceCollection;
    }

}
