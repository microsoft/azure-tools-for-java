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

package com.microsoft.azure.hdinsight.sdk.rest.azure.projectarcadia.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The properties of a spark compute.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SparkCompute extends TrackedResource {
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Properties {
        /**
         * The number of nodes in a spark compute.
         */
        @JsonProperty(value = "nodeCount")
        private Integer nodeCount;

        /**
         * The node size. Possible values include: 'Small', 'Medium', 'Large'.
         */
        @JsonProperty(value = "nodeSize")
        private SparkComputeNodeSize nodeSize;

        /**
         * The node size family. Possible values include: 'MemoryOptimized'.
         */
        @JsonProperty(value = "nodeSizeFamily")
        private SparkComputeNodeSizeFamily nodeSizeFamily;

        /**
         * The autoscale properties.
         */
        @JsonProperty(value = "autoScale")
        private AutoScaleProperties autoScale;

        /**
         * The autoPause properties.
         */
        @JsonProperty(value = "autoPause")
        private AutoPauseProperties autoPause;

        /**
         * The spark version.
         */
        @JsonProperty(value = "sparkVersion")
        private String sparkVersion;

        /**
         * The folder in workspace storage account where spark events will be stored.
         */
        @JsonProperty(value = "sparkEventsFolder")
        private String sparkEventsFolder;

        /**
         * The folder in workspace storage account where spark logs will be stored.
         */
        @JsonProperty(value = "defaultSparkLogFolder")
        private String defaultSparkLogFolder;

        /**
         * The spark compute provisioning state. Possible values include: 'Provisioning', 'Succeeded', 'Failed',
         * 'Deleting'.
         */
        @JsonProperty(value = "provisioningState", access = JsonProperty.Access.WRITE_ONLY)
        private SparkComputeProvisioningState provisioningState;

        /**
         * The spark compute state.
         */
        @JsonProperty(value = "status", access = JsonProperty.Access.WRITE_ONLY)
        private String status;
    }

    @JsonProperty(value = "properties", access = JsonProperty.Access.WRITE_ONLY)
    private Properties properties;

    public Properties properties() {
        return this.properties;
    }

    /**
     * Get the number of nodes in a spark compute.
     *
     * @return the nodeCount value
     */
    public Integer nodeCount() {
        return this.properties == null ? null : this.properties.nodeCount;
    }

    /**
     * Set the number of nodes in a spark compute.
     *
     * @param nodeCount the nodeCount value to set
     * @return the SparkCompute object itself.
     */
    public SparkCompute withNodeCount(Integer nodeCount) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.nodeCount = nodeCount;
        return this;
    }

    /**
     * Get the node size. Possible values include: 'Small', 'Medium', 'Large'.
     *
     * @return the nodeSize value
     */
    public SparkComputeNodeSize nodeSize() {
        return this.properties == null ? null : this.properties.nodeSize;
    }

    /**
     * Set the node size. Possible values include: 'Small', 'Medium', 'Large'.
     *
     * @param nodeSize the nodeSize value to set
     * @return the SparkCompute object itself.
     */
    public SparkCompute withNodeSize(SparkComputeNodeSize nodeSize) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.nodeSize = nodeSize;
        return this;
    }

    /**
     * Get the node size family. Possible values include: 'MemoryOptimized'.
     *
     * @return the nodeSizeFamily value
     */
    public SparkComputeNodeSizeFamily nodeSizeFamily() {
        return this.properties == null ? null : this.properties.nodeSizeFamily;
    }

    /**
     * Set the node size family. Possible values include: 'MemoryOptimized'.
     *
     * @param nodeSizeFamily the nodeSizeFamily value to set
     * @return the SparkCompute object itself.
     */
    public SparkCompute withNodeSizeFamily(SparkComputeNodeSizeFamily nodeSizeFamily) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.nodeSizeFamily = nodeSizeFamily;
        return this;
    }

    /**
     * Get the autoscale properties.
     *
     * @return the autoScale value
     */
    public AutoScaleProperties autoScale() {
        return this.properties == null ? null : this.properties.autoScale;
    }

    /**
     * Set the autoscale properties.
     *
     * @param autoScale the autoScale value to set
     * @return the SparkCompute object itself.
     */
    public SparkCompute withAutoScale(AutoScaleProperties autoScale) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.autoScale = autoScale;
        return this;
    }

    /**
     * Get the autoPause properties.
     *
     * @return the autoPause value
     */
    public AutoPauseProperties autoPause() {
        return this.properties == null ? null : this.properties.autoPause;
    }

    /**
     * Set the autoPause properties.
     *
     * @param autoPause the autoPause value to set
     * @return the SparkCompute object itself.
     */
    public SparkCompute withAutoPause(AutoPauseProperties autoPause) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.autoPause = autoPause;
        return this;
    }

    /**
     * Get the spark version.
     *
     * @return the sparkVersion value
     */
    public String sparkVersion() {
        return this.properties == null ? null : this.properties.sparkVersion;
    }

    /**
     * Set the spark version.
     *
     * @param sparkVersion the sparkVersion value to set
     * @return the SparkCompute object itself.
     */
    public SparkCompute withSparkVersion(String sparkVersion) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.sparkVersion = sparkVersion;
        return this;
    }

    /**
     * Get the folder in workspace storage account where spark events will be stored.
     *
     * @return the sparkEventsFolder value
     */
    public String sparkEventsFolder() {
        return this.properties == null ? null : this.properties.sparkEventsFolder;
    }

    /**
     * Set the folder in workspace storage account where spark events will be stored.
     *
     * @param sparkEventsFolder the sparkEventsFolder value to set
     * @return the SparkCompute object itself.
     */
    public SparkCompute withSparkEventsFolder(String sparkEventsFolder) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.sparkEventsFolder = sparkEventsFolder;
        return this;
    }

    /**
     * Get the folder in workspace storage account where spark logs will be stored.
     *
     * @return the defaultSparkLogFolder value
     */
    public String defaultSparkLogFolder() {
        return this.properties == null ? null : this.properties.defaultSparkLogFolder;
    }

    /**
     * Set the folder in workspace storage account where spark logs will be stored.
     *
     * @param defaultSparkLogFolder the defaultSparkLogFolder value to set
     * @return the SparkCompute object itself.
     */
    public SparkCompute withDefaultSparkLogFolder(String defaultSparkLogFolder) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.defaultSparkLogFolder = defaultSparkLogFolder;
        return this;
    }

    /**
     * Get the spark compute provisioning state. Possible values include: 'Provisioning', 'Succeeded', 'Failed', 'Deleting'.
     *
     * @return the provisioningState value
     */
    public SparkComputeProvisioningState provisioningState() {
        return this.properties == null ? null : this.properties.provisioningState;
    }

    /**
     * Get the spark compute state.
     *
     * @return the status value
     */
    public String status() {
        return this.properties == null ? null : this.properties.status;
    }
}
