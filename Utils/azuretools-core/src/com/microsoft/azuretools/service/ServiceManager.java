/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.service;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.util.concurrent.ConcurrentHashMap;

/**
 * The ServiceManager is for parking the singleton service and its provider.
 */
public class ServiceManager {
    private static final ConcurrentHashMap<Class<?>, Object> serviceProviders = new ConcurrentHashMap<>();

    /**
     * Get the service provider for the specified abstract class or interface
     *
     * @param clazz the specified class or interface
     * @param <T> the specified class type
     * @return the service provider for the class, null for not found
     */
    @Nullable
    public static <T> T getServiceProvider(@NotNull Class<T> clazz) {
        try {
            return (T) serviceProviders.getOrDefault(clazz, null);
        } catch (ClassCastException ignored) {
            return null;
        }
    }

    /**
     * Set the service provider for the specified abstract class or interface
     *
     * @param clazz the specified class or interface
     * @param provider the provider
     * @param <T> the specified class type
     */
    public static <T> void setServiceProvider(@NotNull Class<T> clazz, @NotNull T provider) {
        serviceProviders.put(clazz, provider);
    }
}
