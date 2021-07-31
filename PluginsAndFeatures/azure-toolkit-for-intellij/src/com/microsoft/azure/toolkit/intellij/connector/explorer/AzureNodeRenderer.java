/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.explorer;

import com.intellij.ide.util.treeView.NodeRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Objects;

public class AzureNodeRenderer extends NodeRenderer {

    @Override
    public void customizeCellRenderer(@NotNull JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
        if (Objects.isNull(value)) {
            return;
        }
        if (value instanceof AzureNode) {
            AzureNodeDescriptor<?> descriptor = ((AzureNode<?>) value).getNodeDescriptor();
            if (Objects.nonNull(descriptor) && Objects.nonNull(descriptor.getIcon())) {
                setIcon(descriptor.getIcon());
            }
        }
    }
}
