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

import org.joda.time.DateTime;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.rest.serializer.JsonFlatten;

/**
 * Big Data pool.
 * A Big Data pool.
 */
@JsonFlatten
public class BigDataPoolResourceInfo extends TrackedResource {
    /**
     * The state of the Big Data pool.
     */
    @JsonProperty(value = "properties.provisioningState")
    private String provisioningState;

    /**
     * Auto-scaling properties.
     */
    @JsonProperty(value = "properties.autoScale")
    private AutoScaleProperties autoScale;

    /**
     * The time when the Big Data pool was created.
     */
    @JsonProperty(value = "properties.creationDate")
    private DateTime creationDate;

    /**
     * Auto-pausing properties.
     */
    @JsonProperty(value = "properties.autoPause")
    private AutoPauseProperties autoPause;

    /**
     * The Spark events folder.
     */
    @JsonProperty(value = "properties.sparkEventsFolder")
    private String sparkEventsFolder;

    /**
     * The number of nodes in the Big Data pool.
     */
    @JsonProperty(value = "properties.nodeCount")
    private Integer nodeCount;

    /**
     * Library version requirements.
     */
    @JsonProperty(value = "properties.libraryRequirements")
    private LibraryRequirements libraryRequirements;

    /**
     * The Apache Spark version.
     */
    @JsonProperty(value = "properties.sparkVersion")
    private String sparkVersion;

    /**
     * The default folder where Spark logs will be written.
     */
    @JsonProperty(value = "properties.defaultSparkLogFolder")
    private String defaultSparkLogFolder;

    /**
     * The level of compute power that each node in the Big Data pool has. Possible values include: 'None', 'Small',
     * 'Medium', 'Large'.
     */
    @JsonProperty(value = "properties.nodeSize")
    private NodeSize nodeSize;

    /**
     * The kind of nodes that the Big Data pool provides. Possible values include: 'None', 'MemoryOptimized'.
     */
    @JsonProperty(value = "properties.nodeSizeFamily")
    private NodeSizeFamily nodeSizeFamily;

    /**
     * Get the state of the Big Data pool.
     *
     * @return the provisioningState value
     */
    public String provisioningState() {
        return this.provisioningState;
    }

    /**
     * Set the state of the Big Data pool.
     *
     * @param provisioningState the provisioningState value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withProvisioningState(String provisioningState) {
        this.provisioningState = provisioningState;
        return this;
    }

    /**
     * Get auto-scaling properties.
     *
     * @return the autoScale value
     */
    public AutoScaleProperties autoScale() {
        return this.autoScale;
    }

    /**
     * Set auto-scaling properties.
     *
     * @param autoScale the autoScale value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withAutoScale(AutoScaleProperties autoScale) {
        this.autoScale = autoScale;
        return this;
    }

    /**
     * Get the time when the Big Data pool was created.
     *
     * @return the creationDate value
     */
    public DateTime creationDate() {
        return this.creationDate;
    }

    /**
     * Set the time when the Big Data pool was created.
     *
     * @param creationDate the creationDate value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withCreationDate(DateTime creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    /**
     * Get auto-pausing properties.
     *
     * @return the autoPause value
     */
    public AutoPauseProperties autoPause() {
        return this.autoPause;
    }

    /**
     * Set auto-pausing properties.
     *
     * @param autoPause the autoPause value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withAutoPause(AutoPauseProperties autoPause) {
        this.autoPause = autoPause;
        return this;
    }

    /**
     * Get the Spark events folder.
     *
     * @return the sparkEventsFolder value
     */
    public String sparkEventsFolder() {
        return this.sparkEventsFolder;
    }

    /**
     * Set the Spark events folder.
     *
     * @param sparkEventsFolder the sparkEventsFolder value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withSparkEventsFolder(String sparkEventsFolder) {
        this.sparkEventsFolder = sparkEventsFolder;
        return this;
    }

    /**
     * Get the number of nodes in the Big Data pool.
     *
     * @return the nodeCount value
     */
    public Integer nodeCount() {
        return this.nodeCount;
    }

    /**
     * Set the number of nodes in the Big Data pool.
     *
     * @param nodeCount the nodeCount value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
        return this;
    }

    /**
     * Get library version requirements.
     *
     * @return the libraryRequirements value
     */
    public LibraryRequirements libraryRequirements() {
        return this.libraryRequirements;
    }

    /**
     * Set library version requirements.
     *
     * @param libraryRequirements the libraryRequirements value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withLibraryRequirements(LibraryRequirements libraryRequirements) {
        this.libraryRequirements = libraryRequirements;
        return this;
    }

    /**
     * Get the Apache Spark version.
     *
     * @return the sparkVersion value
     */
    public String sparkVersion() {
        return this.sparkVersion;
    }

    /**
     * Set the Apache Spark version.
     *
     * @param sparkVersion the sparkVersion value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withSparkVersion(String sparkVersion) {
        this.sparkVersion = sparkVersion;
        return this;
    }

    /**
     * Get the default folder where Spark logs will be written.
     *
     * @return the defaultSparkLogFolder value
     */
    public String defaultSparkLogFolder() {
        return this.defaultSparkLogFolder;
    }

    /**
     * Set the default folder where Spark logs will be written.
     *
     * @param defaultSparkLogFolder the defaultSparkLogFolder value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withDefaultSparkLogFolder(String defaultSparkLogFolder) {
        this.defaultSparkLogFolder = defaultSparkLogFolder;
        return this;
    }

    /**
     * Get the level of compute power that each node in the Big Data pool has. Possible values include: 'None', 'Small', 'Medium', 'Large'.
     *
     * @return the nodeSize value
     */
    public NodeSize nodeSize() {
        return this.nodeSize;
    }

    /**
     * Set the level of compute power that each node in the Big Data pool has. Possible values include: 'None', 'Small', 'Medium', 'Large'.
     *
     * @param nodeSize the nodeSize value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withNodeSize(NodeSize nodeSize) {
        this.nodeSize = nodeSize;
        return this;
    }

    /**
     * Get the kind of nodes that the Big Data pool provides. Possible values include: 'None', 'MemoryOptimized'.
     *
     * @return the nodeSizeFamily value
     */
    public NodeSizeFamily nodeSizeFamily() {
        return this.nodeSizeFamily;
    }

    /**
     * Set the kind of nodes that the Big Data pool provides. Possible values include: 'None', 'MemoryOptimized'.
     *
     * @param nodeSizeFamily the nodeSizeFamily value to set
     * @return the BigDataPoolResourceInfo object itself.
     */
    public BigDataPoolResourceInfo withNodeSizeFamily(NodeSizeFamily nodeSizeFamily) {
        this.nodeSizeFamily = nodeSizeFamily;
        return this;
    }

}
