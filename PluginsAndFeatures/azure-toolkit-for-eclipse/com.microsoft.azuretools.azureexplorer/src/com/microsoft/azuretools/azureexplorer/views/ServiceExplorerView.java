/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.azureexplorer.views;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
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
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
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
import com.microsoft.azure.toolkit.eclipse.common.EclipseAzureIcons;
import com.microsoft.azure.toolkit.eclipse.explorer.AzureExplorer;
import com.microsoft.azure.toolkit.eclipse.explorer.AzureTreeNode;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
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
import com.nimbusds.jose.util.ArrayUtils;

public class ServiceExplorerView extends ViewPart implements PropertyChangeListener {

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "com.microsoft.azuretools.azureexplorer.views.ServiceExplorerView";
    private static final List<String> UNSUPPORTED_NODE_LIST = Arrays.asList(
        "com.microsoft.tooling.msservices.serviceexplorer.azure.function.FunctionModule",
        "com.microsoft.tooling.msservices.serviceexplorer.azure.arm.ResourceManagementModule",
        "com.microsoft.tooling.msservices.serviceexplorer.azure.sqlserver.SqlServerModule",
        "com.microsoft.tooling.msservices.serviceexplorer.azure.mysql.MySQLModule",
        "com.microsoft.tooling.msservices.serviceexplorer.azure.webapp.WebAppModule");

    private TreeViewer viewer;
    private Action refreshAction;
    private Action signInOutAction;
    private Action selectSubscriptionAction;
    private Action doubleClickAction;

    private AzureModule azureModule;
    private Map<ImageDescriptor, Image> imageMap = new HashMap<>();

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
        private TreeViewer viewer;
        private TreeNode azureNode;
        private AzureTreeNode[] azureModules;

        public ViewContentProvider(TreeViewer viewer) {
            super();
            this.viewer = viewer;
        }

        @Override
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }

        @Override
        public void dispose() {
        }

        @Override
        public Object[] getElements(Object parent) {
            if (parent.equals(getViewSite())) {
                if (azureNode == null) {
                    initialize();
                }
                return new Object[] {azureNode};
            }
            return getChildren(parent);
        }

        @Override
        public Object getParent(Object child) {
            if (child instanceof TreeNode) {
                return (((TreeNode) child).node).getParent().getViewData();
            } else if (child instanceof AzureTreeNode) {
                return ((AzureTreeNode) child).getParent();
            }
            return null;
        }

        @Override
        public Object[] getChildren(Object parent) {
            if (parent == azureNode) {
                return getServiceNodes();
            } else if (parent instanceof TreeNode) {
                return ((TreeNode) parent).getChildNodes().toArray();
            } else if (parent instanceof AzureTreeNode) {
                return ((AzureTreeNode) parent).getChildren().toArray();
            }
            return new Object[0];
        }

        @Override
        public boolean hasChildren(Object parent) {
            if (parent instanceof TreeNode) {
                return ((TreeNode) parent).getChildNodes().size() > 0;
            } else if (parent instanceof AzureTreeNode) {
                return ((AzureTreeNode) parent).hasChildren();
            }
            return false;
        }

        private void setHDInsightRootModule(@NotNull AzureModule azureModule) {
            HDInsightRootModuleImpl hdInsightRootModule = new HDInsightRootModuleImpl(azureModule);
            azureModule.setHdInsightModule(hdInsightRootModule);

            // Enable HDInsight new SDK for Eclipse
            DefaultLoader.getIdeHelper().setApplicationProperty(
                    com.microsoft.azure.hdinsight.common.CommonConst.ENABLE_HDINSIGHT_NEW_SDK, "true");

        }

        private Object[] getServiceNodes() {
            return ArrayUtils.concat(azureNode.getChildNodes().toArray(), azureModules);
        }

        private void initialize() {
            azureModule = new AzureModuleImpl();

            setHDInsightRootModule(azureModule);
            azureNode = createTreeNode(azureModule);

            azureModules = Arrays.stream(AzureExplorer.getModules()).map(node -> new AzureTreeNode(viewer, null, node))
                    .toArray(AzureTreeNode[]::new);

            azureModule.setClearResourcesListener(() -> {
                AzureTaskManager.getInstance()
                        .runLater(() -> Arrays.stream(azureModules).forEach(module -> module.clearNode()));
            });
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
        if (node.hasChildNodes()) {
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
        if (node.hasChildNodes()) {
            // this remove call should cause the NodeListChangeListener object
            // registered on its child nodes to fire which should recursively
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
                        // Eclipse does no support arm, so here need to skip resource management node
                        if (UNSUPPORTED_NODE_LIST.contains(childNode.getClass().getName())) {
                            continue;
                        }
                        treeNode.add(createTreeNode(childNode));
                    }
                    break;
                case remove:
                    // unregister all event handlers recursively and remove
                    // child nodes from the tree
                    for (Node childNode : (Collection<Node>) e.getOldItems()) {
                        if (UNSUPPORTED_NODE_LIST.contains(childNode.getClass().getName())) {
                            continue;
                        }
                        removeEventHandlers(childNode);
                        // remove this node from the tree
                        treeNode.remove((TreeNode) childNode.getViewData());
                    }
                    break;
                default:
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
            if (obj instanceof AzureTreeNode) {
                return ((AzureTreeNode) obj).getText();
            }
            return obj.toString();
        }

        @Override
        public Image getImage(Object obj) {
            AzureIcon icon = null;
            if (obj instanceof TreeNode) {
                final Node node = ((TreeNode) obj).node;
                final String iconPath = Optional.ofNullable(node.getIconPath()).map(path -> "icons/" + path)
                        .orElseGet(() -> Optional.ofNullable(node.getIconSymbol().getIconPath()).orElse(null));
                icon = StringUtils.isEmpty(iconPath) ? null : AzureIcon.builder().iconPath(iconPath).build();
            } else if (obj instanceof AzureTreeNode) {
            	icon = ((AzureTreeNode) obj).getAzureIcon();
            }
            return Optional.ofNullable(icon).map(EclipseAzureIcons::getIcon).map(des -> imageMap.computeIfAbsent(des, ImageDescriptor::createImage)).orElseGet(()->super.getImage(obj));
        }
    }

    class NameSorter extends ViewerSorter {

        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            // TODO Auto-generated method stub
            if (e1 instanceof AzureTreeNode && e2 instanceof AzureTreeNode) {
                return 0; // Skip AzureTreeNode which order was controlled by node
            }
            return super.compare(viewer, e1, e2);
        }
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
        viewer.setContentProvider(new ViewContentProvider(viewer));
        viewer.setLabelProvider(new ViewLabelProvider());
        viewer.setSorter(new NameSorter());
        viewer.setInput(getViewSite());

        // Create the help context id for the viewer's control
        PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "com.microsoft.azuretools.azureexplorer.viewer");
        makeActions();
        hookContextMenu();
        hookMouseActions();
        hookShortcut();
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
                    Object firstElement = selection.getFirstElement();
                    if (firstElement instanceof TreeNode) {
                        Node node = ((TreeNode) selection.getFirstElement()).node;
                        if (node.hasNodeActions()) {
                            for (final NodeAction nodeAction : node.getNodeActions()) {
                                ImageDescriptor imageDescriptor = nodeAction.getIconPath() != null ?
                                        Activator.getImageDescriptor("icons/" + nodeAction.getIconPath()) : null;
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
                    } else if (firstElement instanceof AzureTreeNode) {
                        ((AzureTreeNode) firstElement).installActionsMenu(manager);
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
        selectSubscriptionAction = new Action("Select Subscriptions", com.microsoft.azuretools.core.Activator
            .getImageDescriptor("icons/ConnectAccountsLight_16.png")) {
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
                    TreeItem[] selection = ((Tree) e.widget).getSelection();
                    if (selection.length > 0) {
                        final Object data = selection[0].getData();
                        if (data instanceof TreeNode) {
                            Node node = ((TreeNode) data).node;
                            // if the node in question is in a "loading" state then
                            // we do not propagate the click event to it
                            if (!node.isLoading()) {
                                node.getClickAction().fireNodeActionEvent();
                            }
                        } else if (data instanceof AzureTreeNode) {
                            ((AzureTreeNode) data).onNodeClick();
                        }
                    }
                }
            }
        });
    }

    private void hookShortcut() {
        Tree tree = (Tree) viewer.getControl();
        tree.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent event) {
                if (event.keyCode == SWT.CR && tree.getSelectionCount() > 0) {
                    final Object data = tree.getSelection()[0].getData();
                    if (data instanceof TreeNode) {
                        Node node = ((TreeNode) data).node;
                        // if the node in question is in a "loading" state then
                        // we do not propagate the click event to it
                        if (!node.isLoading()) {
                            node.getClickAction().fireNodeActionEvent();
                        }
                    } else if (data instanceof AzureTreeNode) {
                        ((AzureTreeNode) data).onNodeClick();
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

	@Override
	public void dispose() {
		imageMap.values().forEach(icon -> icon.dispose());
		imageMap.clear();
		super.dispose();
	}
    
    
}
