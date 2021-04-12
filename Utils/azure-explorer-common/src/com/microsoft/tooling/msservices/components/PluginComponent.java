/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.components;

public interface PluginComponent {
    public PluginSettings getSettings();

    public String getPluginId();
}
