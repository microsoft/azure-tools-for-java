/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AzureComboBoxSimple<T> extends AzureComboBox<T> {

    private DataProvider<? extends List<? extends T>> provider;

    public AzureComboBoxSimple() {
        super();
    }

    public AzureComboBoxSimple(final DataProvider<? extends List<? extends T>> provider) {
        this();
        this.provider = provider;
    }

    @Nonnull
    protected List<? extends T> loadItems() throws Exception {
        if (Objects.nonNull(this.provider)) {
            return this.provider.loadData();
        }
        return Collections.emptyList();
    }

    @FunctionalInterface
    public interface DataProvider<T> {
        T loadData() throws Exception;
    }
}
