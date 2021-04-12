/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RestUtil {
    public static  <T> List<T> getEmptyList(Class<T> tClass) {
        return Collections.unmodifiableList(new ArrayList<T>(0));
    }
}
