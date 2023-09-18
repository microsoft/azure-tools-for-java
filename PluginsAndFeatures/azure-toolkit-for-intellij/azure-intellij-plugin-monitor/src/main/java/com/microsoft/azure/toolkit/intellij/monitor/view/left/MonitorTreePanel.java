/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.monitor.view.left;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.treeView.NodeRenderer;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.RelativeFont;
import com.intellij.ui.render.RenderingUtil;
import com.intellij.ui.treeStructure.SimpleTree;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.tree.TreeUtil;
import com.microsoft.azure.toolkit.intellij.monitor.AzureMonitorManager;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class MonitorTreePanel extends JPanel implements Disposable {
    private JPanel contentPanel;
    private Tree tree;
    private DefaultTreeModel treeModel;
    private TreePath currentTreeNodePath;
    private String currentNodeText;
    @Setter
    private boolean isTableTab;
    @Getter
    private final List<QueryData> customQueries = new ArrayList<>();
    private final String CUSTOM_QUERIES_TAB = "Custom Queries";
    private final AzureEventBus.EventListener eventListener;
    private final Project project;

    public MonitorTreePanel(Project project, Disposable parentDisposable) {
        super();
        this.project = project;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.eventListener = new AzureEventBus.EventListener(e -> this.addQueryNode((QueryData) e.getSource()));
        Disposer.register(parentDisposable, this);
        AzureEventBus.on("azure.monitor.add_query_node", this.eventListener);
    }

    public synchronized void refresh() {
        if (this.isTableTab) {
            loadTableTreeData();
        } else {
            loadQueryTreeData();
        }
        selectNode(this.tree, this.currentTreeNodePath, getDefaultNodeName());
    }

    public void addTreeSelectionListener(TreeSelectionListener listener) {
        this.tree.addTreeSelectionListener(listener);
    }

    public String getQueryString(String nodeDisplayName) {
        final DefaultMutableTreeNode node = TreeUtil.findNode((DefaultMutableTreeNode) tree.getModel().getRoot(), n -> Objects.equals(n.toString(), nodeDisplayName));
        if (Objects.isNull(node)) {
            return StringUtils.EMPTY;
        }
        if (node.getUserObject() instanceof QueryData) {
            return ((QueryData) node.getUserObject()).getQueryString();
        } else {
            return node.toString();
        }
    }

    public void dispose() {
        AzureEventBus.off("azure.monitor.add_query_node", this.eventListener);
    }

    private void addQueryNode(QueryData data) {
        if (isTableTab) {
            return;
        }
        customQueries.stream().filter(q -> Objects.equals(q.displayName, data.displayName)).findAny()
                .ifPresentOrElse(q -> q.setQueryString(data.queryString), () -> customQueries.add(data));
        AzureTaskManager.getInstance().runLater(() -> {
            final DefaultMutableTreeNode customQueryRootNode = getOrCreateCustomQueriesTabNode();
            final DefaultMutableTreeNode overrideNode = TreeUtil.findNode(customQueryRootNode, n ->
                    n.getUserObject() instanceof QueryData && Objects.equals(((QueryData) n.getUserObject()).displayName, data.displayName));
            AzureMonitorManager.getInstance().changeContentView(project, AzureMonitorManager.QUERIES_TAB_NAME);
            final DefaultMutableTreeNode selectedNode;
            if (Objects.nonNull(overrideNode)) {
                this.treeModel.valueForPathChanged(TreeUtil.getPathFromRoot(overrideNode), data);
                selectedNode = overrideNode;
            } else {
                final DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(data);
                this.treeModel.insertNodeInto(newNode, customQueryRootNode, 0);
                selectedNode = newNode;
            }
            TreeUtil.selectNode(this.tree, selectedNode);
        });
        AzureTaskManager.getInstance().runInBackground("save query", () -> {
            final ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            final List<String> customQueryList = new ArrayList<>();
            this.customQueries.forEach(q -> {
                try {
                    customQueryList.add(ow.writeValueAsString(q));
                } catch (final JsonProcessingException ignored) {
                }
            });
            PropertiesComponent.getInstance().setValues(AzureMonitorManager.AZURE_MONITOR_CUSTOM_QUERY_LIST, customQueryList.toArray(new String[0]));
        });
    }

    private void initListener() {
        this.tree.addTreeSelectionListener(e -> {
            final DefaultMutableTreeNode node = (DefaultMutableTreeNode) this.tree.getLastSelectedPathComponent();
            if (Objects.nonNull(node) && node.isLeaf()) {
                this.currentTreeNodePath = TreeUtil.getPathFromRoot(node);
                if (node.getUserObject() instanceof QueryData) {
                    this.currentNodeText = ((QueryData) node.getUserObject()).getQueryString();
                } else {
                    this.currentNodeText = node.toString();
                }
            }
        });
    }

    private void selectNode(Tree tree, TreePath path, String defaultNodeName) {
        if (Objects.nonNull(path)) {
            AzureTaskManager.getInstance().runAndWait(() -> TreeUtil.selectPath(tree, path));
            return;
        }
        final DefaultMutableTreeNode defaultNode = TreeUtil.findNode((DefaultMutableTreeNode) tree.getModel().getRoot(), n -> {
            final String nodeName;
            if (n.getUserObject() instanceof QueryData) {
                nodeName = ((QueryData) n.getUserObject()).getDisplayName();
            } else {
                nodeName = (String) n.getUserObject();
            }
            return Objects.equals(nodeName, defaultNodeName);
        });
        AzureTaskManager.getInstance().runAndWait(() -> TreeUtil.selectNode(tree, defaultNode));
    }

    private void loadTableTreeData() {
        final String dataPath = "table-query-tree/TableTree.json";
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) this.treeModel.getRoot();
        root.removeAllChildren();
        try (final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(dataPath)) {
            final Map<String, List<String>> treeData = new JsonMapper()
                    .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(inputStream, new TypeReference<>() {
                    });
            treeData.forEach((key, value) -> {
                final DefaultMutableTreeNode resourceNode = new DefaultMutableTreeNode(key);
                value.forEach(treeName -> {
                    final DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(treeName);
                    resourceNode.add(treeNode);
                });
                root.add(resourceNode);
            });
        } catch (final Exception ignored) {
        }
        AzureTaskManager.getInstance().runLater(() -> {
            this.treeModel.reload();
            TreeUtil.expandAll(this.tree);
        }, AzureTask.Modality.ANY);
    }

    private void loadQueryTreeData() {
        final String dataPath = "table-query-tree/QueryTree.json";
        final DefaultMutableTreeNode root = (DefaultMutableTreeNode) this.treeModel.getRoot();
        root.removeAllChildren();
        try (final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(dataPath)) {
            final Map<String, List<QueryData>> treeData = new JsonMapper()
                    .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .readValue(inputStream, new TypeReference<>() {
                    });
            treeData.forEach((key, value) -> {
                final DefaultMutableTreeNode resourceNode = new DefaultMutableTreeNode(key);
                value.forEach(treeName -> {
                    final DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(treeName);
                    resourceNode.add(treeNode);
                });
                root.add(resourceNode);
            });
        } catch (final Exception ignored) {
        }
        final String[] queries = PropertiesComponent.getInstance().getValues(AzureMonitorManager.AZURE_MONITOR_CUSTOM_QUERY_LIST);
        Optional.ofNullable(queries).map(array -> Arrays.stream(array).collect(Collectors.toList()))
                .ifPresent(l -> l.forEach(s -> {
                    final ObjectMapper mapper = new ObjectMapper();
                    try {
                        customQueries.add(mapper.readValue(s, QueryData.class));
                    } catch (final JsonProcessingException ignored) {
                    }
                }));
        if (this.customQueries.size() > 0) {
            final DefaultMutableTreeNode tabNode = new DefaultMutableTreeNode(CUSTOM_QUERIES_TAB);
            this.customQueries.forEach(queryData -> {
                final DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(queryData);
                tabNode.add(treeNode);
            });
            root.add(tabNode);
        }
        AzureTaskManager.getInstance().runLater(() -> {
            this.treeModel.reload();
            TreeUtil.expandAll(this.tree);
        }, AzureTask.Modality.ANY);
    }

    private DefaultMutableTreeNode getOrCreateCustomQueriesTabNode() {
        final DefaultMutableTreeNode node = TreeUtil.findNode((DefaultMutableTreeNode) tree.getModel().getRoot(), n -> Objects.equals(n.toString(), CUSTOM_QUERIES_TAB));
        if (Objects.nonNull(node)) {
            return node;
        }
        final DefaultMutableTreeNode tabNode = new DefaultMutableTreeNode(CUSTOM_QUERIES_TAB);
        this.treeModel.insertNodeInto(tabNode, (DefaultMutableTreeNode) this.treeModel.getRoot(), 0);
        return tabNode;
    }

    private Tree initTree(DefaultTreeModel treeModel) {
        final SimpleTree tree = new SimpleTree(treeModel);
        tree.putClientProperty(RenderingUtil.ALWAYS_PAINT_SELECTION_AS_FOCUSED, true);
        tree.setCellRenderer(new NodeRenderer());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        TreeUtil.installActions(tree);
        RelativeFont.BOLD.install(tree);
        return tree;
    }

    private String getDefaultNodeName() {
        return this.isTableTab ? "AppTraces" : "Exceptions causing request failures";
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    private void createUIComponents() {
        this.treeModel = new DefaultTreeModel(new DefaultMutableTreeNode("Azure Monitor"));
        this.tree = this.initTree(this.treeModel);
        this.initListener();
    }

    @Getter
    @Setter
    public static class QueryData {
        private String displayName;
        private String queryString;

        @Override
        public String toString() {
            return this.displayName;
        }
    }
}
