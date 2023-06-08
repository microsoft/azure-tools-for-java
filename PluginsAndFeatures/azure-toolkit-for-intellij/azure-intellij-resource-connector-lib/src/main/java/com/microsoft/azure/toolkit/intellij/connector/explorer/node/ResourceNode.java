/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.explorer.node;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.component.NodeView;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;

public class ResourceNode extends AbstractTreeNode<Node<?>> implements IAzureProjectExplorerNode {
    public ResourceNode(@Nonnull Project project, final Node<?> node) {
        super(project, node);
    }

    @Override
    @Nonnull
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
        final Node<?> node = this.getValue();
        return node.getChildren().stream().map(n -> new ResourceNode(this.getProject(), n)).toList();
    }

    @Override
    protected void update(@Nonnull final PresentationData presentation) {
        final Node<?> node = this.getValue();
        final NodeView view = node.view();
        presentation.addText(view.getLabel() + StringUtils.SPACE, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        presentation.setIcon(IntelliJAzureIcons.getIcon(view.getIcon()));
        presentation.addText(view.getDescription(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
    }

    @Override
    public IActionGroup getActionGroup() {
        return Optional.ofNullable(getValue()).map(Node::actions).orElse(null);
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        if (StringUtils.equalsIgnoreCase(dataId, Action.SOURCE)) {
            return Optional.ofNullable(getValue()).map(Node::data).orElse(null);
        }
        return null;
    }
}

