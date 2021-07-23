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
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkCategoryEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkFeatureEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkServiceEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.service.AzureSdkCategoryService;
import com.microsoft.azure.toolkit.intellij.azuresdk.service.AzureSdkLibraryService;
import com.microsoft.azure.toolkit.intellij.common.TextDocumentListenerAdapter;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
    private Map<String, List<AzureSdkCategoryEntity>> categories;
    private TreePath lastNodePath;

    public AzureSdkTreePanel() {
        this.initEventListeners();
        this.filter = new TailingDebouncer(() -> this.filter(this.searchBox.getText()), 300);
    }

    private void initEventListeners() {
        this.searchBox.addDocumentListener(this);
        this.tree.addTreeSelectionListener(e -> {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) this.tree.getLastSelectedPathComponent();
            if (Objects.nonNull(node) && node.isLeaf() && node.getUserObject() instanceof AzureSdkFeatureEntity) {
                this.lastNodePath = TreeUtil.getPathFromRoot(node);
                selectFeature((AzureSdkFeatureEntity) node.getUserObject());
            }
        });
    }

    @AzureOperation(name = "sdk|reference_book.show_details", params = "feature.getName()", type = AzureOperation.Type.ACTION)
    private void selectFeature(final AzureSdkFeatureEntity feature) {
        this.onSdkFeatureNodeSelected.accept(feature);
    }

    @Override
    public void onDocumentChanged() {
        this.filter.debounce();
    }

    private void filter(final String text) {
        final String[] filters = Arrays.stream(text.split("\\s+")).filter(StringUtils::isNoneBlank).map(String::toLowerCase).toArray(String[]::new);
        this.loadData(this.categories, this.services, filters);
    }

    public void refresh(boolean... force) {
        try {
            if(ArrayUtils.isNotEmpty(force) && force[0]){
                AzureSdkLibraryService.refresh();
            }
            this.services = AzureSdkLibraryService.loadAzureSdkServices();
            this.categories = AzureSdkCategoryService.loadAzureSDKCategories();
            this.fillDescriptionFromCategoryIfMissing(this.categories, this.services);
            this.filter.debounce();
            Optional.ofNullable(this.lastNodePath).ifPresent(p -> TreeUtil.selectPath(this.tree, p));
        } catch (final IOException e) {
            //TODO: messager.warning(...)
            e.printStackTrace();
        }
    }

    private void fillDescriptionFromCategoryIfMissing(final Map<String, List<AzureSdkCategoryEntity>> categoryToServiceMap, final List<? extends AzureSdkServiceEntity> services) {
        final Map<String, AzureSdkServiceEntity> serviceMap = services.stream().collect(Collectors.toMap(e -> getServiceKeyByName(e.getName()), e -> e));
        categoryToServiceMap.forEach((category, categoryServices) -> categoryServices.forEach(categoryService ->
            Optional.ofNullable(serviceMap.get(getServiceKeyByName(categoryService.getServiceName()))).ifPresent(service -> {
                for (final AzureSdkFeatureEntity feature : service.getContent()) {
                    if (StringUtils.isBlank(feature.getDescription()) && StringUtils.isNotBlank(categoryService.getDescription())) {
                        feature.setDescription(categoryService.getDescription());
                    }
                }
            })));
    }

    private void loadData(final Map<String, List<AzureSdkCategoryEntity>> categoryToServiceMap, final List<? extends AzureSdkServiceEntity> services, String... filters) {
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) this.model.getRoot();
        root.removeAllChildren();
        final Map<String, AzureSdkServiceEntity> serviceMap = services.stream().collect(Collectors.toMap(e -> getServiceKeyByName(e.getName()), e -> e));
        final List<String> categories = categoryToServiceMap.keySet().stream().filter(StringUtils::isNotBlank).sorted(
                (s1, s2) -> StringUtils.contains(s1, "Others") ? 1 : StringUtils.contains(s2, "Others") ? -1 : s1.compareTo(s2)).collect(Collectors.toList());
        for (final String category : categories) {
            // no feature found for current category
            if (CollectionUtils.isEmpty(categoryToServiceMap.get(category)) ||
                categoryToServiceMap.get(category).stream().allMatch(e -> Objects.isNull(serviceMap.get(getServiceKeyByName(e.getServiceName()))) ||
                    CollectionUtils.isEmpty(serviceMap.get(getServiceKeyByName(e.getServiceName())).getContent()))) {
                continue;
            }
            // add features for current category
            final MutableTreeNode categoryNode = new DefaultMutableTreeNode(category);
            final boolean categoryMatched = this.isMatchedFilters(category, filters);
            categoryToServiceMap.get(category)
                .stream().sorted(Comparator.comparing(AzureSdkCategoryEntity::getServiceName))
                .forEach(categoryService -> {
                    final AzureSdkServiceEntity service = serviceMap.get(getServiceKeyByName(categoryService.getServiceName()));
                    this.loadServiceData(service, categoryService, categoryNode, filters);
                });
            if (ArrayUtils.isEmpty(filters) || categoryMatched || categoryNode.getChildCount() > 0) {
                this.model.insertNodeInto(categoryNode, root, root.getChildCount());
            }
        }
        this.model.reload();
        if (ArrayUtils.isNotEmpty(filters)) {
            TreeUtil.expandAll(this.tree);
        }
        TreeUtil.promiseSelectFirstLeaf(this.tree);
    }

    private String getServiceKeyByName(final String name) {
        return StringUtils.lowerCase(StringUtils.trim(name));
    }

    private void loadServiceData(AzureSdkServiceEntity service, AzureSdkCategoryEntity categoryService, MutableTreeNode categoryNode, String... filters) {
        if (Objects.isNull(service) || CollectionUtils.isEmpty(service.getContent())) {
            return;
        }
        final boolean categoryMatched = this.isMatchedFilters(categoryService.getCategory(), filters);
        if (CollectionUtils.size(service.getContent()) == 1 && StringUtils.equals(service.getName(), service.getContent().get(0).getName())) {
            final AzureSdkFeatureEntity feature = service.getContent().get(0);
            final boolean featureMatched = this.isMatchedFilters(feature.getName(), filters);
            if (ArrayUtils.isEmpty(filters) || categoryMatched || featureMatched) {
                final DefaultMutableTreeNode featureNode = new DefaultMutableTreeNode(feature);
                this.model.insertNodeInto(featureNode, categoryNode, categoryNode.getChildCount());
            }
            return;
        }
        final MutableTreeNode serviceNode = new DefaultMutableTreeNode(service);
        final boolean serviceMatched = this.isMatchedFilters(service.getName(), filters);
        for (final AzureSdkFeatureEntity feature : service.getContent()) {
            final boolean featureMatched = this.isMatchedFilters(feature.getName(), filters);
            if (ArrayUtils.isEmpty(filters) || categoryMatched || serviceMatched || featureMatched) {
                final DefaultMutableTreeNode featureNode = new DefaultMutableTreeNode(feature);
                this.model.insertNodeInto(featureNode, serviceNode, serviceNode.getChildCount());
            }
        }
        if (ArrayUtils.isEmpty(filters) || categoryMatched || serviceMatched || serviceNode.getChildCount() > 0) {
            this.model.insertNodeInto(serviceNode, categoryNode, categoryNode.getChildCount());
        }
    }

    private boolean isMatchedFilters(String content, String... filters) {
        return Arrays.stream(filters).allMatch(f -> StringUtils.containsIgnoreCase(content, f));
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
                AzureSdkTreePanel.this.refresh(true);
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
