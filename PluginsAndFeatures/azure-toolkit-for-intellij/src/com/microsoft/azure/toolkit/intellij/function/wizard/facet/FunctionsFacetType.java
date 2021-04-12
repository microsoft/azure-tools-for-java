/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.wizard.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FunctionsFacetType extends FacetType<FunctionsFacet, FunctionsFacetConfiguration> {
    FunctionsFacetType() {
        super(FunctionsFacet.FACET_TYPE_ID, "AzureFunctions", "AzureFunctions");
    }

    @Override
    public FunctionsFacetConfiguration createDefaultConfiguration() {
        return null;
    }

    @Override
    public FunctionsFacet createFacet(@NotNull Module module, String name,
        @NotNull FunctionsFacetConfiguration configuration, @Nullable Facet underlyingFacet) {
        return null;
    }

    @Override
    public boolean isSuitableModuleType(ModuleType moduleType) {
        return false;
    }
}
