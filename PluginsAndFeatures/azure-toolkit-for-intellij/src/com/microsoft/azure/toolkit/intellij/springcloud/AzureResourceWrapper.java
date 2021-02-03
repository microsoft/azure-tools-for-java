/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.springcloud;


import com.microsoft.azure.ProxyResource;

public class AzureResourceWrapper {

    private static final String NEW_CREATED_PATTERN = "%s (New Created)";

    private final boolean isNewCreate;
    private final boolean fixedOption;
    private final String name;

    public AzureResourceWrapper(String name, boolean fixedOption, boolean isNewCreate) {
        this.fixedOption = fixedOption;
        this.isNewCreate = isNewCreate;
        this.name = name;
    }
    public AzureResourceWrapper(String name, boolean fixedOption) {
        this.fixedOption = fixedOption;
        this.isNewCreate = !fixedOption;
        this.name = name;
    }

    public AzureResourceWrapper(ProxyResource app) {
        this.fixedOption = false;
        this.isNewCreate = false;
        this.name = app.name();
    }

    public boolean isNewCreate() {
        return isNewCreate;
    }

    public boolean isFixedOption() {
        return fixedOption;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return isNewCreate ? String.format(NEW_CREATED_PATTERN, name) : name;
    }
}
