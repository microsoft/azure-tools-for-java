/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.jobs.framework;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;

public enum HttpRequestType {
    SparkRest("spark"),
    YarnRest("yarn"),
    YarnHistory("yarn-history"),
    LivyBatchesRest("livy"),
    MultiTask("multi-task"),
    None("NONE");

    private final String type;

    private HttpRequestType(@NotNull String type) {
        this.type = type;
    }

    public String getText() {
        return type;
    }

    public static HttpRequestType fromString(String text) {
        if (StringHelper.isNullOrWhiteSpace(text)) {
            return None;
        }

        for (HttpRequestType element : HttpRequestType.values()) {
            if (element.type.equalsIgnoreCase(text)) {
                return element;
            }
        }
        return None;
    }
}
