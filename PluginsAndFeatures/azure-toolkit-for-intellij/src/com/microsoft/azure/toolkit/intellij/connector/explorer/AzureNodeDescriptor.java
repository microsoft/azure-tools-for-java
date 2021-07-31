/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.explorer;

import com.intellij.ide.util.treeView.NodeDescriptor;

import javax.swing.*;
import java.util.Objects;
import java.util.function.Function;

public class AzureNodeDescriptor<T> extends NodeDescriptor<T> {
    private final T entity;

    private final Function<T, String> textGetter;

    public AzureNodeDescriptor(T entity) {
        this(entity, Objects::toString, null);
    }

    public AzureNodeDescriptor(T entity, Icon icon) {
        this(entity, Objects::toString, icon);
    }

    public AzureNodeDescriptor(T entity, Function<T, String> textGetter) {
        this(entity, textGetter, null);
    }

    public AzureNodeDescriptor(T entity, Function<T, String> textGetter, Icon icon) {
        super(null, null);
        this.entity = entity;
        this.textGetter = textGetter;
        if (Objects.nonNull(icon)) {
            setIcon(icon);
        }
    }

    @Override
    public String toString() {
        return this.textGetter.apply(this.entity);
    }

    @Override
    public boolean update() {
        return false;
    }

    @Override
    public T getElement() {
        return this.entity;
    }
}
