/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.tree;

import com.intellij.openapi.actionSystem.DefaultActionGroup;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Objects;
import java.util.function.Function;

public class AzureNode<T> extends DefaultMutableTreeNode {
    private final DefaultActionGroup actionGroup = new DefaultActionGroup();

    private final T data;

    public AzureNode(T data) {
        this(data, Objects::toString, null);
    }

    public AzureNode(T data, Function<T, String> textGetter) {
        this(data, textGetter, null);
    }

    public AzureNode(T data, Icon icon) {
        this(data, Objects::toString, icon);
    }

    public AzureNode(T data, Function<T, String> textGetter, Icon icon) {
        this.data = data;
        AzureNodeDescriptor<T> nodeDescriptor = new AzureNodeDescriptor<>(data, textGetter, icon);
        setUserObject(nodeDescriptor);
    }

    public T getData() {
        return this.data;
    }

    public AzureNodeDescriptor<?> getNodeDescriptor() {
        return (AzureNodeDescriptor<?>) getUserObject();
    }

    public DefaultActionGroup getActionGroup() {
        return this.actionGroup;
    }

}
