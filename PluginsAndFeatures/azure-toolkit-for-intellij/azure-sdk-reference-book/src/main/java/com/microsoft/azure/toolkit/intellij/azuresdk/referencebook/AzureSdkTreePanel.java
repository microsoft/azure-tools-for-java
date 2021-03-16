/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook;

import com.intellij.icons.AllIcons;
import com.intellij.ide.ActivityTracker;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.DefaultTreeExpander;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.RelativeFont;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.render.RenderingUtil;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.tree.TreeUtil;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkFeatureEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkServiceEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.service.AzureSdkLibraryService;
import com.microsoft.azure.toolkit.intellij.common.TextDocumentListenerAdapter;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class AzureSdkTreePanel implements TextDocumentListenerAdapter {
    private final TailingDebouncer filter;
    @Setter
    private Consumer<AzureSdkFeatureEntity> onSdkFeatureNodeSelected;
    @Getter
    private JPanel contentPanel;
    private Tree tree;
    private ActionToolbarImpl toolbar;
    private JBScrollPane scroller;
    private SearchTextField searchBox;
    private DefaultTreeModel model;
    private List<? extends AzureSdkServiceEntity> services;

    public AzureSdkTreePanel() {
        this.initEventListeners();
        this.filter = new TailingDebouncer(() -> this.filter(this.searchBox.getText()), 300);
    }

    private void initEventListeners() {
        this.searchBox.addDocumentListener(this);
        this.tree.addTreeSelectionListener(e -> {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) this.tree.getLastSelectedPathComponent();
            if (Objects.nonNull(node) && node.isLeaf() && node.getUserObject() instanceof AzureSdkFeatureEntity) {
                this.onSdkFeatureNodeSelected.accept((AzureSdkFeatureEntity) node.getUserObject());
            }
        });
    }

    @Override
    public void onDocumentChanged() {
        this.filter.debounce();
    }

    private void filter(final String text) {
        final String[] filters = Arrays.stream(text.split("\\s+")).filter(StringUtils::isNoneBlank).map(String::toLowerCase).toArray(String[]::new);
        this.loadData(this.services, filters);
    }

    public void reload(Boolean... force) {
        try {
            this.setData(AzureSdkLibraryService.loadAzureSdkServices(force));
        } catch (final IOException e) {
            //TODO: messager.warning(...)
            e.printStackTrace();
        }
    }

    public void setData(@Nonnull final List<? extends AzureSdkServiceEntity> services) {
        this.services = services;
        this.loadData(this.services);
    }

    private void loadData(final List<? extends AzureSdkServiceEntity> services, String... filters) {
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) this.model.getRoot();
        root.removeAllChildren();
        for (final AzureSdkServiceEntity service : services) {
            final DefaultMutableTreeNode serviceNode = new DefaultMutableTreeNode(service);
            final boolean serviceMatched = Arrays.stream(filters).allMatch(f -> StringUtils.containsIgnoreCase(service.getName(), f));
            for (final AzureSdkFeatureEntity feature : service.getContent()) {
                final boolean featureMatched = Arrays.stream(filters).allMatch(f -> StringUtils.containsIgnoreCase(feature.getName(), f));
                if (ArrayUtils.isEmpty(filters) || serviceMatched || featureMatched) {
                    final DefaultMutableTreeNode featureNode = new DefaultMutableTreeNode(feature);
                    this.model.insertNodeInto(featureNode, serviceNode, serviceNode.getChildCount());
                }
            }
            if (ArrayUtils.isEmpty(filters) || serviceMatched || serviceNode.getChildCount() > 0) {
                this.model.insertNodeInto(serviceNode, root, root.getChildCount());
            }
        }
        this.model.reload();
        if (ArrayUtils.isNotEmpty(filters)) {
            TreeUtil.expandAll(this.tree);
        }
        TreeUtil.promiseSelectFirstLeaf(this.tree);
    }

    private ActionToolbarImpl initToolbar() {
        final DefaultTreeExpander expander = new DefaultTreeExpander(this.tree);
        final DefaultActionGroup group = new DefaultActionGroup();
        final CommonActionsManager manager = CommonActionsManager.getInstance();
        group.add(new RefreshAction());
        group.addSeparator();
        group.add(manager.createExpandAllAction(expander, this.tree));
        group.add(manager.createCollapseAllAction(expander, this.tree));
        return new ActionToolbarImpl(ActionPlaces.TOOLBAR, group, true);
    }

    private Tree initTree() {
        final DefaultMutableTreeNode root = new DefaultMutableTreeNode("Azure SDK Libraries");
        this.model = new DefaultTreeModel(root);
        final SimpleTree tree = new SimpleTree(model);
        tree.putClientProperty(RenderingUtil.ALWAYS_PAINT_SELECTION_AS_FOCUSED, true);
        tree.setCellRenderer(new NodeRenderer());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        TreeUtil.installActions(tree);
        RelativeFont.BOLD.install(tree);
        return tree;
    }

    private void createUIComponents() {
        this.tree = this.initTree();
        this.toolbar = this.initToolbar();
        this.toolbar.setTargetComponent(this.tree);
        this.toolbar.setForceMinimumSize(true);
    }

    private class RefreshAction extends com.intellij.ide.actions.RefreshAction {
        private boolean loading = false;

        RefreshAction() {
            super(IdeBundle.messagePointer("action.refresh"), IdeBundle.messagePointer("action.refresh"), AllIcons.Actions.Refresh);
        }

        @Override
        public final void actionPerformed(@NotNull final AnActionEvent e) {
            this.loading = true;
            ActivityTracker.getInstance().inc();
            AzureTaskManager.getInstance().runLater(() -> {
                AzureSdkTreePanel.this.reload(true);
                this.loading = false;
            });
        }

        @Override
        public final void update(@NotNull final AnActionEvent event) {
            final Presentation presentation = event.getPresentation();
            final Icon icon = loading ? new AnimatedIcon.Default() : this.getTemplatePresentation().getIcon();
            presentation.setIcon(icon);
            presentation.setEnabled(!loading);
        }
    }
}
