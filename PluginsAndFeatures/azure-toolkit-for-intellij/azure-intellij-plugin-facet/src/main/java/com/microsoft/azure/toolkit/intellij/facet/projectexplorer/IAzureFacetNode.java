/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.facet.projectexplorer;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.project.Project;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.util.ui.tree.TreeUtil;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;

import javax.annotation.Nullable;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Objects;

public interface IAzureFacetNode extends DataProvider, Disposable {
    @Nullable
    default IActionGroup getActionGroup() {
        return null;
    }

    default void onClicked(Object event) {

    }

    default void onDoubleClicked(Object event) {

    }

    default void rerender(boolean updateStructure) {
        final Project project = getProject();
        if (project.isDisposed()) {
            return;
        }
        final AbstractProjectViewPane pane = ProjectView.getInstance(project).getCurrentProjectViewPane();
        final AsyncTreeModel model = (AsyncTreeModel) pane.getTree().getModel();
        final DefaultMutableTreeNode node = TreeUtil.findNodeWithObject((DefaultMutableTreeNode) model.getRoot(), this);
        if (Objects.nonNull(node)) {
            final TreePath path = TreeUtil.getPath((TreeNode) model.getRoot(), node);
            pane.updateFrom(path, false, updateStructure);
        }
    }

    Project getProject();

    default void dispose() {
        setDisposed(true);
        rerender(true);
    }

    boolean isDisposed();

    void setDisposed(boolean isDisposed);
}
