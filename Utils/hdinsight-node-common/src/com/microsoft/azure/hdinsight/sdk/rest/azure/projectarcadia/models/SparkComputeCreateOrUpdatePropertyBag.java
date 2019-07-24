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
 * The spark compute create or update property bag.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SparkComputeCreateOrUpdatePropertyBag {
    /**
     * The number of nodes in a spark compute.
     */
    @JsonProperty(value = "nodeCount")
    private Integer nodeCount;

    /**
     * The node size. Possible values include: 'Small', 'Medium', 'Large'.
     */
    @JsonProperty(value = "nodeSize")
    private SparkComputeNodeSizeFamily nodeSize;

    /**
     * The node size family. Possible values include: 'MemoryOptimized'.
     */
    @JsonProperty(value = "nodeSizeFamily")
    private SparkComputeNodeSize nodeSizeFamily;

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
     * Get the number of nodes in a spark compute.
     *
     * @return the nodeCount value
     */
    public Integer nodeCount() {
        return this.nodeCount;
    }

    /**
     * Set the number of nodes in a spark compute.
     *
     * @param nodeCount the nodeCount value to set
     * @return the SparkComputeCreateOrUpdatePropertyBag object itself.
     */
    public SparkComputeCreateOrUpdatePropertyBag withNodeCount(Integer nodeCount) {
        this.nodeCount = nodeCount;
        return this;
    }

    /**
     * Get the node size. Possible values include: 'Small', 'Medium', 'Large'.
     *
     * @return the nodeSize value
     */
    public SparkComputeNodeSizeFamily nodeSize() {
        return this.nodeSize;
    }

    /**
     * Set the node size. Possible values include: 'Small', 'Medium', 'Large'.
     *
     * @param nodeSize the nodeSize value to set
     * @return the SparkComputeCreateOrUpdatePropertyBag object itself.
     */
    public SparkComputeCreateOrUpdatePropertyBag withNodeSize(SparkComputeNodeSizeFamily nodeSize) {
        this.nodeSize = nodeSize;
        return this;
    }

    /**
     * Get the node size family. Possible values include: 'MemoryOptimized'.
     *
     * @return the nodeSizeFamily value
     */
    public SparkComputeNodeSize nodeSizeFamily() {
        return this.nodeSizeFamily;
    }

    /**
     * Set the node size family. Possible values include: 'MemoryOptimized'.
     *
     * @param nodeSizeFamily the nodeSizeFamily value to set
     * @return the SparkComputeCreateOrUpdatePropertyBag object itself.
     */
    public SparkComputeCreateOrUpdatePropertyBag withNodeSizeFamily(SparkComputeNodeSize nodeSizeFamily) {
        this.nodeSizeFamily = nodeSizeFamily;
        return this;
    }

    /**
     * Get the autoscale properties.
     *
     * @return the autoScale value
     */
    public AutoScaleProperties autoScale() {
        return this.autoScale;
    }

    /**
     * Set the autoscale properties.
     *
     * @param autoScale the autoScale value to set
     * @return the SparkComputeCreateOrUpdatePropertyBag object itself.
     */
    public SparkComputeCreateOrUpdatePropertyBag withAutoScale(AutoScaleProperties autoScale) {
        this.autoScale = autoScale;
        return this;
    }

    /**
     * Get the autoPause properties.
     *
     * @return the autoPause value
     */
    public AutoPauseProperties autoPause() {
        return this.autoPause;
    }

    /**
     * Set the autoPause properties.
     *
     * @param autoPause the autoPause value to set
     * @return the SparkComputeCreateOrUpdatePropertyBag object itself.
     */
    public SparkComputeCreateOrUpdatePropertyBag withAutoPause(AutoPauseProperties autoPause) {
        this.autoPause = autoPause;
        return this;
    }

    /**
     * Get the spark version.
     *
     * @return the sparkVersion value
     */
    public String sparkVersion() {
        return this.sparkVersion;
    }

    /**
     * Set the spark version.
     *
     * @param sparkVersion the sparkVersion value to set
     * @return the SparkComputeCreateOrUpdatePropertyBag object itself.
     */
    public SparkComputeCreateOrUpdatePropertyBag withSparkVersion(String sparkVersion) {
        this.sparkVersion = sparkVersion;
        return this;
    }

    /**
     * Get the folder in workspace storage account where spark events will be stored.
     *
     * @return the sparkEventsFolder value
     */
    public String sparkEventsFolder() {
        return this.sparkEventsFolder;
    }

    /**
     * Set the folder in workspace storage account where spark events will be stored.
     *
     * @param sparkEventsFolder the sparkEventsFolder value to set
     * @return the SparkComputeCreateOrUpdatePropertyBag object itself.
     */
    public SparkComputeCreateOrUpdatePropertyBag withSparkEventsFolder(String sparkEventsFolder) {
        this.sparkEventsFolder = sparkEventsFolder;
        return this;
    }

    /**
     * Get the folder in workspace storage account where spark logs will be stored.
     *
     * @return the defaultSparkLogFolder value
     */
    public String defaultSparkLogFolder() {
        return this.defaultSparkLogFolder;
    }

    /**
     * Set the folder in workspace storage account where spark logs will be stored.
     *
     * @param defaultSparkLogFolder the defaultSparkLogFolder value to set
     * @return the SparkComputeCreateOrUpdatePropertyBag object itself.
     */
    public SparkComputeCreateOrUpdatePropertyBag withDefaultSparkLogFolder(String defaultSparkLogFolder) {
        this.defaultSparkLogFolder = defaultSparkLogFolder;
        return this;
    }

}
