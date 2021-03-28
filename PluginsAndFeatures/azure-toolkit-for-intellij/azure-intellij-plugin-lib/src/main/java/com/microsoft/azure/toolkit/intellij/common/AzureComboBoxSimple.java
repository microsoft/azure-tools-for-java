/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import lombok.RequiredArgsConstructor;
import lombok.Setter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@RequiredArgsConstructor
@Setter
public class AzureComboBoxSimple<T> extends AzureComboBox<T> {

    private final Supplier<? extends List<? extends T>> supplier;
    private Validator validator;

    @Nonnull
    protected List<? extends T> loadItems() throws Exception {
        if (Objects.nonNull(this.supplier)) {
            return this.supplier.get();
        }
        return Collections.emptyList();
    }
}
