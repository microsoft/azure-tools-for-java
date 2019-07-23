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

package com.microsoft.azure.hdinsight.sdk.rest.arcadia.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Workspace connectivity endpoints.
 */
public class ConnectivityEndpoints {
    /**
     * Web endpoint.
     */
    @JsonProperty(value = "web")
    private String web;

    /**
     * Spark endpoint.
     */
    @JsonProperty(value = "spark")
    private String spark;

    /**
     * Artifacts endpoint.
     */
    @JsonProperty(value = "artifacts")
    private String artifacts;

    /**
     * SQL endpoint.
     */
    @JsonProperty(value = "sql")
    private String sql;

    /**
     * Get web endpoint.
     *
     * @return the web value
     */
    public String web() {
        return this.web;
    }

    /**
     * Set web endpoint.
     *
     * @param web the web value to set
     * @return the ConnectivityEndpoints object itself.
     */
    public ConnectivityEndpoints withWeb(String web) {
        this.web = web;
        return this;
    }

    /**
     * Get spark endpoint.
     *
     * @return the spark value
     */
    public String spark() {
        return this.spark;
    }

    /**
     * Set spark endpoint.
     *
     * @param spark the spark value to set
     * @return the ConnectivityEndpoints object itself.
     */
    public ConnectivityEndpoints withSpark(String spark) {
        this.spark = spark;
        return this;
    }

    /**
     * Get artifacts endpoint.
     *
     * @return the artifacts value
     */
    public String artifacts() {
        return this.artifacts;
    }

    /**
     * Set artifacts endpoint.
     *
     * @param artifacts the artifacts value to set
     * @return the ConnectivityEndpoints object itself.
     */
    public ConnectivityEndpoints withArtifacts(String artifacts) {
        this.artifacts = artifacts;
        return this;
    }

    /**
     * Get sQL endpoint.
     *
     * @return the sql value
     */
    public String sql() {
        return this.sql;
    }

    /**
     * Set sQL endpoint.
     *
     * @param sql the sql value to set
     * @return the ConnectivityEndpoints object itself.
     */
    public ConnectivityEndpoints withSql(String sql) {
        this.sql = sql;
        return this;
    }

}
