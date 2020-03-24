/*
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
