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

package com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Big Data pool.
 * A Big Data pool.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BigDataPoolResourceInfo extends TrackedResource {
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Properties {
        /**
         * The state of the Big Data pool.
         */
        @JsonProperty(value = "provisioningState")
        private String provisioningState;

        /**
         * Auto-scaling properties.
         */
        @JsonProperty(value = "autoScale")
        private AutoScaleProperties autoScale;

        /**
         * The time when the Big Data pool was created.
         */
        @JsonProperty(value = "creationDate")
        private String creationDate;

        /**
         * Auto-pausing properties.
         */
        @JsonProperty(value = "autoPause")
        private AutoPauseProperties autoPause;

        /**
         * The Spark events folder.
         */
        @JsonProperty(value = "sparkEventsFolder")
        private String sparkEventsFolder;

        /**
         * The number of nodes in the Big Data pool.
         */
        @JsonProperty(value = "nodeCount")
        private Integer nodeCount;

        /**
         * Library version requirements.
         */
        @JsonProperty(value = "libraryRequirements")
        private LibraryRequirements libraryRequirements;

        /**
         * The Apache Spark version.
         */
        @JsonProperty(value = "sparkVersion")
        private String sparkVersion;

        /**
         * The default folder where Spark logs will be written.
         */
        @JsonProperty(value = "defaultSparkLogFolder")
        private String defaultSparkLogFolder;

        /**
         * The level of compute power that each node in the Big Data pool has. Possible values include: 'None', 'Small',
         * 'Medium', 'Large'.
         */
        @JsonProperty(value = "nodeSize")
        private NodeSize nodeSize;

        /**
         * The kind of nodes that the Big Data pool provides. Possible values include: 'None', 'MemoryOptimized'.
         */
        @JsonProperty(value = "nodeSizeFamily")
        private NodeSizeFamily nodeSizeFamily;
    }

    @JsonProperty(value = "properties", access = JsonProperty.Access.WRITE_ONLY)
    private Properties properties;

    public Properties properties() {
        return this.properties == null ? null : this.properties;
    }

    /**
     * Get the state of the Big Data pool.
     *
     * @return the provisioningState value
     */
    public String provisioningState() {
        return this.properties == null ? null : this.properties.provisioningState;
    }

    /**
     * Set the state of the Big Data pool.
     *
     * @param provisioningState the provisioningState value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withProvisioningState(String provisioningState) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.provisioningState = provisioningState;
        return this;
    }

    /**
     * Get auto-scaling properties.
     *
     * @return the autoScale value
     */
    public AutoScaleProperties autoScale() {
        return this.properties == null ? null : this.properties.autoScale;
    }

    /**
     * Set auto-scaling properties.
     *
     * @param autoScale the autoScale value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withAutoScale(AutoScaleProperties autoScale) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.autoScale = autoScale;
        return this;
    }

    /**
     * Get the time when the Big Data pool was created.
     *
     * @return the creationDate value
     */
    public String creationDate() {
        return this.properties == null ? null : this.properties.creationDate;
    }

    /**
     * Set the time when the Big Data pool was created.
     *
     * @param creationDate the creationDate value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withCreationDate(String creationDate) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.creationDate = creationDate;
        return this;
    }

    /**
     * Get auto-pausing properties.
     *
     * @return the autoPause value
     */
    public AutoPauseProperties autoPause() {
        return this.properties == null ? null : this.properties.autoPause;
    }

    /**
     * Set auto-pausing properties.
     *
     * @param autoPause the autoPause value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withAutoPause(AutoPauseProperties autoPause) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.autoPause = autoPause;
        return this;
    }

    /**
     * Get the Spark events folder.
     *
     * @return the sparkEventsFolder value
     */
    public String sparkEventsFolder() {
        return this.properties == null ? null : this.properties.sparkEventsFolder;
    }

    /**
     * Set the Spark events folder.
     *
     * @param sparkEventsFolder the sparkEventsFolder value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withSparkEventsFolder(String sparkEventsFolder) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.sparkEventsFolder = sparkEventsFolder;
        return this;
    }

    /**
     * Get the number of nodes in the Big Data pool.
     *
     * @return the nodeCount value
     */
    public Integer nodeCount() {
        return this.properties == null ? null : this.properties.nodeCount;
    }

    /**
     * Set the number of nodes in the Big Data pool.
     *
     * @param nodeCount the nodeCount value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withNodeCount(Integer nodeCount) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.nodeCount = nodeCount;
        return this;
    }

    /**
     * Get library version requirements.
     *
     * @return the libraryRequirements value
     */
    public LibraryRequirements libraryRequirements() {
        return this.properties == null ? null : this.properties.libraryRequirements;
    }

    /**
     * Set library version requirements.
     *
     * @param libraryRequirements the libraryRequirements value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withLibraryRequirements(LibraryRequirements libraryRequirements) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.libraryRequirements = libraryRequirements;
        return this;
    }

    /**
     * Get the Apache Spark version.
     *
     * @return the sparkVersion value
     */
    public String sparkVersion() {
        return this.properties == null ? null : this.properties.sparkVersion;
    }

    /**
     * Set the Apache Spark version.
     *
     * @param sparkVersion the sparkVersion value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withSparkVersion(String sparkVersion) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.sparkVersion = sparkVersion;
        return this;
    }

    /**
     * Get the default folder where Spark logs will be written.
     *
     * @return the defaultSparkLogFolder value
     */
    public String defaultSparkLogFolder() {
        return this.properties == null ? null : this.properties.defaultSparkLogFolder;
    }

    /**
     * Set the default folder where Spark logs will be written.
     *
     * @param defaultSparkLogFolder the defaultSparkLogFolder value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withDefaultSparkLogFolder(String defaultSparkLogFolder) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.defaultSparkLogFolder = defaultSparkLogFolder;
        return this;
    }

    /**
     * Get the level of compute power that each node in the Big Data pool has. Possible values include: 'None', 'Small', 'Medium', 'Large'.
     *
     * @return the nodeSize value
     */
    public NodeSize nodeSize() {
        return this.properties == null ? null : this.properties.nodeSize;
    }

    /**
     * Set the level of compute power that each node in the Big Data pool has. Possible values include: 'None', 'Small', 'Medium', 'Large'.
     *
     * @param nodeSize the nodeSize value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withNodeSize(NodeSize nodeSize) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.nodeSize = nodeSize;
        return this;
    }

    /**
     * Get the kind of nodes that the Big Data pool provides. Possible values include: 'None', 'MemoryOptimized'.
     *
     * @return the nodeSizeFamily value
     */
    public NodeSizeFamily nodeSizeFamily() {
        return this.properties == null ? null : this.properties.nodeSizeFamily;
    }

    /**
     * Set the kind of nodes that the Big Data pool provides. Possible values include: 'None', 'MemoryOptimized'.
     *
     * @param nodeSizeFamily the nodeSizeFamily value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withNodeSizeFamily(NodeSizeFamily nodeSizeFamily) {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        this.properties.nodeSizeFamily = nodeSizeFamily;
        return this;
    }

}
