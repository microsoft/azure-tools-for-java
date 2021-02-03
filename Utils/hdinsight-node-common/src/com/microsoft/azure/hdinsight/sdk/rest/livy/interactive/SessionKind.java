/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.livy.interactive;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

public enum SessionKind {
    SPARK,          // Interactive Scala Spark session
    PYSPARK,        // Interactive Python 2 Spark session
    PYSPARK3,       // Interactive Python 3 Spark session
    SPARKR          // Interactive R Spark session
    ;

    @JsonValue
    public String getKind() {
        return name().toLowerCase();
    }

    /**
     * To convert the string to SessionKind type with case insensitive
     *
     * @param kind Session kind string
     * @return SessionKind parsed
     * @throws IllegalArgumentException for no enum value matched
     */
    @JsonCreator
    static public SessionKind parse(@NotNull String kind) {
        return SessionKind.valueOf(kind.trim().toUpperCase());
    }
}
