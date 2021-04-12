/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.common;

import java.util.HashMap;
import java.util.Map;

public class SparkConfigures extends HashMap<String, Object> {
    public SparkConfigures(Map<String, ?> m) {
        super(m);
    }

    @SuppressWarnings("unchecked")
    public SparkConfigures(Object o) {
        this((Map<String, Object>) o);
    }

    public SparkConfigures() {
        super();
    }
}
