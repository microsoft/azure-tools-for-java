/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.tooling.msservices.serviceexplorer;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.core.mvp.ui.base.MvpView;
import com.microsoft.azuretools.core.mvp.ui.base.NodeContent;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.BasicTelemetryProperty;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.Name;
import com.microsoft.tooling.msservices.helpers.collections.ObservableList;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Node implements MvpView, BasicTelemetryProperty {
    private static final String CLICK_ACTION = "click";
    public static final String REST_SEGMENT_JOB_MANAGEMENT_TENANTID = "/#@";
    public static final String REST_SEGMENT_JOB_MANAGEMENT_RESOURCE = "/resource";
    public static final String OPEN_RESOURCES_IN_PORTAL_FAILED = "Fail to open resources in portal.";

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

    public Node(String id, String name, Node parent, String iconPath) {
        this(id, name, parent, iconPath, false);
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
        String oldValue = this.name;
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
        if (isDirectChild(node))
            return true;
        for (Node child : childNodes) {
            if (child.isDescendant(node))
                return true;
        }

        return false;
    }

    // Walk up the tree till we find a parent node who's type
    // is equal to "clazz".
    public <T extends Node> T findParentByType(Class<T> clazz) {
        if (parent == null)
            return null;
        if (parent.getClass().equals(clazz))
            return (T) parent;
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
            Node node = childNodes.get(0);

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

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        String oldValue = this.iconPath;
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
        return nodeAction;
    }

    public NodeAction addAction(String name, String iconPath, NodeActionListener actionListener) {
        NodeAction nodeAction = addAction(name, actionListener);
        nodeAction.setIconPath(iconPath);
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
        Map<String, Class<? extends NodeActionListener>> actions = initActions();

        if (actions != null) {
            for (Map.Entry<String, Class<? extends NodeActionListener>> entry : actions.entrySet()) {
                try {
                    // get default constructor
                    Class<? extends NodeActionListener> listenerClass = entry.getValue();
                    NodeActionListener actionListener = createNodeActionListener(listenerClass);
                    addAction(entry.getKey(), actionListener);
                } catch (InstantiationException e) {
                    DefaultLoader.getUIHelper().showException(e.getMessage(), e,
                            "MS Services - Error", true, false);
                } catch (IllegalAccessException e) {
                    DefaultLoader.getUIHelper().showException(e.getMessage(), e,
                            "MS Services - Error", true, false);
                } catch (NoSuchMethodException e) {
                    DefaultLoader.getUIHelper().showException(e.getMessage(), e,
                            "MS Services - Error", true, false);
                } catch (InvocationTargetException e) {
                    DefaultLoader.getUIHelper().showException(e.getMessage(), e,
                            "MS Services - Error", true, false);
                }
            }
        }
    }

    protected NodeActionListener createNodeActionListener(Class<? extends NodeActionListener> listenerClass)
            throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor constructor = listenerClass.getDeclaredConstructor(getClass());

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
        List<Class<? extends NodeActionListener>> actions = node2Actions.get(this.getClass());
        if (actions != null) {
            try {
                for (Class<? extends NodeActionListener> actionListener : actions) {
                    Name nameAnnotation = actionListener.getAnnotation(Name.class);
                    if (nameAnnotation != null) {
                        addAction(nameAnnotation.value(), createNodeActionListener(actionListener));
                    }
                }
            } catch (InstantiationException e) {
                DefaultLoader.getUIHelper().showException(e.getMessage(), e,
                        "MS Services - Error", true, false);
            } catch (IllegalAccessException e) {
                DefaultLoader.getUIHelper().showException(e.getMessage(), e,
                        "MS Services - Error", true, false);
            } catch (NoSuchMethodException e) {
                DefaultLoader.getUIHelper().showException(e.getMessage(), e,
                        "MS Services - Error", true, false);
            } catch (InvocationTargetException e) {
                DefaultLoader.getUIHelper().showException(e.getMessage(), e,
                        "MS Services - Error", true, false);
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
        return Iterators.tryFind(nodeActions.iterator(), new Predicate<NodeAction>() {
            @Override
            public boolean apply(NodeAction nodeAction) {
                return name.compareTo(nodeAction.getName()) == 0;
            }
        }).orNull();
    }

    public boolean hasNodeActions() {
        return !nodeActions.isEmpty();
    }

    public void addActions(Iterable<NodeAction> actions) {
        for (NodeAction action : actions) {
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

    public void removeNode(String sid, String id, Node node) { }

    public Node createNode(Node parent, String sid, NodeContent content) {
        return new Node(content.getId(), content.getName());
    }

    @Override
    @NotNull
    public String getServiceName() {
        return TelemetryConstants.ACTION;
    }

    public void openResourcesInPortal(String subscriptionId, String resourceRelativePath) throws AzureCmdException {
        try {
            final AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            // not signed in
            if (azureManager == null) {
                return;
            }
            final String portalUrl = azureManager.getPortalUrl();
            final String tenantId = azureManager.getTenantIdBySubscription(subscriptionId);
            String url = portalUrl
                    + REST_SEGMENT_JOB_MANAGEMENT_TENANTID
                    + tenantId
                    + REST_SEGMENT_JOB_MANAGEMENT_RESOURCE
                    + resourceRelativePath;
            DefaultLoader.getIdeHelper().openLinkInBrowser(url);
        } catch (IOException e) {
            throw new AzureCmdException(OPEN_RESOURCES_IN_PORTAL_FAILED, e);
        }
    }
}
