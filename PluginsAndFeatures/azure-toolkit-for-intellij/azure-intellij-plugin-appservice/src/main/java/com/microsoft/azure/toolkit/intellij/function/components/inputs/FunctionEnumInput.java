/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.components.inputs;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.legacy.function.template.FunctionSettingTemplate;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FunctionEnumInput extends AzureComboBox<String> {
    private final FunctionSettingTemplate template;

    public FunctionEnumInput(@Nonnull final FunctionSettingTemplate template) {
        super(false);
        this.template = template;
        this.setRequired(template.isRequired());
        Optional.ofNullable(template.getDefaultValue()).ifPresent(this::setValue);
        refreshItems();
    }

    @Nonnull
    @Override
    protected List<? extends String> loadItems() throws Exception {
        return Arrays.stream(template.getSettingEnum()).map(en -> en.getValue()).collect(Collectors.toList());
    }
}
