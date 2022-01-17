/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.ide.common.icon;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AzureIconProvider<T> {

    private final Function<T, String> iconPathFunction;
    private final List<Function<T, AzureIcon.Modifier>> modifierFunctionList;

    public AzureIconProvider(@Nonnull Function<T, String> iconPathFunction, Function<T, AzureIcon.Modifier>... modifierFunction) {
        this.iconPathFunction = iconPathFunction;
        this.modifierFunctionList = modifierFunction == null ? Collections.emptyList() : Arrays.asList(modifierFunction);
    }

    public AzureIcon getIcon(T resource) {
        final String iconPath = iconPathFunction.apply(resource);
        final List<AzureIcon.Modifier> modifiers = modifierFunctionList == null ? Collections.emptyList() :
                modifierFunctionList.stream().filter(Objects::nonNull).map(function -> function.apply(resource)).collect(Collectors.toList());
        return AzureIcon.builder().iconPath(iconPath).modifierList(modifiers).build();
    }

    public AzureIcon getIconWithoutModifier(T resource) {
        return AzureIcon.builder().iconPath(iconPathFunction.apply(resource)).modifierList(Collections.emptyList()).build();
    }
}
