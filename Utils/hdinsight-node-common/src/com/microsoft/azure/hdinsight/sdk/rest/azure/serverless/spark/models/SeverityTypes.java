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
 * Defines values for SeverityTypes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SeverityTypes extends ExpandableStringEnum<SeverityTypes> {
    /** Static value Warning for SeverityTypes. */
    public static final SeverityTypes WARNING = fromString("Warning");

    /** Static value Error for SeverityTypes. */
    public static final SeverityTypes ERROR = fromString("Error");

    /** Static value Info for SeverityTypes. */
    public static final SeverityTypes INFO = fromString("Info");

    /** Static value SevereWarning for SeverityTypes. */
    public static final SeverityTypes SEVERE_WARNING = fromString("SevereWarning");

    /** Static value Deprecated for SeverityTypes. */
    public static final SeverityTypes DEPRECATED = fromString("Deprecated");

    /** Static value UserWarning for SeverityTypes. */
    public static final SeverityTypes USER_WARNING = fromString("UserWarning");

    /**
     * Creates or finds a SeverityTypes from its string representation.
     * @param name a name to look for
     * @return the corresponding SeverityTypes
     */
    @JsonCreator
    public static SeverityTypes fromString(String name) {
        return fromString(name, SeverityTypes.class);
    }

    /**
     * @return known SeverityTypes values
     */
    public static Collection<SeverityTypes> values() {
        return values(SeverityTypes.class);
    }
}
