/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.base;

public enum WebAppBaseState {
    RUNNING,
    STOPPED,
    UPDATING;

    private static WebAppBaseState[] copyOfValues = values();

    public static WebAppBaseState fromString(final String name) {
        for (final WebAppBaseState value: copyOfValues) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
