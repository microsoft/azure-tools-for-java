/**
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.microsoft.azuretools.azureexplorer.views;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.microsoft.azure.hdinsight.serverexplore.HDInsightRootModuleImpl;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azureexplorer.Activator;
import com.microsoft.azuretools.azureexplorer.AzureModuleImpl;
import com.microsoft.azuretools.core.handlers.SelectSubsriptionsCommandHandler;
import com.microsoft.azuretools.core.handlers.SignInCommandHandler;
import com.microsoft.azuretools.core.handlers.SignOutCommandHandler;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.collections.ListChangeListener;
import com.microsoft.tooling.msservices.helpers.collections.ListChangedEvent;
import com.microsoft.tooling.msservices.helpers.collections.ObservableList;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.NodeAction;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureModule;

public class ServiceExplorerView extends ViewPart implements PropertyChangeListener {

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "com.microsoft.azuretools.azureexplorer.views.ServiceExplorerView";

    private TreeViewer viewer;
    private Action refreshAction;
    private Action signInOutAction;
    private Action selectSubscriptionAction;
    private Action doubleClickAction;

    private AzureModule azureModule;

    /*
     * The content provider class is responsible for
     * providing objects to the view. It can wrap
     * existing objects in adapters or simply return
     * objects as-is. These objects may be sensitive
     * to the current input of the view, or ignore
     * it and always show the same content
     * (like Task List, for example).
     */
    class ViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {
        private TreeNode invisibleRoot;

        @Override
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public Object[] getElements(Object parent) {
            if (parent.equals(getViewSite())) {
                if (invisibleRoot == null) initialize();
                return getChildren(invisibleRoot);
            }
            return getChildren(parent);
        }

        @Override
        public Object getParent(Object child) {
            if (child instanceof TreeNode) {
                return (((TreeNode) child).node).getParent().getViewData();
            }
            return null;
        }

        @Override
        public Object[] getChildren(Object parent) {
            if (parent instanceof TreeNode) {
                return ((TreeNode) parent).getChildNodes().toArray();
            }
            return new Object[0];
        }

        @Override
        public boolean hasChildren(Object parent) {
            if (parent instanceof TreeNode)
                return ((TreeNode) parent).getChildNodes().size() > 0;
                return false;
        }

        private void initialize() {
            azureModule = new AzureModuleImpl();
            HDInsightRootModuleImpl hdInsightRootModule =  new HDInsightRootModuleImpl(azureModule);
            azureModule.setHdInsightModule(hdInsightRootModule);
            invisibleRoot = new TreeNode(null);
            invisibleRoot.add(createTreeNode(azureModule));

            azureModule.load(false);
        }
    }

    private class TreeNode {
        Node node;
        List<TreeNode> childNodes = new ArrayList<TreeNode>();

        public TreeNode(Node node) {
            this.node = node;
        }

        public void add(TreeNode treeNode) {
            childNodes.add(treeNode);
        }


        public List<TreeNode> getChildNodes() {
            return childNodes;
        }

        public void remove(TreeNode treeNode) {
            childNodes.remove(treeNode);
        }

        @Override
        public String toString() {
            return node.getName();
        }
    }

    private TreeNode createTreeNode(Node node) {
        TreeNode treeNode = new TreeNode(node);

        // associate the TreeNode with the Node via it's "viewData"
        // property; this allows us to quickly retrieve the DefaultMutableTreeNode
        // object associated with a Node
        node.setViewData(treeNode);

        // listen for property change events on the node
        node.addPropertyChangeListener(this);

        // listen for structure changes on the node, i.e. when child nodes are
        // added or removed
        node.getChildNodes().addChangeListener(new NodeListChangeListener(treeNode));

        // create child tree nodes for each child node
        if(node.hasChildNodes()) {
            for (Node childNode : node.getChildNodes()) {
                treeNode.add(createTreeNode(childNode));
            }
        }
        return treeNode;
    }

    private void removeEventHandlers(Node node) {
        node.removePropertyChangeListener(this);

        ObservableList<Node> childNodes = node.getChildNodes();
        childNodes.removeAllChangeListeners();
        //
        if(node.hasChildNodes()) {
            // this remove call should cause the NodeListChangeListener object
            // registered on it's child nodes to fire which should recursively
            // clean up event handlers on it's children
            node.removeAllChildNodes();
        }
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        // this event is fired whenever a property on a node in the
        // model changes; we respond by triggering a node change
        // event in the tree's model
        final Node node = (Node) evt.getSource();
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                viewer.refresh(node.getViewData());
            }
        });
    }

    private class NodeListChangeListener implements ListChangeListener {
        private TreeNode treeNode;

        public NodeListChangeListener(TreeNode treeNode) {
            this.treeNode = treeNode;
        }

        @Override
        public void listChanged(final ListChangedEvent e) {
            switch (e.getAction()) {
            case add:
                // create child tree nodes for the new nodes
                for (Node childNode : (Collection<Node>) e.getNewItems()) {
                    // Dirty fix for issue https://github.com/Microsoft/azure-tools-for-java/issues/2791
                    // Since we do not support slot, here should not let user see slot in the azure explorer
                    if (childNode.getName() == null || !childNode.getName().equals("Deployment Slots")) {
                        treeNode.add(createTreeNode(childNode));
                    }
                }
                break;
            case remove:
                // unregister all event handlers recursively and remove
                // child nodes from the tree
                for(Node childNode : (Collection<Node>)e.getOldItems()) {
                    removeEventHandlers(childNode);

                    // remove this node from the tree
                    treeNode.remove((TreeNode) childNode.getViewData());
                }
                break;
            }
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    viewer.refresh(treeNode);
                }
            });
        }
    }

    class ViewLabelProvider extends LabelProvider {

        @Override
        public String getText(Object obj) {
            return obj.toString();
        }

        @Override
        public Image getImage(Object obj) {
            if (obj instanceof TreeNode) {
                String iconPath = ((TreeNode) obj).node.getIconPath();
                if (iconPath != null) {
                    return Activator.getImageDescriptor("icons/" + iconPath).createImage();//Activator.getDefault().getImageRegistry().get((((Node) obj).getIconPath()));
                }
            }
            return super.getImage(obj);
        }
    }

    class NameSorter extends ViewerSorter {
    }

    /**
     * The constructor.
     */
    public ServiceExplorerView() {
    }

    /**
     * This is a callback that will allow us
     * to create the viewer and initialize it.
     */
    @Override
    public void createPartControl(Composite parent) {
        viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.setContentProvider(new ViewContentProvider());
        viewer.setLabelProvider(new ViewLabelProvider());
        viewer.setSorter(new NameSorter());
        viewer.setInput(getViewSite());

        // Create the help context id for the viewer's control
        PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "com.microsoft.azuretools.azureexplorer.viewer");
        makeActions();
        hookContextMenu();
        hookMouseActions();
        contributeToActionBars();
    }

    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                if (viewer.getSelection().isEmpty()) {
                    return;
                }
                if (viewer.getSelection() instanceof IStructuredSelection) {
                    IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
                    Node node = ((TreeNode) selection.getFirstElement()).node;
                    if (node.hasNodeActions()) {
                        for (final NodeAction nodeAction : node.getNodeActions()) {
                            ImageDescriptor imageDescriptor = nodeAction.getIconPath() != null ? Activator.getImageDescriptor("icons/" + nodeAction.getIconPath()) : null;
                            Action action = new Action(nodeAction.getName(), imageDescriptor) {
                                @Override
                                public void run() {
                                    nodeAction.fireNodeActionEvent();
                                }
                            };
                            action.setEnabled(nodeAction.isEnabled());
                            manager.add(action);
                        }
                    }
                }
            }
        });
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalToolBar(bars.getToolBarManager());
        updateActions();
        try {
            Runnable signInOutListener = new Runnable() {
                @Override
                public void run() {
                    updateActions();
                }
            };
            AuthMethodManager.getInstance().addSignInEventListener(signInOutListener);
            AuthMethodManager.getInstance().addSignOutEventListener(signInOutListener);
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().logError(ex.getMessage(), ex);
        }
    }

    private void updateActions() {
        try {
            boolean isSignedIn = AuthMethodManager.getInstance().isSignedIn();
            selectSubscriptionAction.setEnabled(isSignedIn);
            signInOutAction.setImageDescriptor(Activator.getImageDescriptor(isSignedIn ? "icons/SignOutLight_16.png" : "icons/SignInLight_16.png"));
            signInOutAction.setToolTipText(isSignedIn ? "Sign Out" : "Sign In");
        } catch (Exception ex) {
            // ignore
        }
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(refreshAction);
        manager.add(signInOutAction);
        manager.add(selectSubscriptionAction);
        manager.add(new Separator());
    }

    private void makeActions() {
        refreshAction = new Action("Refresh", Activator.getImageDescriptor("icons/RefreshLight_16.png")) {
            @Override
            public void run() {
                azureModule.load(true);
            }
        };
        refreshAction.setToolTipText("Refresh");
        signInOutAction = new Action("Sign In/Sign Out", com.microsoft.azuretools.core.Activator.getImageDescriptor("icons/SignOutLight_16.png")) {
            @Override
            public void run() {
                try {
                    AuthMethodManager authMethodManager = AuthMethodManager.getInstance();
                    boolean isSignedIn = authMethodManager.isSignedIn();
                    if (isSignedIn) {
                        SignOutCommandHandler.doSignOut(PluginUtil.getParentShell());
                    } else {
                        SignInCommandHandler.doSignIn(PluginUtil.getParentShell());
                    }
                } catch (Exception ex) {
                    Activator.getDefault().log(ex.getMessage(), ex);
                }
            }
        };
        selectSubscriptionAction = new Action("Select Subscriptions", com.microsoft.azuretools.core.Activator.getImageDescriptor("icons/ConnectAccountsLight_16.png")) {
            @Override
            public void run() {
                try {
                    if (AuthMethodManager.getInstance().isSignedIn()) {
                        SelectSubsriptionsCommandHandler.onSelectSubscriptions(PluginUtil.getParentShell());
                        azureModule.load(false);
                    }
                } catch (Exception ex) {
                }
            }
        };
        selectSubscriptionAction.setToolTipText("Select Subscriptions");
        doubleClickAction = new Action() {
            @Override
            public void run() {
                ISelection selection = viewer.getSelection();
                Object obj = ((IStructuredSelection) selection).getFirstElement();
                if (!viewer.getExpandedState(obj)) {
                    viewer.expandToLevel(obj, 1);
                } else {
                    viewer.collapseToLevel(obj, 1);
                }
            }
        };
    }

    private void hookMouseActions() {
        viewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                doubleClickAction.run();
            }
        });
        Tree tree = (Tree) viewer.getControl();
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                if (e.button == 1) { // left button
                    TreeItem[] selection = ((Tree)e.widget).getSelection();
                    if (selection.length > 0) {
                        TreeItem item = ((Tree) e.widget).getSelection()[0];
                        Node node = ((TreeNode) item.getData()).node;
                        // if the node in question is in a "loading" state then
                        // we do not propagate the click event to it
                        if (!node.isLoading()) {
                            node.getClickAction().fireNodeActionEvent();
                        }
                    }
                }
            }
        });
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }
}