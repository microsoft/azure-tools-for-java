/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import java.util.Collection;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microsoft.rest.ExpandableStringEnum;

/**
 * Defines values for SparkNodeType.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SparkNodeType extends ExpandableStringEnum<SparkNodeType> {
    /** Static value SparkMaster for SparkNodeType. */
    public static final SparkNodeType SPARK_MASTER = fromString("SparkMaster");

    /** Static value SparkWorker for SparkNodeType. */
    public static final SparkNodeType SPARK_WORKER = fromString("SparkWorker");

    /**
     * Creates or finds a SparkNodeType from its string representation.
     * @param name a name to look for
     * @return the corresponding SparkNodeType
     */
    @JsonCreator
    public static SparkNodeType fromString(String name) {
        return fromString(name, SparkNodeType.class);
    }

    /**
     * @return known SparkNodeType values
     */
    public static Collection<SparkNodeType> values() {
        return values(SparkNodeType.class);
    }
}
