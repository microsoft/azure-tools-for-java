/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.wizard.facet;

import com.intellij.facet.Facet;
import com.intellij.facet.FacetType;
import com.intellij.facet.FacetTypeId;
import com.intellij.openapi.module.Module;

public class FunctionsFacet extends Facet<FunctionsFacetConfiguration> {
    public static final FacetTypeId<FunctionsFacet> FACET_TYPE_ID = new FacetTypeId<>("azurefunctions");

    protected FunctionsFacet(FacetType facetType, Module module, String name, FunctionsFacetConfiguration configuration, Facet underlyingFacet) {
        super(facetType, module, name, configuration, underlyingFacet);
    }
}
