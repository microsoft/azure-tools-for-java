/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet.projectexplorer;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class ResourceNode extends AbstractAzureFacetNode<Node<?>> implements Node.ChildrenRenderer, Node.ViewRenderer {

    public ResourceNode(@Nonnull Project project, final Node<?> node) {
        super(project, node);
        node.setViewRenderer(this);
        node.setChildrenRenderer(this);
    }

    @Override
    @Nonnull
    public Collection<? extends AbstractTreeNode<?>> getChildren() {
        final ArrayList<AbstractTreeNode<?>> children = new ArrayList<>();
        if (this.isDisposed()) {
            return Collections.emptyList();
        }
        try {
            final Node<?> node = this.getValue();
            children.addAll(node.getChildren().stream().map(n -> new ResourceNode(this.getProject(), n)).toList());
            if (node.hasMoreChildren()) {
                final Action<Object> loadMoreAction = new Action<>(Action.Id.of("user/common.load_more"))
                    .withHandler(i -> node.loadMoreChildren())
                    .withLabel("load more")
                    .withAuthRequired(true);
                children.add(new ActionNode<>(this.getProject(), loadMoreAction));
            }
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        }
        return children;
    }

    @Override
    protected void update(@Nonnull final PresentationData presentation) {
        try {
            final Node<?> node = this.getValue();
            final Node.View view = node.getView();
            presentation.setIcon(IntelliJAzureIcons.getIcon(view.getIcon()));
            presentation.addText(view.getLabel(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            presentation.setTooltip(view.getTips());
            Optional.ofNullable(view.getDescription()).ifPresent(d -> presentation.addText(" " + d, SimpleTextAttributes.GRAYED_ATTRIBUTES));
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    @Override
    public void updateView() {
        rerender(false);
    }

    @Override
    public void updateChildren(boolean... incremental) {
        rerender(true);
    }

    @Override
    @Nullable
    public Object getData(@Nonnull String dataId) {
        if (StringUtils.equalsIgnoreCase(dataId, Action.SOURCE)) {
            return Optional.ofNullable(getValue()).map(Node::getValue).orElse(null);
        }
        return null;
    }

    @Override
    public void onDoubleClicked(Object event) {
        Optional.ofNullable(this.getValue()).ifPresent(n -> n.doubleClick(event));
    }

    @Override
    public void onClicked(Object event) {
        Optional.ofNullable(this.getValue()).ifPresent(n -> n.click(event));
    }

    @Override
    @Nullable
    public IActionGroup getActionGroup() {
        return Optional.ofNullable(getValue()).map(Node::getActions).orElse(null);
    }

    @Override
    public void dispose() {
        super.dispose();
        Optional.ofNullable(getValue()).ifPresent(Node::dispose);
    }

    @EqualsAndHashCode.Include
    public AbstractTreeNode<?> getMyParent() {
        return this.getParent();
    }
}