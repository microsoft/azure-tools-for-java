/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.ui.base.MvpView;
import com.microsoft.azuretools.core.mvp.ui.base.NodeContent;
import com.microsoft.azuretools.telemetry.BasicTelemetryProperty;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.helpers.collections.ObservableList;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Node implements MvpView, BasicTelemetryProperty, Sortable {
    private static final String CLICK_ACTION = "click";
    public static final String REST_SEGMENT_JOB_MANAGEMENT_TENANTID = "/#@";
    public static final String REST_SEGMENT_JOB_MANAGEMENT_RESOURCE = "/resource";
    public static final String OPEN_RESOURCES_IN_PORTAL_FAILED = "Fail to open resources in portal.";
    public static final int DEFAULT_SORT_PRIORITY = 100;
    private static final String PROGRESS_MESSAGE_PATTERN = "%s %s (%s)...";
    private static final String PROMPT_MESSAGE_PATTERN = "This operation will %s your %s: %s. Are you sure you want to continue?";

    protected static Map<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>> node2Actions;

    protected String id;
    protected String name;
    protected Node parent;
    protected ObservableList<Node> childNodes = new ObservableList<Node>();
    protected String iconPath;
    protected Object viewData;
    protected NodeAction clickAction = new NodeAction(this, CLICK_ACTION);
    protected List<NodeAction> nodeActions = new ArrayList<NodeAction>();
    protected JTree tree;
    protected TreePath treePath;

    // marks this node as being in a "loading" state; when this field is true
    // the following consequences apply:
    //  [1] all actions associated with this node get disabled
    //  [2] click action gets disabled automatically
    protected boolean loading = false;

    protected PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public Node(String id, String name) {
        this(id, name, null, null, false);
    }

    public Node(String id, String name, Node parent) {
        this(id, name, parent, false);
    }

    public Node(String id, String name, Node parent, String iconPath) {
        this(id, name, parent, iconPath, false);
    }

    public Node(String id, String name, Node parent, boolean delayActionLoading) {
        this.id = id;
        this.name = name;
        this.parent = parent;

        if (!delayActionLoading) {
            loadActions();
        }
    }

    public Node(String id, String name, Node parent, String iconPath, boolean delayActionLoading) {
        this.id = id;
        this.name = name;
        this.parent = parent;
        this.iconPath = iconPath;

        if (!delayActionLoading) {
            loadActions();
        }
    }

    public String getId() {
        return id;
    }

    public JTree getTree() {
        return tree;
    }

    public void setTree(JTree tree) {
        this.tree = tree;
    }

    public TreePath getTreePath() {
        return treePath;
    }

    public void setTreePath(TreePath treePath) {
        this.treePath = treePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        final String oldValue = this.name;
        this.name = name;
        propertyChangeSupport.firePropertyChange("name", oldValue, name);
    }

    public Node getParent() {
        return parent;
    }

    public ObservableList<Node> getChildNodes() {
        return childNodes;
    }

    public boolean isDirectChild(Node node) {
        return childNodes.contains(node);
    }

    public boolean isDescendant(Node node) {
        if (isDirectChild(node)) {
            return true;
        }
        for (final Node child : childNodes) {
            if (child.isDescendant(node)) {
                return true;
            }
        }

        return false;
    }

    // Walk up the tree till we find a parent node who's type
    // is equal to "clazz".
    public <T extends Node> @Nullable T findParentByType(Class<T> clazz) {
        if (parent == null) {
            return null;
        }
        if (parent.getClass().equals(clazz)) {
            return (T) parent;
        }
        return parent.findParentByType(clazz);
    }

    public boolean hasChildNodes() {
        return !childNodes.isEmpty();
    }

    public void removeDirectChildNode(Node childNode) {
        if (isDirectChild(childNode)) {
            // remove this node's child nodes (so they get an
            // opportunity to clean up after them)
            childNode.removeAllChildNodes();

            // this remove call should cause the NodeListChangeListener object
            // registered on it's child nodes to fire
            childNodes.remove(childNode);
        }
    }

    public void removeAllChildNodes() {
        while (!childNodes.isEmpty()) {
            final Node node = childNodes.get(0);

            // sometimes node can be null if multiple threads access this method; safer to check than make it synchronized
            if (node != null) {
                // remove this node's child nodes (so they get an
                // opportunity to clean up after them)
                node.removeAllChildNodes();
            }

            // this remove call should cause the NodeListChangeListener object
            // registered on it's child nodes to fire
            childNodes.remove(0);
        }
    }

    /**
     * higher priority than iconPath and icon
     */
    @Nullable
    public AzureIcon getIconSymbol() {
        return null;
    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        final String oldValue = this.iconPath;
        this.iconPath = iconPath;
        propertyChangeSupport.firePropertyChange("iconPath", oldValue, iconPath);
    }

    /**
     * higher priority than iconPath
     */
    @Nullable
    public Icon getIcon() {
        return null;
    }

    public void addChildNode(Node child) {
        childNodes.add(child);
    }

    public void addAction(NodeAction action) {
        nodeActions.add(action);
    }

    // Convenience method to add a new action with a pre-configured listener. If
    // an action with the same name already exists then the listener is added
    // to that action.
    public NodeAction addAction(String name, NodeActionListener actionListener) {
        NodeAction nodeAction = getNodeActionByName(name);
        if (nodeAction == null) {
            nodeAction = new NodeAction(this, name);
            addAction(nodeAction);
        }
        nodeAction.addListener(actionListener);
        nodeAction.setPriority(actionListener.getPriority());
        nodeAction.setGroup(actionListener.getGroup());
        nodeAction.setIconSymbol(actionListener.getIconSymbol());
        return nodeAction;
    }

    public NodeAction addAction(DelegateActionListener.BasicActionListener actionListener) {
        return addAction(actionListener.getActionEnum().getName(), actionListener);
    }

    public NodeAction addAction(String name, String iconPath, NodeActionListener actionListener) {
        return addAction(name, iconPath, actionListener, Groupable.DEFAULT_GROUP, Sortable.DEFAULT_PRIORITY);
    }

    public NodeAction addAction(String name, String iconPath, NodeActionListener actionListener, int group) {
        return addAction(name, iconPath, actionListener, group, Sortable.DEFAULT_PRIORITY);
    }

    public NodeAction addAction(String name, String iconPath, NodeActionListener actionListener, int group, int priority) {
        final NodeAction nodeAction = addAction(name, actionListener);
        nodeAction.setIconPath(iconPath);
        nodeAction.setGroup(group);
        nodeAction.setPriority(priority);
        return nodeAction;
    }

    protected void loadActions() {
        // add the click action handler
        addClickActionListener(new NodeActionListener() {
            @Override
            public void actionPerformed(NodeActionEvent e) {
                onNodeClick(e);
            }
        });

        // add the other actions
        final Map<String, Class<? extends NodeActionListener>> actions = initActions();

        if (actions != null) {
            for (final Map.Entry<String, Class<? extends NodeActionListener>> entry : actions.entrySet()) {
                try {
                    // get default constructor
                    final Class<? extends NodeActionListener> listenerClass = entry.getValue();
                    final NodeActionListener actionListener = createNodeActionListener(listenerClass);
                    addAction(entry.getKey(), actionListener);
                } catch (final InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                    DefaultLoader.getUIHelper().showException(e.getMessage(), e, "MS Services - Error", true, false);
                }
            }
        }
    }

    protected NodeActionListener createNodeActionListener(Class<? extends NodeActionListener> listenerClass)
            throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        final Constructor constructor = listenerClass.getDeclaredConstructor(getClass());

        // create an instance passing this object as a constructor argument
        // since we assume that this is an inner class
        return (NodeActionListener) constructor.newInstance(this);
    }

    // sub-classes are expected to override this method and
    // add code for initializing node-specific actions; this
    // method is called when the node is being constructed and
    // is guaranteed to be called only once per node
    // NOTE: The Class<?> objects returned by this method MUST be
    // public inner classes of the sub-class. We assume that they are.
    protected Map<String, Class<? extends NodeActionListener>> initActions() {
        final List<Class<? extends NodeActionListener>> actions = node2Actions.get(this.getClass());
        if (actions != null) {
            try {
                for (final Class<? extends NodeActionListener> actionClazz : actions) {
                    final NodeActionListener actionListener = createNodeActionListener(actionClazz);
                    if (Objects.nonNull(actionListener.getAction())) {
                        addAction(new DelegateActionListener.BasicActionListener(actionListener, actionListener.getAction()));
                        continue;
                    }
                    final Name nameAnnotation = actionClazz.getAnnotation(Name.class);
                    if (nameAnnotation != null) {
                        addAction(nameAnnotation.value(), actionListener);
                    }
                }
            } catch (final InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                DefaultLoader.getUIHelper().showException(e.getMessage(), e, "MS Services - Error", true, false);
            }
        }
        return null;
    }

    // sub-classes are expected to override this method and
    // add a handler for the case when something needs to be
    // done when the user left-clicks this node in the tree view
    protected void onNodeClick(NodeActionEvent e) {
    }

    public void onNodeDblClicked(Object context) {
    }

    public List<NodeAction> getNodeActions() {
        return nodeActions;
    }

    public NodeAction getNodeActionByName(final String name) {
        return Iterators.tryFind(nodeActions.iterator(), nodeAction -> name.compareTo(nodeAction.getName()) == 0).orNull();
    }

    public boolean hasNodeActions() {
        return !nodeActions.isEmpty();
    }

    public void addActions(Iterable<NodeAction> actions) {
        for (final NodeAction action : actions) {
            addAction(action);
        }
    }

    public NodeAction getClickAction() {
        return clickAction;
    }

    public void addClickActionListener(NodeActionListener actionListener) {
        clickAction.addListener(actionListener);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public Object getViewData() {
        return viewData;
    }

    public void setViewData(Object viewData) {
        this.viewData = viewData;
    }

    public String getToolTip() {
        return getName();
    }

    public Object getProject() {
        // delegate to parent node if there's one else return null
        if (parent != null) {
            return parent.getProject();
        }

        return null;
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public static void setNode2Actions(Map<Class<? extends Node>, ImmutableList<Class<? extends NodeActionListener>>> node2Actions) {
        Node.node2Actions = node2Actions;
    }

    public void removeNode(String sid, String id, Node node) {
    }

    public Node createNode(Node parent, String sid, NodeContent content) {
        return new Node(content.getId(), content.getName());
    }

    @Override
    @NotNull
    public String getServiceName() {
        return TelemetryConstants.ACTION;
    }

    @AzureOperation(name = "common.open_portal.resource", params = {"nameFromResourceId(resourceId)"}, type = AzureOperation.Type.ACTION)
    public void openResourcesInPortal(String subscriptionId, String resourceId) {
        final Account account = Azure.az(AzureAccount.class).account();
        final String portalUrl = account.getPortalUrl();
        final Subscription subscription = account.getSubscription(subscriptionId);
        final String url = portalUrl
                + REST_SEGMENT_JOB_MANAGEMENT_TENANTID
                + subscription.getTenantId()
                + REST_SEGMENT_JOB_MANAGEMENT_RESOURCE
                + resourceId;
        DefaultLoader.getIdeHelper().openLinkInBrowser(url);
    }

    public static String getProgressMessage(String doingName, String moduleName, String nodeName) {
        return String.format(PROGRESS_MESSAGE_PATTERN, doingName, moduleName, nodeName);
    }

    public static String getPromptMessage(String actionName, String moduleName, String nodeName) {
        return String.format(PROMPT_MESSAGE_PATTERN, actionName, moduleName, nodeName);
    }
}
