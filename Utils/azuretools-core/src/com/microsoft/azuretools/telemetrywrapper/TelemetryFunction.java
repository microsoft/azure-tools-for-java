/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.telemetrywrapper;

@FunctionalInterface
public interface TelemetryFunction<T, R> {

    R apply(T t) throws Exception;

}
