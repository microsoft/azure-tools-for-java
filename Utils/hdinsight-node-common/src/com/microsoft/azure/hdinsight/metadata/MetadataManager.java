/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.metadata;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.HashMap;
import java.util.Map;

public class MetadataManager {
    private static Map<Class<? extends MetaDataService>, MetaDataService> serviceMap = new HashMap<>();

    public static <T> T getService(@NotNull Class<T> serviceClass) {
        return (T) serviceMap.get(serviceClass);
    }

    public static void register( MetaDataService service) {
        serviceMap.put(service.getClass(), service);
    }
}
