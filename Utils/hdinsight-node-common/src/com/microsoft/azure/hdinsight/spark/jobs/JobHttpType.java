/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.jobs;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.Arrays;

public enum JobHttpType {

    SparkRest("spark"),
    YarnRest("yarn-rest"),
    YarnHistory("spark-history"),
    LivyBatchesRest("livy"),
    MultiTask("multi-task"),
    Action("action"),
    Unknown("unknown");

    private final String myType;
    JobHttpType(@NotNull String type) {
        this.myType = type;
    }

    public static JobHttpType convertTypeFromString(@NotNull final String type) {
        return Arrays.stream(JobHttpType.values())
                .filter(jobHttpType -> jobHttpType.myType.equalsIgnoreCase(type))
                .findFirst()
                .orElse(JobHttpType.Unknown);
    }

    @Override
    public String toString() {
        return myType;
    }
}
